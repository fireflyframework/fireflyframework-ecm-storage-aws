# Firefly Framework - ECM Storage AWS (S3)

[![CI](https://github.com/fireflyframework/fireflyframework-ecm-storage-aws/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-ecm-storage-aws/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

> Amazon S3 storage adapter for the Firefly ECM abstraction — reactive document content storage, retrieval, streaming, checksums and pre-signed URLs backed by AWS S3 (or any S3-compatible object store).

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Overview

`fireflyframework-ecm-storage-aws` is a pluggable **storage adapter** for the Firefly Enterprise Content Management (ECM) abstraction. The ECM core module (`fireflyframework-ecm`) defines a hexagonal **port**, `DocumentContentPort`, that describes how a document's binary content is stored, streamed, deleted and verified — without coupling the application to any specific backend. This module provides the Amazon S3 implementation of that port.

The adapter is built on **AWS SDK for Java v2** (`software.amazon.awssdk:s3`) and exposes a fully reactive (Project Reactor) API, so it integrates cleanly with Spring WebFlux applications. It stores each document as an S3 object keyed by document UUID under a configurable path prefix, and supports streaming reads/writes, HTTP range requests for partial content, server-side encryption (SSE / KMS), versioning, multipart uploads for large files, and time-boxed pre-signed upload URLs.

Like every Firefly ECM adapter, it is selected at runtime by a single property. Setting `firefly.ecm.adapter-type=s3` activates the S3 auto-configuration; the adapter then registers itself as the active `DocumentContentPort` implementation with the ECM core's adapter registry. Because the backend is selected by property rather than by code, you can swap S3 for another provider (for example the Azure Blob adapter) without touching application logic.

### Where it fits

| Module | Role |
| ------ | ---- |
| `fireflyframework-ecm` | ECM core — defines the `DocumentContentPort` SPI, the `@EcmAdapter` registry and the `firefly.ecm.adapter-type` selector |
| **`fireflyframework-ecm-storage-aws`** (this module) | **Amazon S3 storage adapter** — `adapter-type: s3` |
| `fireflyframework-ecm-storage-azure` | Azure Blob Storage storage adapter |
| `fireflyframework-ecm-esignature-docusign` | DocuSign eSignature adapter |
| `fireflyframework-ecm-esignature-adobe-sign` | Adobe Sign eSignature adapter |
| `fireflyframework-ecm-esignature-logalty` | Logalty eSignature adapter |

## Features

- **Reactive `DocumentContentPort` implementation** (`S3DocumentContentAdapter`) — all operations return Reactor `Mono`/`Flux` and run on the bounded-elastic scheduler, suitable for WebFlux services.
- **Full content lifecycle** — store, retrieve, stream, delete and existence/size checks by document `UUID` *and* by raw storage path (`getContentByPath`, `getContentStreamByPath`, `deleteContentByPath`).
- **Streaming I/O** — chunked download via `getContentStream` (8 KB `DataBuffer` chunks) and streamed upload via `storeContentStream`, avoiding loading entire large documents into memory where possible.
- **HTTP range requests** — `getContentRange(documentId, start, end)` issues S3 `bytes=` range reads for partial / resumable downloads.
- **Integrity verification** — `calculateChecksum` and `verifyChecksum` compute hex digests using any JDK `MessageDigest` algorithm (e.g. `SHA-256`, `MD5`).
- **Pre-signed upload URLs** — `generateUploadUrl(documentId, expirationMinutes)` produces a time-boxed S3 `PUT` URL via `S3Presigner` for secure direct-to-S3 client uploads.
- **Large-file uploads** — multipart upload is selected automatically when content exceeds the configurable `multipart-threshold`.
- **Flexible credentials** — static access/secret keys when provided, otherwise the AWS `DefaultCredentialsProvider` chain (IAM roles, instance profiles, environment, SSO, etc.).
- **S3-compatible backends** — `endpoint` override + `path-style-access` make the adapter work against MinIO, LocalStack and other S3-compatible object stores.
- **Server-side encryption & storage tiers** — properties for KMS encryption (`kms-key-id`) and S3 storage class selection.
- **Zero-code activation** — Spring Boot auto-configuration (`S3AdapterAutoConfiguration` + `S3Beans`) wires the `S3Client`, `S3Presigner`, credentials provider and adapter bean, gated on `firefly.ecm.adapter-type=s3`.

## Requirements

- Java 21+ (Java 25 recommended)
- Spring Boot 3.x
- Maven 3.9+
- `fireflyframework-ecm` (the ECM core) on the classpath — pulled in transitively by this module
- An Amazon S3 bucket (or an S3-compatible endpoint such as MinIO / LocalStack) and credentials with `s3:GetObject`, `s3:PutObject`, `s3:DeleteObject` and `s3:ListBucket` permissions

## Installation

Add the dependency. The version is managed by the Firefly parent / BOM, so you normally omit `<version>`:

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-ecm-storage-aws</artifactId>
    <!-- version managed by the Firefly parent POM / BOM -->
</dependency>
```

This adapter declares a transitive dependency on `fireflyframework-ecm`, so adding it brings the ECM core onto the classpath automatically. If you depend on the ECM core explicitly as well, that is fine.

## Quick Start

**1. Add the dependency** (see above) and **select the S3 backend** with a single property:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3          # activates this adapter as the active DocumentContentPort
    adapter:
      s3:
        bucket-name: firefly-ecm-documents
        region: us-east-1
        # credentials omitted -> uses the AWS default credentials provider chain (IAM role, etc.)
```

That is all that is required — `S3AdapterAutoConfiguration` builds the `S3Client`, `S3Presigner` and credentials provider, and registers `S3DocumentContentAdapter` as the active `DocumentContentPort`.

**2. Inject and use the port** from your services (program against the SPI, not the S3 type):

```java
import org.fireflyframework.ecm.port.document.DocumentContentPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentContentPort content;

    public DocumentService(DocumentContentPort content) {
        this.content = content;
    }

    public Mono<String> upload(UUID documentId, byte[] bytes) {
        // returns the S3 object key where the content was stored
        return content.storeContent(documentId, bytes, "application/pdf");
    }

    public Mono<byte[]> download(UUID documentId) {
        return content.getContent(documentId);
    }

    public Mono<Boolean> verify(UUID documentId, String expectedSha256) {
        return content.verifyChecksum(documentId, expectedSha256, "SHA-256");
    }
}
```

**3. (Optional) generate a pre-signed upload URL** for direct browser-to-S3 uploads by injecting the concrete adapter:

```java
import org.fireflyframework.ecm.adapter.s3.S3DocumentContentAdapter;

// 15-minute pre-signed PUT URL
Mono<String> url = s3Adapter.generateUploadUrl(documentId, 15);
```

## Configuration

All properties live under the `firefly.ecm.adapter.s3` prefix and are bound by `S3AdapterProperties` (validated with Bean Validation). The example below shows every property with its real default:

```yaml
firefly:
  ecm:
    enabled: true
    adapter-type: s3                 # must be "s3" to activate this adapter
    adapter:
      s3:
        # --- Required ---
        bucket-name: firefly-ecm-documents   # target S3 bucket (required, not blank)
        region: us-east-1                    # AWS region (required, not blank)

        # --- Credentials (optional; omit to use the default provider chain / IAM roles) ---
        access-key:                          # static AWS access key
        secret-key:                          # static AWS secret key

        # --- Endpoint & addressing (optional; for MinIO / LocalStack / S3-compatible stores) ---
        endpoint:                            # custom S3 endpoint override
        path-style-access: false             # force path-style URLs (true for MinIO/LocalStack)
        path-prefix: documents/              # key prefix for every stored object

        # --- Reliability ---
        connection-timeout: PT30S            # API call timeout (Duration)
        socket-timeout: PT30S                # API call attempt timeout (Duration)
        max-retries: 3                       # SDK retry attempts

        # --- Object behaviour ---
        enable-versioning: true              # mark objects as versioned
        enable-encryption: true              # request server-side encryption
        kms-key-id:                          # SSE-KMS key id (when using KMS)
        storage-class: STANDARD              # S3 storage class

        # --- Large files ---
        enable-multipart: true               # use multipart upload above the threshold
        multipart-threshold: 5242880         # 5 MB — switch to multipart above this size
        multipart-part-size: 5242880         # 5 MB — part size for multipart uploads
```

### Key properties

| Property | Default | Description |
| -------- | ------- | ----------- |
| `firefly.ecm.adapter-type` | — | Set to `s3` to activate this adapter (ECM core property). |
| `firefly.ecm.adapter.s3.bucket-name` | — | **Required.** Target S3 bucket. |
| `firefly.ecm.adapter.s3.region` | — | **Required.** AWS region, e.g. `eu-west-1`. |
| `firefly.ecm.adapter.s3.access-key` / `secret-key` | _unset_ | Static credentials. When omitted, the AWS `DefaultCredentialsProvider` chain is used (recommended: IAM roles). |
| `firefly.ecm.adapter.s3.endpoint` | _unset_ | Custom endpoint for S3-compatible stores (MinIO, LocalStack). |
| `firefly.ecm.adapter.s3.path-style-access` | `false` | Force path-style addressing — set `true` for MinIO / LocalStack. |
| `firefly.ecm.adapter.s3.path-prefix` | `documents/` | Key prefix applied to every object: keys become `<prefix><documentId>`. |
| `firefly.ecm.adapter.s3.connection-timeout` | `PT30S` | Overall API call timeout. |
| `firefly.ecm.adapter.s3.socket-timeout` | `PT30S` | Per-attempt API call timeout. |
| `firefly.ecm.adapter.s3.max-retries` | `3` | Maximum SDK retry attempts. |
| `firefly.ecm.adapter.s3.enable-multipart` | `true` | Enable multipart upload for large objects. |
| `firefly.ecm.adapter.s3.multipart-threshold` | `5242880` (5 MB) | Size above which multipart upload is used. |
| `firefly.ecm.adapter.s3.kms-key-id` | _unset_ | KMS key id for SSE-KMS encryption. |
| `firefly.ecm.adapter.s3.storage-class` | `STANDARD` | S3 storage class (e.g. `STANDARD_IA`, `INTELLIGENT_TIERING`). |

> A ready-to-use sample profile is bundled at `src/main/resources/application-s3.yml`, with every value sourced from environment variables.

## Documentation

- Firefly Framework org & module catalog: [github.com/fireflyframework](https://github.com/fireflyframework)
- ECM core (SPI, ports, adapter registry): [`fireflyframework-ecm`](https://github.com/fireflyframework/fireflyframework-ecm)
- Sibling storage adapter: [`fireflyframework-ecm-storage-azure`](https://github.com/fireflyframework/fireflyframework-ecm-storage-azure)

## Contributing

Contributions are welcome. Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code of conduct, development process, and how to submit pull requests.

## License

Copyright 2024-2026 Firefly Software Foundation.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
