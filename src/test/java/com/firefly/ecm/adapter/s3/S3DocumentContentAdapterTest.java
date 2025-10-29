package com.firefly.ecm.adapter.s3;

import com.firefly.core.ecm.port.document.DocumentContentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.internal.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {
        S3DocumentContentAdapterTest.TestConfig.class,
        S3DocumentContentAdapter.class
})
@TestPropertySource(properties = {
        "firefly.ecm.enabled=true",
        "firefly.ecm.adapter-type=s3"
})
class S3DocumentContentAdapterTest {

    @Configuration
    static class TestConfig {
        @Bean
        S3AdapterProperties properties() {
            S3AdapterProperties p = new S3AdapterProperties();
            p.setBucketName("test-bucket");
            p.setRegion("us-east-1");
            p.setPathPrefix("documents/");
            p.setEnableMultipart(true);
            p.setMultipartThreshold(5L * 1024 * 1024);
            return p;
        }

        @Bean
        S3Client s3Client() {
            return mock(S3Client.class);
        }

        @Bean
        S3Presigner s3Presigner(S3AdapterProperties properties) {
            return S3Presigner.builder()
                    .region(software.amazon.awssdk.regions.Region.of(properties.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")
                    ))
                    .build();
        }
    }

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private S3AdapterProperties properties;

    @Autowired
    private DocumentContentPort contentPort;

    private UUID documentId;

    @BeforeEach
    void setUp() {
        documentId = UUID.randomUUID();
        reset(s3Client);
    }

    @Test
    void storeContent_smallBytes_putsObjectAndReturnsKey() {
        byte[] data = "hello".getBytes();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        String key = contentPort.storeContent(documentId, data, "text/plain").block();

        assertThat(key).isEqualTo("documents/" + documentId);
        ArgumentCaptor<PutObjectRequest> req = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(req.capture(), any(RequestBody.class));
        assertThat(req.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(req.getValue().key()).isEqualTo("documents/" + documentId);
    }

    @Test
    void storeContentStream_buffersAndStores_andReturnsKey() {
        DataBuffer buf1 = new DefaultDataBufferFactory().wrap("hello ".getBytes());
        DataBuffer buf2 = new DefaultDataBufferFactory().wrap("world".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        StepVerifier.create(
                contentPort.storeContentStream(documentId, Flux.just(buf1, buf2), "text/plain", null)
        )
        .expectNext("documents/" + documentId)
        .verifyComplete();

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void getContentRange_readsRequestedRange_andReturnsBytes() throws Exception {
        byte[] rangeBytes = "bcd".getBytes();
        GetObjectResponse getResp = GetObjectResponse.builder().contentLength((long) rangeBytes.length).build();
ResponseInputStream<GetObjectResponse> ris = new ResponseInputStream<>(
                getResp,
                AbortableInputStream.create(new ByteArrayInputStream(rangeBytes))
        );
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(ris);

        byte[] result = ((S3DocumentContentAdapter) contentPort).getContentRange(documentId, 1L, 3L).block();
        assertThat(new String(result)).isEqualTo("bcd");

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertThat(captor.getValue().range()).isEqualTo("bytes=1-3");
    }

    @Test
    void existsContent_true_whenHeadObjectSucceeds() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(10L).build());
        Boolean exists = contentPort.existsContent(documentId).block();
        assertThat(exists).isTrue();
    }

    @Test
    void existsContent_false_whenNoSuchKey() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());
        Boolean exists = contentPort.existsContent(documentId).block();
        assertThat(exists).isFalse();
    }

    @Test
    void getContentSize_returnsHeadObjectContentLength() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(123L).build());
        Long size = contentPort.getContentSize(documentId).block();
        assertThat(size).isEqualTo(123L);
    }

    @Test
    void deleteContent_invokesDeleteObject() {
        doNothing().when(s3Client).deleteObject(any(DeleteObjectRequest.class));
        contentPort.deleteContent(documentId).block();
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void generateUploadUrl_returnsSignedUrl() {
        String url = ((S3DocumentContentAdapter) contentPort).generateUploadUrl(documentId, 10)
                .block(Duration.ofSeconds(2));
        assertThat(url).isNotBlank();
        assertThat(url).contains("https://");
    }
}