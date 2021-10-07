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

package edu.harvard.drs.verify;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.drs.verify.dto.OcflInventory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * Amazon S3 test helper.
 */
public final class AmazonS3TestHelper {

    public static final String endpointOverride = "http://localhost:9090";

    public static final String bucket = "test-preservation";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Setup object store.
     *
     * @param s3 s3 client
     * @throws IOException something went wrong
     */
    public static void setup(final S3Client s3) 
        throws JsonParseException, JsonMappingException, IOException {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
            .bucket(bucket)
            .build();

        s3.createBucket(createBucketRequest);

        populate(s3);

        s3.close();
    }

    /**
     * Cleanup objects store.
     *
     * @param s3 s3 client
     */
    public static void cleanup(final S3Client s3) {
        ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder()
            .bucket(bucket)
            .build();

        ListObjectsV2Iterable iterable = s3.listObjectsV2Paginator(listObjectsV2Request);

        List<ObjectIdentifier> identifiers = iterable.contents().stream()
            .map(o -> ObjectIdentifier.builder().key(o.key()).build())
            .collect(Collectors.toList());

        if (!identifiers.isEmpty()) {
            Delete delete = Delete.builder()
                .objects(identifiers)
                .build();

            DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(delete)
                .build();

            DeleteObjectsResponse response = s3.deleteObjects(deleteObjectsRequest);

            assertTrue(response.hasDeleted());
            assertFalse(response.hasErrors());
        }

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
            .bucket(bucket)
            .build();

        s3.deleteBucket(deleteBucketRequest);

        s3.close();
    }

    /**
     * Normalize eTag.
     *
     * @param etag eTag
     * @return normalized eTag
     */
    public static String normalizeEtag(String etag) {
        return removeEnd(removeStart(etag, "\""), "\"");
    }

    /**
     * Put object in S3.
     *
     * @param s3     s3 client
     * @param key    desired object key
     * @param file   file to store
     */
    public static void putObject(final S3Client s3, String key, File file) {
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();

        PutObjectResponse response = s3.putObject(request, RequestBody.fromFile(file));

        assertEquals(md5Hex(file), normalizeEtag(response.eTag()));
    }

    /**
     * Delete object in S3.
     *
     * @param s3  s3 client
     * @param key object key
     */
    public static void deleteObject(final S3Client s3, String key) {
        ObjectIdentifier identifier = ObjectIdentifier.builder()
            .key(key)
            .build();

        Delete delete = Delete.builder()
            .objects(identifier)
            .build();

        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(bucket)
            .delete(delete)
            .build();

        DeleteObjectsResponse response = s3.deleteObjects(request);

        assertTrue(response.hasDeleted());
        assertFalse(response.hasErrors());
    }

    private static void populate(final S3Client s3)
        throws JsonParseException, JsonMappingException, IOException {
        Path path = Path.of("src/test/resources/inventory");

        for (File inventoryRoot : path.toFile().listFiles(File::isDirectory)) {
            String id = inventoryRoot.getName();

            String inventoryKey = format("%s/inventory.json", id);

            File inventoryFile = path.resolve(inventoryKey).toFile();

            putObject(s3, inventoryKey, inventoryFile);

            OcflInventory inventory = objectMapper.readValue(inventoryFile, OcflInventory.class);

            inventory.getManifest()
                .entrySet()
                .parallelStream()
                .forEach(manifest -> {
                    for (String manifestEntry : manifest.getValue()) {
                        String key = format("%s/%s", id, manifestEntry);

                        File file = path.resolve(key).toFile();

                        putObject(s3, key, file);
                    }

                });

        }
    }

    private static String md5Hex(File file) {
        try (InputStream is = new FileInputStream(file)) {
            return DigestUtils.md5Hex(is);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
