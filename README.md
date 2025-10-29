# Firefly ECM Storage – AWS S3

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21+-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

Amazon S3 storage adapter for Firefly lib‑ecm. Implements DocumentContentPort with streaming downloads, buffered/multipart uploads, checksums, size/existence, deletes, and pre‑signed upload URLs.

## Features
- Streaming downloads via Flux<DataBuffer>
- Buffered uploads with optional multipart threshold
- Byte‑range reads; exists/size; delete by ID or path
- Optional SSE‑KMS (via kmsKeyId) and storage class selection
- Pre‑signed upload URL generation (PUT)
- Conditional auto‑wiring when `firefly.ecm.adapter-type=s3`

## Installation
```xml
<dependency>
  <groupId>com.firefly</groupId>
  <artifactId>lib-ecm-storage-aws</artifactId>
  <version>${firefly.version}</version>
</dependency>
```

## Configuration
```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3
    adapter:
      s3:
        bucket-name: ${S3_BUCKET_NAME}
        region: ${AWS_REGION:us-east-1}
        # auth (prefer IAM roles)
        access-key: ${AWS_ACCESS_KEY_ID:}
        secret-key: ${AWS_SECRET_ACCESS_KEY:}
        # optional
        endpoint: ${S3_ENDPOINT:}
        path-prefix: ${S3_PATH_PREFIX:documents/}
        enable-versioning: ${S3_ENABLE_VERSIONING:true}
        path-style-access: ${S3_PATH_STYLE_ACCESS:false}
        connection-timeout: 30s
        socket-timeout: 30s
        max-retries: 3
        enable-encryption: true
        kms-key-id: ${S3_KMS_KEY_ID:}
        storage-class: STANDARD
        enable-multipart: true
        multipart-threshold: 5242880   # 5MB
        multipart-part-size: 5242880   # 5MB
```

## Usage
```java
@Autowired DocumentContentPort contentPort;
UUID id = UUID.randomUUID();
// upload (buffers stream in memory before put)
Mono<String> path = contentPort.storeContentStream(id, bodyFlux, "application/pdf", null);
// download streaming
Flux<DataBuffer> stream = contentPort.getContentStream(id);
// pre-signed PUT URL (direct-to-S3)
String url = ((S3DocumentContentAdapter) contentPort).generateUploadUrl(id, 15).block();
```

Notes
- Upload method buffers the incoming stream before S3 PUT; for very large files consider using pre‑signed URLs or extending to true multipart streaming.
- Prefer IAM roles or environment credentials; avoid static keys in configuration.

## Testing
- Includes Spring Boot auto‑configuration test validating bean wiring and sample properties.

## License
Apache 2.0
