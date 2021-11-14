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
import edu.harvard.drs.verify.dto.OcflInventory;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

        awsConfig = new AwsConfig();
        awsConfig.setBucketName(AmazonS3TestHelper.bucket);
        awsConfig.setEndpointOverride(AmazonS3TestHelper.endpointOverride);
        verifyService = new VerifyService(awsConfig);

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

    @Test
    public void testFetchInventory() throws IOException {
        Long id = 101000305L;

        OcflInventory inventory = verifyService.fetchInventory(id);

        assertNotNull(inventory);
        assertEquals("URN-3:HUL.DRS.OBJECT:101000305", inventory.getId());
        assertEquals("https://ocfl.io/1.0/spec/#inventory", inventory.getType());
        assertEquals("sha512", inventory.getDigestAlgorithm());
        assertEquals("v00004", inventory.getHead());
        assertEquals("content", inventory.getContentDirectory());
        assertTrue(inventory.getFixity().isEmpty());
        assertFalse(inventory.getManifest().isEmpty());

        assertEquals(9, inventory.getManifest().size());

        assertTrue(inventory.getManifest().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("v00001/content/metadata/400018807_textMD.xml"));
        assertTrue(inventory.getManifest().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("v00001/content/data/400018806.jp2"));
        assertTrue(inventory.getManifest().get(
            "3f86fcda2b23ac0a15b57279a36c0ff185176b3873d67e2e81afe5c559c748"
                + "f28795237eaa721a52122c44b23af09e6203ffca27362570a1e351d372f4e2faf5"
        ).contains("v00002/content/descriptor/400018804_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("v00001/content/metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getManifest().get(
            "65494940aebd7d6dab17d3ada273e83e8878bf3403e1b92249a56516113976"
                + "4d1f98ede1b55a4ab140e49bf99518e42b1ee3c9752a32e3dd1b84e2da60c9bd72"
        ).contains("v00004/content/descriptor/400018804_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "bc6717bc037fb954df066299f73adb56096d308f67158b31bc43983b0f4231"
                + "e40cf5e571f639738f4e2c332b6306e3a97455942ebcce0bf2459a8739eb3df08e"
        ).contains("v00003/content/descriptor/400018804_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("v00001/content/data/400018807.xml"));
        assertTrue(inventory.getManifest().get(
            "c91bb2f4ab2f59c607fa9e0b99f793e16cc881c04f22ef37d8406b4f0b50f5"
                + "8abbe85c033877dfb70f60259d49ffd60f3d1c4254c400d90a8516e85e8d95002e"
        ).contains("v00001/content/descriptor/400018804_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("v00001/content/metadata/400018806_mix.xml"));

        List<String> versionKeys = new ArrayList<>(inventory.getVersions().keySet());

        assertEquals("v00004", versionKeys.get(0));
        assertEquals("2021-10-25T21:00:07.88546Z", inventory.getVersions().get("v00004").getCreated());
        assertEquals("PREMIS:metadata modification", inventory.getVersions().get("v00004").getMessage());
        assertEquals("DRS2 Services/latest", inventory.getVersions().get("v00004").getUser().getName());
        assertEquals(
            "http://idtest.lib.harvard.edu:10020/wordshack/software/26986",
            inventory.getVersions().get("v00004").getUser().getAddress()
        );
        assertEquals(6, inventory.getVersions().get("v00004").getState().size());

        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("metadata/400018807_textMD.xml"));
        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("data/400018806.jp2"));
        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "65494940aebd7d6dab17d3ada273e83e8878bf3403e1b92249a56516113976"
                + "4d1f98ede1b55a4ab140e49bf99518e42b1ee3c9752a32e3dd1b84e2da60c9bd72"
        ).contains("descriptor/400018804_mets.xml"));
        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("data/400018807.xml"));
        assertTrue(inventory.getVersions().get("v00004").getState().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("metadata/400018806_mix.xml"));

        assertEquals("v00003", versionKeys.get(1));
        assertEquals("2021-10-22T13:50:52.911295Z", inventory.getVersions().get("v00003").getCreated());
        assertEquals("PREMIS:metadata modification", inventory.getVersions().get("v00003").getMessage());
        assertEquals("DRS2 Services/latest", inventory.getVersions().get("v00003").getUser().getName());
        assertEquals(
            "http://idtest.lib.harvard.edu:10020/wordshack/software/26986",
            inventory.getVersions().get("v00003").getUser().getAddress()
        );
        assertEquals(6, inventory.getVersions().get("v00003").getState().size());

        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("metadata/400018807_textMD.xml"));
        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("data/400018806.jp2"));
        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "bc6717bc037fb954df066299f73adb56096d308f67158b31bc43983b0f4231"
                + "e40cf5e571f639738f4e2c332b6306e3a97455942ebcce0bf2459a8739eb3df08e"
        ).contains("descriptor/400018804_mets.xml"));
        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("data/400018807.xml"));
        assertTrue(inventory.getVersions().get("v00003").getState().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("metadata/400018806_mix.xml"));

        assertEquals("v00002", versionKeys.get(2));
        assertEquals("2021-10-21T18:50:08.010715Z", inventory.getVersions().get("v00002").getCreated());
        assertEquals("PREMIS:metadata modification", inventory.getVersions().get("v00002").getMessage());
        assertEquals("DRS2 Services/latest", inventory.getVersions().get("v00002").getUser().getName());
        assertEquals(
            "http://idtest.lib.harvard.edu:10020/wordshack/software/26986",
            inventory.getVersions().get("v00002").getUser().getAddress()
        );
        assertEquals(6, inventory.getVersions().get("v00002").getState().size());

        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("metadata/400018807_textMD.xml"));
        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("data/400018806.jp2"));
        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "3f86fcda2b23ac0a15b57279a36c0ff185176b3873d67e2e81afe5c559c748"
                + "f28795237eaa721a52122c44b23af09e6203ffca27362570a1e351d372f4e2faf5"
        ).contains("descriptor/400018804_mets.xml"));
        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("data/400018807.xml"));
        assertTrue(inventory.getVersions().get("v00002").getState().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("metadata/400018806_mix.xml"));

        assertEquals("v00001", versionKeys.get(3));
        assertEquals("2021-10-20T02:45:19.597393Z", inventory.getVersions().get("v00001").getCreated());
        assertEquals("PREMIS:refreshment", inventory.getVersions().get("v00001").getMessage());
        assertEquals("DRS Migrator/2.0.1", inventory.getVersions().get("v00001").getUser().getName());
        assertEquals(
            "http://idtest.lib.harvard.edu:10020/wordshack/software/26906",
            inventory.getVersions().get("v00001").getUser().getAddress()
        );
        assertEquals(6, inventory.getVersions().get("v00001").getState().size());

        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("metadata/400018807_textMD.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("data/400018806.jp2"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("data/400018807.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "c91bb2f4ab2f59c607fa9e0b99f793e16cc881c04f22ef37d8406b4f0b50f5"
                + "8abbe85c033877dfb70f60259d49ffd60f3d1c4254c400d90a8516e85e8d95002e"
        ).contains("descriptor/400018804_mets.xml"));
        assertTrue(inventory.getVersions().get("v00001").getState().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("metadata/400018806_mix.xml"));
    }

    @Test
    public void testGetHeadObject() {
        HeadObjectResponse response = verifyService.getHeadObject(
            "4264/5210/1254624/v00001/content/data/400000254.txt"
        );

        assertEquals("text/plain", response.contentType());
        assertEquals(611864L, response.contentLength());
        assertEquals("872c1b7d198907a3f3f9e6735b32f0ee", AmazonS3TestHelper.normalizeEtag(response.eTag()));
    }

}
