/*
 * Copyright 2024 Firefly Software Solutions Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.firefly.ecm.adapter.s3;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;

/**
 * Configuration properties for the Amazon S3 ECM adapter.
 */
@Data
@Validated
@ConfigurationProperties(prefix = "firefly.ecm.adapter.s3")
public class S3AdapterProperties {

    @NotBlank(message = "S3 bucket name is required")
    private String bucketName;

    @NotBlank(message = "AWS region is required")
    private String region;

    private String accessKey;

    private String secretKey;

    private String endpoint;

    private String pathPrefix = "documents/";

    private Boolean enableVersioning = true;

    private Boolean pathStyleAccess = false;

    @NotNull
    private Duration connectionTimeout = Duration.ofSeconds(30);

    @NotNull
    private Duration socketTimeout = Duration.ofSeconds(30);

    private Integer maxRetries = 3;

    private Boolean enableEncryption = true;

    private String kmsKeyId;

    private String storageClass = "STANDARD";

    private Boolean enableMultipart = true;

    private Long multipartThreshold = 5L * 1024 * 1024; // 5MB

    private Long multipartPartSize = 5L * 1024 * 1024; // 5MB
}
