# Firefly Framework - ECM Storage - AWS S3

[![CI](https://github.com/fireflyframework/fireflyframework-ecm-storage-aws/actions/workflows/ci.yml/badge.svg)](https://github.com/fireflyframework/fireflyframework-ecm-storage-aws/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21%2B-orange.svg)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

> Amazon S3 storage adapter for Firefly ECM document content management.

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

This module implements the Firefly ECM `DocumentContentPort` using Amazon S3 as the storage backend. It provides `S3DocumentContentAdapter` for storing, retrieving, and managing document content in S3 buckets with configurable bucket policies and access controls.

The adapter auto-configures via `S3AdapterAutoConfiguration` with AWS SDK v2 and is activated by including this module on the classpath alongside the ECM core module.

## Features

- Amazon S3 integration for document content storage and retrieval
- Spring Boot auto-configuration for seamless activation
- Implements Firefly ECM DocumentContentPort
- Configurable via application properties
- Standalone provider library (include alongside fireflyframework-ecm)

## Requirements

- Java 21+
- Spring Boot 3.x
- Maven 3.9+
- Amazon S3 account and API credentials

## Installation

```xml
<dependency>
    <groupId>org.fireflyframework</groupId>
    <artifactId>fireflyframework-ecm-storage-aws</artifactId>
    <version>26.02.03</version>
</dependency>
```

## Quick Start

The adapter is automatically activated when included on the classpath with the ECM core module:

```xml
<dependencies>
    <dependency>
        <groupId>org.fireflyframework</groupId>
        <artifactId>fireflyframework-ecm</artifactId>
    </dependency>
    <dependency>
        <groupId>org.fireflyframework</groupId>
        <artifactId>fireflyframework-ecm-storage-aws</artifactId>
    </dependency>
</dependencies>
```

## Configuration

```yaml
firefly:
  ecm:
    storage:
      aws-s3:
        bucket: my-documents-bucket
        region: us-east-1
```

## Documentation

No additional documentation available for this project.

## Contributing

Contributions are welcome. Please read the [CONTRIBUTING.md](CONTRIBUTING.md) guide for details on our code of conduct, development process, and how to submit pull requests.

## License

Copyright 2024-2026 Firefly Software Solutions Inc.

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
