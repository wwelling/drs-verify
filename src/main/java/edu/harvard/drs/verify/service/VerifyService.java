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
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.OcflInventory;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
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
 * Verify service.
 */
@Slf4j
@Service
@RequestScope
public class VerifyService {

    private final String bucket;

    private final S3Client s3;

    private final ObjectMapper om;

    /**
     * Construct verify service.
     */
    @Autowired
    public VerifyService(AwsConfig awsConfig) {
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
        this.s3 = builder.build();
        this.om = new ObjectMapper();
    }

    /**
     * Verify ingest.
     *
     * @param id    object id
     * @param input expected checksum map
     * @throws IOException failed to get inventory
     * @throws VerificationException failed verification
     */
    public void verifyIngest(Long id, Map<String, String> input) throws IOException, VerificationException {
        log.info("Veryfing ingest object {}", id);

        OcflInventory inventory = getInventory(id);

        Map<String, String> wrappedInput = new ConcurrentHashMap<>(input);

        Map<String, VerificationError> errors = new ConcurrentHashMap<>();

        inventory.getManifest()
            .entrySet()
            .parallelStream()
            .forEach(manifest -> {
                for (String manifestEntry : manifest.getValue()) {
                    String key = buildKey(id, manifestEntry);

                    try {
                        HeadObjectResponse response = getHeadObject(key);

                        String actual = removeEnd(removeStart(response.eTag(), "\""), "\"");

                        if (wrappedInput.containsKey(manifestEntry)) {
                            String expected = wrappedInput.remove(manifestEntry);

                            if (!expected.equals(actual)) {
                                VerificationError error = VerificationError.builder()
                                    .error("Checksums do not match")
                                    .expected(expected)
                                    .actual(actual)
                                    .build();

                                errors.put(manifestEntry, error);
                            }
                        } else {
                            errors.put(manifestEntry, VerificationError.from("Missing input checksum"));
                        }

                    } catch (Exception e) {
                        log.error(format("Failed to get head obect of manifest entry %s", key), e);
                        errors.put(manifestEntry, VerificationError.from(e.getMessage()));
                    }
                }
            });

        if (!wrappedInput.isEmpty()) {
            wrappedInput.entrySet()
                .parallelStream()
                .forEach(entry -> {
                    errors.put(entry.getKey(), VerificationError.from("Not found in inventory manifest"));
                });
        }

        if (!errors.isEmpty()) {
            throw new VerificationException(errors);
        }

    }

    /**
     * Verify update.
     *
     * @param id    object id
     * @param input expected checksum map
     * @throws IOException failed to get inventory
     * @throws VerificationException failed verification
     */
    public void verifyUpdate(Long id, Map<String, String> input) throws IOException, VerificationException {
        log.info("Veryfing update object {}", id);

        OcflInventory inventory = getInventory(id);

        Map<String, VerificationError> errors = new ConcurrentHashMap<>();

        input.entrySet()
            .parallelStream()
            .forEach(entry -> {
                String key = buildKey(id, entry.getKey());

                if (inventory.contains(entry.getKey())) {
                    String expected = entry.getValue();

                    try {
                        HeadObjectResponse response = getHeadObject(key);

                        String actual = removeEnd(removeStart(response.eTag(), "\""), "\"");

                        if (!expected.equals(actual)) {
                            VerificationError error = VerificationError.builder()
                                .error("Checksums do not match")
                                .expected(expected)
                                .actual(actual)
                                .build();

                            errors.put(entry.getKey(), error);
                        }

                    } catch (Exception e) {
                        log.error(format("Failed to get head obect of manifest entry %s", key), e);
                        errors.put(entry.getKey(), VerificationError.from(e.getMessage()));
                    }
                } else {
                    errors.put(entry.getKey(), VerificationError.from("Not found in inventory manifest"));
                }
            });

        if (!errors.isEmpty()) {
            throw new VerificationException(errors);
        }

    }

    OcflInventory getInventory(Long id) throws NoSuchKeyException, InvalidObjectStateException,
        AwsServiceException, SdkClientException, S3Exception, IOException {

        String key = buildKey(id, "inventory.json");

        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        try (InputStream is = this.s3.getObject(request, ResponseTransformer.toInputStream())) {
            return this.om.readValue(is, OcflInventory.class);
        }
    }

    HeadObjectResponse getHeadObject(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        return this.s3.headObject(request);
    }

}
