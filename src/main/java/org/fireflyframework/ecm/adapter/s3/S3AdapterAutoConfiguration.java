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
package org.fireflyframework.ecm.adapter.s3;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Auto-configuration for the Amazon S3 ECM adapter.
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(name = "firefly.ecm.adapter-type", havingValue = "s3")
@EnableConfigurationProperties(S3AdapterProperties.class)
public class S3AdapterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AwsCredentialsProvider awsCredentialsProvider(S3AdapterProperties properties) {
        if (properties.getAccessKey() != null && properties.getSecretKey() != null) {
            log.info("Using static credentials for S3 adapter");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    properties.getAccessKey(),
                    properties.getSecretKey()
            );
            return StaticCredentialsProvider.create(credentials);
        } else {
            log.info("Using default credentials provider chain for S3 adapter");
            return DefaultCredentialsProvider.create();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings("deprecation")
    public S3Client s3Client(S3AdapterProperties properties, AwsCredentialsProvider credentialsProvider) {
        log.info("Configuring S3 client for region: {}, bucket: {}",
                properties.getRegion(), properties.getBucketName());

        S3ClientBuilder clientBuilder = S3Client.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider);

        if (properties.getEndpoint() != null && !properties.getEndpoint().trim().isEmpty()) {
            log.info("Using custom S3 endpoint: {}", properties.getEndpoint());
            clientBuilder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (properties.getPathStyleAccess()) {
            log.info("Enabling path-style access for S3 client");
            clientBuilder.forcePathStyle(true);
        }

        ClientOverrideConfiguration.Builder overrideBuilder = ClientOverrideConfiguration.builder();
        if (properties.getConnectionTimeout() != null) {
            overrideBuilder.apiCallTimeout(properties.getConnectionTimeout());
        }
        if (properties.getSocketTimeout() != null) {
            overrideBuilder.apiCallAttemptTimeout(properties.getSocketTimeout());
        }
        if (properties.getMaxRetries() != null) {
            RetryPolicy retryPolicy = RetryPolicy.builder()
                    .numRetries(properties.getMaxRetries())
                    .build();
            overrideBuilder.retryPolicy(retryPolicy);
        }

        clientBuilder.overrideConfiguration(overrideBuilder.build());
        return clientBuilder.build();
    }
}
