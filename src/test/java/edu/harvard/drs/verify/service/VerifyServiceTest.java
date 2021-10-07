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

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import edu.harvard.drs.verify.AmazonS3TestHelper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.OcflInventory;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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
    public void testVerifyIngest() throws IOException, VerificationException {
        Long id = 1254624L;

        Map<String, String> input = new HashMap<>() {
            {
                put("v00001/content/descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036279");
                put("v00001/content/metadata/400000254_textMD.xml", "0aff68fa16c9be40ca946f403e4e5180");
                put("v00001/content/metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("v00001/content/data/400000254.txt", "872c1b7d198907a3f3f9e6735b32f0ee");
            }
        };

        verifyService.verifyIngest(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyIngestValidationFailed(final S3Client s3) throws IOException, VerificationException {
        Long id = 1254624L;

        Map<String, String> input = new HashMap<>() {
            {
                put("v00001/content/descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036280");
                put("v00001/content/metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("v00001/content/data/400000254.txt", "872c1b7d198907a3f3f9e6735b32f0ee");
                put("v00001/content/data/9991231.pdf", "32723094875a987b9797dd987ea979712");
            }
        };
        
        AmazonS3TestHelper.deleteObject(s3, "1254624/v00001/content/data/400000254.txt");

        VerificationException exception = assertThrows(VerificationException.class, () -> {
            verifyService.verifyIngest(id, input);
        });

        assertFalse(exception.getErrors().isEmpty());

        assertEquals(4, exception.getErrors().size());

        VerificationError checksumError = exception.getErrors().get("v00001/content/descriptor/400000252_mets.xml");
        assertNotNull(checksumError);
        assertEquals("Checksums do not match", checksumError.getError());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036280", checksumError.getExpected());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036279", checksumError.getActual());

        VerificationError missingError = exception.getErrors().get("v00001/content/metadata/400000254_textMD.xml");
        assertNotNull(missingError);
        assertEquals("Missing input checksum", missingError.getError());

        VerificationError s3Error = exception.getErrors().get("v00001/content/data/400000254.txt");
        assertNotNull(s3Error);

        VerificationError unexpectedError = exception.getErrors().get("v00001/content/data/9991231.pdf");
        assertNotNull(unexpectedError);
        assertEquals("Not found in inventory manifest", unexpectedError.getError());

        Path path = Path.of(
            "src/test/resources/inventory",
            valueOf(id),
            "v00001/content/data/400000254.txt"
        );

        AmazonS3TestHelper.putObject(s3, "1254624/v00001/content/data/400000254.txt", path.toFile());
    }

    @Test
    public void testVerifyUpdate() throws IOException, VerificationException {
        Long id = 1254624L;

        Map<String, String> input = new HashMap<>() {
            {
                put("v00001/content/descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036279");
                put("v00001/content/metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
            }
        };

        verifyService.verifyUpdate(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyUpdateValidationFailed(final S3Client s3) throws IOException, VerificationException {
        Long id = 1254624L;

        AmazonS3TestHelper.deleteObject(s3, "1254624/v00001/content/metadata/400000252_structureMap.xml");

        Map<String, String> input = new HashMap<>() {
            {
                put("v00001/content/descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036280");
                put("v00001/content/metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("v00001/content/data/9991231.pdf", "32723094875a987b9797dd987ea979712");
            }
        };

        VerificationException exception = assertThrows(VerificationException.class, () -> {
            verifyService.verifyUpdate(id, input);
        });

        assertFalse(exception.getErrors().isEmpty());

        assertEquals(3, exception.getErrors().size());

        VerificationError checksumError = exception.getErrors().get("v00001/content/descriptor/400000252_mets.xml");
        assertNotNull(checksumError);
        assertEquals("Checksums do not match", checksumError.getError());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036280", checksumError.getExpected());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036279", checksumError.getActual());

        VerificationError s3Error = exception.getErrors().get("v00001/content/metadata/400000252_structureMap.xml");
        assertNotNull(s3Error);

        VerificationError unexpectedError = exception.getErrors().get("v00001/content/data/9991231.pdf");
        assertNotNull(unexpectedError);
        assertEquals("Not found in inventory manifest", unexpectedError.getError());

        Path path = Path.of(
            "src/test/resources/inventory",
            valueOf(id),
            "v00001/content/metadata/400000252_structureMap.xml"
        );

        AmazonS3TestHelper.putObject(s3, "1254624/v00001/content/metadata/400000252_structureMap.xml", path.toFile());
    }

    @Test
    public void testGetInventory() throws IOException {
        Long id = 1254624L;

        OcflInventory inventory = verifyService.getInventory(id);

        assertInventory(inventory);
    }

    @Test
    public void testSetupStagingDirectory() throws IOException {
        Path path = Path.of("target/inventory/1254624/inventory.json");

        verifyService.setupStagingDirectory(path);

        assertTrue(path.getParent().toFile().exists());

        verifyService.cleanupStagingDirectory(path);
    }

    @Test
    public void testSetupStagingDirectoryFailure() throws IOException {
        Path path = Path.of("target/inventory/1254624/inventory.json");

        verifyService.setupStagingDirectory(path);

        IOException exception = assertThrows(IOException.class, () -> {
            verifyService.setupStagingDirectory(path);
        });

        String expected = format(
            "Failed to create staging directory %s",
            path.toFile().getParentFile().getPath()
        );

        assertEquals(expected, exception.getMessage());

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

        assertInventory(inventory);
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

    private void assertInventory(OcflInventory inventory) {
        assertNotNull(inventory);
        assertEquals("URN-3:HUL.DRS.OBJECT:1254624", inventory.getId());
        assertEquals("https://ocfl.io/1.0/spec/#inventory", inventory.getType());
        assertEquals("sha512", inventory.getDigestAlgorithm());
        assertEquals("v00001", inventory.getHead());
        assertEquals("content", inventory.getContentDirectory());
        assertTrue(inventory.getFixity().isEmpty());
        assertFalse(inventory.getManifest().isEmpty());
        assertTrue(inventory.getManifest().containsKey(
            "3fd49ddf921491702972a9386854a42d06cc7c9a8d50dac140b86936abc925e8"
            + "fb10174d336dd1f88cb001fe4c8426e75d140ddbc067bbfb04bcb2f5b5fcc259"
        ));
        assertTrue(inventory.getManifest().containsKey(
            "a98e03107a45f8ce9e94d0a12d266dd99900597b126ed96ecbf22256fe0b09f2"
            + "8b6b586a0b696535b112ee3578baa3c69f7ff0c23616ffc515a95bee71fbe68c"
        ));
        assertTrue(inventory.getManifest().containsKey(
            "fad537f28838500f1e232b980a3123852a0c60e1a57b22e505076a622b084dab"
            + "01c67383f3df17274d771b2f2e0f6a1c38eb19a66512bc85dd432dbac7cf34a7"
        ));
        assertTrue(inventory.getManifest().containsKey(
            "fdea3768ba483e0b54c9b6ccedbcedf01443d62a47272819e4715d46b990650f"
            + "5d03827bd116ddc723155332cea220c9a2533ce8427c9e6d3913ee110f478218"
        ));
        assertTrue(inventory.getManifest().get(
            "3fd49ddf921491702972a9386854a42d06cc7c9a8d50dac140b86936abc925e8"
            + "fb10174d336dd1f88cb001fe4c8426e75d140ddbc067bbfb04bcb2f5b5fcc259"
        ).contains("v00001/content/metadata/400000252_structureMap.xml"));
        assertTrue(inventory.getManifest().get(
            "a98e03107a45f8ce9e94d0a12d266dd99900597b126ed96ecbf22256fe0b09f2"
            + "8b6b586a0b696535b112ee3578baa3c69f7ff0c23616ffc515a95bee71fbe68c"
        ).contains("v00001/content/descriptor/400000252_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "fad537f28838500f1e232b980a3123852a0c60e1a57b22e505076a622b084dab"
            + "01c67383f3df17274d771b2f2e0f6a1c38eb19a66512bc85dd432dbac7cf34a7"
        ).contains("v00001/content/metadata/400000254_textMD.xml"));
        assertTrue(inventory.getManifest().get(
            "fdea3768ba483e0b54c9b6ccedbcedf01443d62a47272819e4715d46b990650f"
            + "5d03827bd116ddc723155332cea220c9a2533ce8427c9e6d3913ee110f478218"
        ).contains("v00001/content/data/400000254.txt"));
        assertFalse(inventory.getVersions().isEmpty());
        assertTrue(inventory.getVersions().containsKey("v00001"));
        assertEquals("2021-07-14T14:59:16.98723Z", inventory.getVersions().get("v00001").getCreated());
        assertEquals("PREMIS:refreshment", inventory.getVersions().get("v00001").getMessage());
        assertEquals("DRS Migrator/2.0.1", inventory.getVersions().get("v00001").getUser().getName());
        assertEquals(
            "http://idtest.lib.harvard.edu:10020/wordshack/software/26906",
            inventory.getVersions().get("v00001").getUser().getAddress()
        );
        assertTrue(inventory.getVersions().get("v00001").getState().containsKey(
            "3fd49ddf921491702972a9386854a42d06cc7c9a8d50dac140b86936abc925e8"
            + "fb10174d336dd1f88cb001fe4c8426e75d140ddbc067bbfb04bcb2f5b5fcc259"
        ));
        assertTrue(inventory.getVersions().get("v00001").getState().containsKey(
            "a98e03107a45f8ce9e94d0a12d266dd99900597b126ed96ecbf22256fe0b09f2"
            + "8b6b586a0b696535b112ee3578baa3c69f7ff0c23616ffc515a95bee71fbe68c"
        ));
        assertTrue(inventory.getVersions().get("v00001").getState().containsKey(
            "fad537f28838500f1e232b980a3123852a0c60e1a57b22e505076a622b084dab"
            + "01c67383f3df17274d771b2f2e0f6a1c38eb19a66512bc85dd432dbac7cf34a7"
        ));
        assertTrue(inventory.getVersions().get("v00001").getState().containsKey(
            "fdea3768ba483e0b54c9b6ccedbcedf01443d62a47272819e4715d46b990650f"
            + "5d03827bd116ddc723155332cea220c9a2533ce8427c9e6d3913ee110f478218"
        ));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "3fd49ddf921491702972a9386854a42d06cc7c9a8d50dac140b86936abc925e8"
            + "fb10174d336dd1f88cb001fe4c8426e75d140ddbc067bbfb04bcb2f5b5fcc259"
        ).contains("metadata/400000252_structureMap.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "a98e03107a45f8ce9e94d0a12d266dd99900597b126ed96ecbf22256fe0b09f2"
            + "8b6b586a0b696535b112ee3578baa3c69f7ff0c23616ffc515a95bee71fbe68c"
        ).contains("descriptor/400000252_mets.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "fad537f28838500f1e232b980a3123852a0c60e1a57b22e505076a622b084dab"
            + "01c67383f3df17274d771b2f2e0f6a1c38eb19a66512bc85dd432dbac7cf34a7"
        ).contains("metadata/400000254_textMD.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "fdea3768ba483e0b54c9b6ccedbcedf01443d62a47272819e4715d46b990650f"
            + "5d03827bd116ddc723155332cea220c9a2533ce8427c9e6d3913ee110f478218"
        ).contains("data/400000254.txt"));
    }

}
