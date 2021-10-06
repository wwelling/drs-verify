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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edu.harvard.drs.verify.AmazonS3TestHelper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.OcflInventory;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

/**
 * DRS verify service tests.
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ExtendWith({ S3MockExtension.class })
public class VerifyServiceTest {

    private AwsConfig awsConfig;

    private VerifyService verifyService;

    /**
     * Setup verify service tests.
     *
     * @param s3 s3 client
     * @throws JsonParseException couldn't parse
     * @throws JsonMappingException couldn't map
     * @throws IOException something went wrong
     */
    @BeforeAll
    public void setup(final S3Client s3) throws JsonParseException, JsonMappingException, IOException {
        AmazonS3TestHelper.setup(s3);

        awsConfig = new AwsConfig();
        awsConfig.setBucketName(AmazonS3TestHelper.bucket);
        awsConfig.setEndpointOverride(AmazonS3TestHelper.endpointOverride);
        verifyService = new VerifyService(awsConfig);
    }

    @AfterAll
    public void cleanup(final S3Client s3) {
        AmazonS3TestHelper.cleanup(s3);
    }

    @Test
    public void testGetInventory() throws IOException {
        Long id = 1254624L;

        OcflInventory inventory = verifyService.getInventory(id);

        assertNotNull(inventory);
    }

    @Test
    public void testSetupStagingDirectory() throws IOException {
        Path path = Path.of("target/inventory/1254624/inventory.json");

        verifyService.setupStagingDirectory(path);

        assertTrue(path.getParent().toFile().exists());

        verifyService.cleanupStagingDirectory(path);
    }

    @Test
    public void testDownloadObject() throws IOException {
        String key = "1254624/inventory.json";
        Path path = Path.of("target/inventory", key);

        verifyService.setupStagingDirectory(path);

        verifyService.downloadObject(key, path);

        assertTrue(path.toFile().exists());

        verifyService.cleanupStagingDirectory(path);
    }

    @Test
    public void testReadInventoryFile() throws IOException {
        Path path = Path.of("src/test/resources/inventory/1254624/inventory.json");

        OcflInventory inventory = verifyService.readInventoryFile(path);

        assertNotNull(inventory);
    }

    @Test
    public void testCleanupStagingDirectory() throws IOException {
        Path path = Path.of("target/inventory/1254624/inventory.json");

        verifyService.setupStagingDirectory(path);

        verifyService.cleanupStagingDirectory(path);

        assertFalse(path.getParent().toFile().exists());
    }

    @Test
    public void testGetHeadObject() {
        HeadObjectResponse response = verifyService.getHeadObject("1254624/v00001/content/data/400000254.txt");

        assertEquals("text/plain", response.contentType());
        assertEquals(611864L, response.contentLength());
        assertEquals("872c1b7d198907a3f3f9e6735b32f0ee", AmazonS3TestHelper.normalizeEtag(response.eTag()));
    }

}
