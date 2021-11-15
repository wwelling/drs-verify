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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.drs.verify.AmazonS3TestHelper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * DRS verify service tests.
 */
@SpringBootTest
@TestInstance(PER_CLASS)
@ExtendWith({ S3MockExtension.class })
public class VerifyServiceTest {

    private VerifyService verifyService;

    private ObjectMapper om;

    /**
     * Setup verify service tests.
     *
     * @param s3 s3 client
     * @throws IOException something went wrong
     */
    @BeforeAll
    public void setup(final S3Client s3) throws IOException {
        AmazonS3TestHelper.setup(s3);

        AwsConfig awsConfig = new AwsConfig();
        awsConfig.setBucketName(AmazonS3TestHelper.bucket);
        awsConfig.setEndpointOverride(AmazonS3TestHelper.endpointOverride);
        verifyService = new VerifyService(new S3Service(awsConfig));

        om = new ObjectMapper();
    }

    @AfterAll
    public void cleanup(final S3Client s3) {
        AmazonS3TestHelper.cleanup(s3);
    }

    /**
     * Verify a set of objects.
     *
     * @param id object id
     * @throws IOException could not read verify file
     * @throws VerificationException failed verification
     */
    @ParameterizedTest
    @ValueSource(longs = { 100000020L, 101000305L, 101081248L, 1254624L, 1254654L, 1254709L })
    public void testVerifyIngest(Long id) throws IOException, VerificationException {
        File file = new File(format("src/test/resources/inventory/%s/verify.json", id));
        Map<String, String> input = om.readValue(file, new TypeReference<Map<String, String>>() {});

        verifyService.verifyIngest(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyIngestValidationFailed(final S3Client s3) throws IOException, VerificationException {
        Long id = 1254624L;

        Map<String, String> input = new HashMap<>() {
            {
                put("descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036280");
                put("metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("data/400000254.txt", "872c1b7d198907a3f3f9e6735b32f0ee");
                put("data/9991231.pdf", "32723094875a987b9797dd987ea979712");
            }
        };

        AmazonS3TestHelper.deleteObject(s3, "4264/5210/1254624/v00001/content/data/400000254.txt");

        VerificationException exception = assertThrows(VerificationException.class, () -> {
            verifyService.verifyIngest(id, input);
        });

        assertFalse(exception.getErrors().isEmpty());

        // assertEquals(4, exception.getErrors().size());

        VerificationError checksumError = exception.getErrors().get("descriptor/400000252_mets.xml");
        assertNotNull(checksumError);
        assertEquals("Checksums do not match", checksumError.getError());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036280", checksumError.getExpected());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036279", checksumError.getActual());

        VerificationError missingError = exception.getErrors().get("metadata/400000254_textMD.xml");
        assertNotNull(missingError);
        assertEquals("Missing input checksum", missingError.getError());

        VerificationError s3Error = exception.getErrors().get("data/400000254.txt");
        assertNotNull(s3Error);

        VerificationError unexpectedError = exception.getErrors().get("data/9991231.pdf");
        assertNotNull(unexpectedError);
        assertEquals("Not found in inventory manifest", unexpectedError.getError());

        Path path = Path.of(
            "src/test/resources/inventory",
            valueOf(id),
            "v00001/content/data/400000254.txt"
        );

        AmazonS3TestHelper.putObject(s3, "4264/5210/1254624/v00001/content/data/400000254.txt", path.toFile());
    }

    @Test
    public void testVerifyUpdate() throws IOException, VerificationException {
        Long id = 1254624L;

        Map<String, String> input = new HashMap<>() {
            {
                put("descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036279");
                put("metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
            }
        };

        verifyService.verifyUpdate(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyUpdateValidationFailed(final S3Client s3) throws IOException, VerificationException {
        Long id = 1254624L;

        AmazonS3TestHelper.deleteObject(s3, "4264/5210/1254624/v00001/content/metadata/400000252_structureMap.xml");

        Map<String, String> input = new HashMap<>() {
            {
                put("descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036280");
                put("metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("data/9991231.pdf", "32723094875a987b9797dd987ea979712");
            }
        };

        VerificationException exception = assertThrows(VerificationException.class, () -> {
            verifyService.verifyUpdate(id, input);
        });

        assertFalse(exception.getErrors().isEmpty());

        assertEquals(3, exception.getErrors().size());

        VerificationError checksumError = exception.getErrors().get("descriptor/400000252_mets.xml");
        assertNotNull(checksumError);
        assertEquals("Checksums do not match", checksumError.getError());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036280", checksumError.getExpected());
        assertEquals("52fe5cdbf844ebc72fc5d1e10f036279", checksumError.getActual());

        VerificationError s3Error = exception.getErrors().get("metadata/400000252_structureMap.xml");
        assertNotNull(s3Error);

        VerificationError unexpectedError = exception.getErrors().get("data/9991231.pdf");
        assertNotNull(unexpectedError);
        assertEquals("Not found in inventory manifest", unexpectedError.getError());

        Path path = Path.of(
            "src/test/resources/inventory",
            valueOf(id),
            "v00001/content/metadata/400000252_structureMap.xml"
        );

        AmazonS3TestHelper.putObject(
            s3,
            "4264/5210/1254624/v00001/content/metadata/400000252_structureMap.xml",
            path.toFile()
        );
    }

}
