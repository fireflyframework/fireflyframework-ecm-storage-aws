package org.fireflyframework.ecm.adapter.s3;

import org.fireflyframework.ecm.port.document.DocumentContentPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {S3AdapterAutoConfiguration.class, S3Beans.class, S3DocumentContentAdapter.class})
@TestPropertySource(properties = {
        "firefly.ecm.enabled=true",
        "firefly.ecm.adapter-type=s3",
        "firefly.ecm.adapter.s3.bucket-name=test-bucket",
        "firefly.ecm.adapter.s3.region=us-east-1"
})
class S3AdapterAutoConfigurationTest {

    @Autowired
    private DocumentContentPort contentPort;

    @Test
    void contextLoads_andS3DocumentContentAdapterPresent() {
        assertThat(contentPort).isInstanceOf(S3DocumentContentAdapter.class);
    }
}
