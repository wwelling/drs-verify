/**
 * Copyright (c) 2021 President and Fellows of Harvard College
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.harvard.drs.verify.service;

import static edu.harvard.drs.verify.utility.KeyUtility.buildKey;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.OcflInventory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * S3 service.
 */
@Slf4j
@Service
public class S3Service {

    private final String bucket;

    private final S3Client s3Client;

    private final ObjectMapper objectMapper;

    /**
     * Autowired S3 service constructor.
     *
     * @param awsConfig AWS config
     */
    @Autowired
    public S3Service(AwsConfig awsConfig) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            awsConfig.getAccessKeyId(),
            awsConfig.getSecretAccessKey()
        );

        S3ClientBuilder builder = S3Client.builder()
            .region(awsConfig.getRegion())
            .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (StringUtils.isNotEmpty(awsConfig.getEndpointOverride())) {
            log.info("AWS endpoint override: {}", awsConfig.getEndpointOverride());
            builder = builder.endpointOverride(URI.create(awsConfig.getEndpointOverride()));
        }

        this.bucket = awsConfig.getBucketName();
        this.s3Client = builder.build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch OCFL inventoy.json from S3 and serialize.
     *
     * @param id DRS id
     * @return serialize OCFL inventory
     * @throws NoSuchKeyException the specified key does not exist
     * @throws InvalidObjectStateException object is archived and inaccessible until restored
     * @throws AwsServiceException something went wrong with S3 request
     * @throws SdkClientException something went wrong with S3 request
     * @throws S3Exception something went wrong with S3 request
     * @throws IOException something went wrong serializing OCFL inventory
     */
    public OcflInventory fetchInventory(Long id) throws NoSuchKeyException, InvalidObjectStateException,
        AwsServiceException, SdkClientException, S3Exception, IOException {

        String key = buildKey(id, "inventory.json");

        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try (InputStream is = this.s3Client.getObject(request, ResponseTransformer.toInputStream())) {
            return this.objectMapper.readValue(is, OcflInventory.class);
        }
    }

    /**
     * Request head object eTag from S3 for given key.
     *
     * @param key S3 object key
     * @return S3 head object eTag
     */
    @Cacheable(value = "etags", sync = true)
    public String getHeadObjectEtag(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        HeadObjectResponse response = this.s3Client.headObject(request);

        return removeEnd(removeStart(response.eTag(), "\""), "\"");
    }

}
