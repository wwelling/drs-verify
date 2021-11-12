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
import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.drs.verify.AmazonS3TestHelper;
import edu.harvard.drs.verify.config.AwsConfig;
import edu.harvard.drs.verify.dto.OcflInventory;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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
        om = new ObjectMapper();
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
                put("descriptor/400000252_mets.xml", "52fe5cdbf844ebc72fc5d1e10f036279");
                put("metadata/400000254_textMD.xml", "0aff68fa16c9be40ca946f403e4e5180");
                put("metadata/400000252_structureMap.xml", "17e0a42b63075f7a60fa1db80cfe26b9");
                put("data/400000254.txt", "872c1b7d198907a3f3f9e6735b32f0ee");
            }
        };

        verifyService.verifyIngest(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyIngestDereferenceLogicalPath() throws IOException, VerificationException {
        Long id = 1254654L;

        Map<String, String> input = new HashMap<>() {
            {
                put("metadata/400005076_aes57.xml", "563295c50220c295e317cc6f5c37ee8b");
                put("descriptor/400005067_mets.xml", "bea2ca7c6ec1f1b70ef85265aa396dbd");
                put("data/400005079.zip", "4b59de5306cfa1e3797febec199e9708");
                put("data/400005070.wav", "cfd9b3e993c7b030f4807688cddbd77c");
                put("metadata/400005067_structureMap.xml", "fa511483e64be6ef2b7c9e07543df158");
                put("data/400005074.wav", "327b95b2c32f1fa36436c0a12ae5bbf8");
                put("data/400005078.mp3", "1db61ef8c9f1243b80837d52895d688b");
                put("metadata/400005077_aes57.xml", "67aba5ae05507a320f701de7adb60633");
                put("data/400005069.xml", "ece0d3d596258f4215c3bc35b0b36e34");
                put("data/400005072.adl", "a6e126e393d9917e300394d57756ffac");
                // logical path for metadata/400005072_textMD.xml manifest entry
                put("metadata/400005075_textMD.xml", "6f559e7352abee9d8972447eda23cc84");
                put("metadata/400005079_containerMD.xml", "d5504e692af05bceb4e4760b3700c07f");
                put("data/400005075.adl", "60da72142a630e223a205877e29be9c9");
                put("metadata/400005073_aes57.xml", "1f982931f12de9309157b4e891e0abc7");
                put("metadata/400005070_aes57.xml", "db0b43dbf03894bbdb1ddb53768448d9");
                put("metadata/400005069_textMD.xml", "94c551c460d935f3148ebbd06d5412cb");
                put("metadata/400005071_aes57.xml", "cb81e8172c153649be8e5e59236f43c3");
                put("data/400005073.wav", "23a8548521d63065d58dcbafb82dbeb3");
                put("data/400005076.mp3", "610e9c6768c1e41b0f997776861308a1");
                put("metadata/400005074_aes57.xml", "1d50396a886425b9d8c7e50461d7b523");
                put("data/400005077.mp3", "61233f404ce85c80a17f521cab99feb5");
                put("metadata/400005078_aes57.xml", "ed97471a97dc39a13b951afe7a34fb71");
            }
        };

        verifyService.verifyIngest(id, input);

        assertTrue(true);
    }

    @Test
    public void testVerifyIngestWithMultipleVersions() throws IOException, VerificationException {
        Long id = 101000305L;

        Map<String, String> input = new HashMap<>() {
            {
                put("descriptor/400018804_mets.xml", "485cb52bf9838339b89037b233530e1b");
                put("metadata/400018804_structureMap.xml", "1684c7f0313b7b1e83e6c6553e236ae1");
                put("metadata/400018806_mix.xml", "85a1e107b1a6b23ca844cec8e13381f4");
                put("metadata/400018807_textMD.xml", "d75b3f1329e2b56b53676b32b3dbba99");
                put("data/400018806.jp2", "5eab6e472a2fc8363104180ac5f4b37a");
                put("data/400018807.xml", "3a5af24ee7058434a4bcde0dd612fa4a");
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

        assertEquals(4, exception.getErrors().size());

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
    public void testGetInventory() throws IOException {
        Long id = 101000305L;

        OcflInventory inventory = verifyService.getInventory(id);

        assertInventory(inventory);
    }

    @Test
    public void testReduceManifest(final S3Client s3) throws IOException {
        Long id = 101000305L;

        String key = buildKey(id, "inventory.json");

        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(AmazonS3TestHelper.bucket)
            .key(key)
            .build();

        try (InputStream is = s3.getObject(request, ResponseTransformer.toInputStream())) {
            OcflInventory inventory = this.om.readValue(is, OcflInventory.class);

            assertManifestEntryNotNull(
                inventory,
                "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e53001311e"
                + "77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9",
                "v00001/content/metadata/400018807_textMD.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219c5"
                + "79972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d",
                "v00001/content/data/400018806.jp2"
            );

            assertManifestEntryNotNull(
                inventory,
                "3f86fcda2b23ac0a15b57279a36c0ff185176b3873d67e2e81afe5c559c748f2"
                + "8795237eaa721a52122c44b23af09e6203ffca27362570a1e351d372f4e2faf5",
                "v00002/content/descriptor/400018804_mets.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9cd"
                + "ab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122",
                "v00001/content/metadata/400018804_structureMap.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "65494940aebd7d6dab17d3ada273e83e8878bf3403e1b92249a565161139764d"
                + "1f98ede1b55a4ab140e49bf99518e42b1ee3c9752a32e3dd1b84e2da60c9bd72",
                "v00004/content/descriptor/400018804_mets.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "bc6717bc037fb954df066299f73adb56096d308f67158b31bc43983b0f4231e4"
                + "0cf5e571f639738f4e2c332b6306e3a97455942ebcce0bf2459a8739eb3df08e",
                "v00003/content/descriptor/400018804_mets.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc955015d"
                + "81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582",
                "v00001/content/data/400018807.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "c91bb2f4ab2f59c607fa9e0b99f793e16cc881c04f22ef37d8406b4f0b50f58a"
                + "bbe85c033877dfb70f60259d49ffd60f3d1c4254c400d90a8516e85e8d95002e",
                "v00001/content/descriptor/400018804_mets.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e07e"
                + "4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2",
                "v00001/content/metadata/400018806_mix.xml"
            );

            inventory = verifyService.reduceManifest(inventory);

            assertManifestEntryNotNull(
                inventory,
                "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e53001311e"
                + "77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9",
                "v00001/content/metadata/400018807_textMD.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219c5"
                + "79972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d",
                "v00001/content/data/400018806.jp2"
            );

            assertNull(inventory.getManifest().get(
                "3f86fcda2b23ac0a15b57279a36c0ff185176b3873d67e2e81afe5c559c748f2"
                + "8795237eaa721a52122c44b23af09e6203ffca27362570a1e351d372f4e2faf5"
            ));

            assertManifestEntryNotNull(
                inventory,
                "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9cd"
                + "ab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122",
                "v00001/content/metadata/400018804_structureMap.xml"
            );

            assertManifestEntryNotNull(
                inventory,
                "65494940aebd7d6dab17d3ada273e83e8878bf3403e1b92249a565161139764d"
                + "1f98ede1b55a4ab140e49bf99518e42b1ee3c9752a32e3dd1b84e2da60c9bd72",
                "v00004/content/descriptor/400018804_mets.xml"
            );

            assertNull(inventory.getManifest().get(
                "bc6717bc037fb954df066299f73adb56096d308f67158b31bc43983b0f4231e4"
                + "0cf5e571f639738f4e2c332b6306e3a97455942ebcce0bf2459a8739eb3df08e"
            ));

            assertManifestEntryNotNull(
                inventory,
                "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc955015d"
                + "81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582",
                "v00001/content/data/400018807.xml"
            );

            assertNull(inventory.getManifest().get(
                "c91bb2f4ab2f59c607fa9e0b99f793e16cc881c04f22ef37d8406b4f0b50f58a"
                + "bbe85c033877dfb70f60259d49ffd60f3d1c4254c400d90a8516e85e8d95002e"
            ));

            assertManifestEntryNotNull(
                inventory,
                "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e07e"
                + "4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2",
                "v00001/content/metadata/400018806_mix.xml"
            );
        }
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

    private void assertInventory(OcflInventory inventory) {
        assertNotNull(inventory);
        assertEquals("URN-3:HUL.DRS.OBJECT:101000305", inventory.getId());
        assertEquals("https://ocfl.io/1.0/spec/#inventory", inventory.getType());
        assertEquals("sha512", inventory.getDigestAlgorithm());
        assertEquals("v00004", inventory.getHead());
        assertEquals("content", inventory.getContentDirectory());
        assertTrue(inventory.getFixity().isEmpty());
        assertFalse(inventory.getManifest().isEmpty());

        assertEquals(6, inventory.getManifest().size());

        assertTrue(inventory.getManifest().get(
            "ea10c5ce2aea498f1db6878cb99653e963bfe0a010f9570c993bd70b6b64e0"
                + "7e4531d7c30466dfdbe7e6c9c0cb8b3133b8116458e43935ee73822c01bf3768c2"
        ).contains("v00001/content/metadata/400018806_mix.xml"));
        assertTrue(inventory.getManifest().get(
            "65494940aebd7d6dab17d3ada273e83e8878bf3403e1b92249a56516113976"
                + "4d1f98ede1b55a4ab140e49bf99518e42b1ee3c9752a32e3dd1b84e2da60c9bd72"
        ).contains("v00004/content/descriptor/400018804_mets.xml"));
        assertTrue(inventory.getManifest().get(
            "4c4e58c54b8fa8a67e2257a7d5e4000fddd9b24ce3b5eb9c5cdb8a51c13bb9"
                + "cdab7d489996f48a71c088609b2ce0fba18af4815a2e7838f8d4acf1cdb1612122"
        ).contains("v00001/content/metadata/400018804_structureMap.xml"));
        assertTrue(inventory.getManifest().get(
            "c0ccda2feb51af4dfabba25fcd4828dd7e16ca2ad4f8dc64bd30207cc95501"
                + "5d81b8a0bf9873faa2a7418fa89e9348bc4a38ff419ae0ca546fadcb1764447582"
        ).contains("v00001/content/data/400018807.xml"));
        assertTrue(inventory.getManifest().get(
            "2daf8d458f9cba6fcf46daad06d1d9127e0397cd8773aac021b5da50beb219"
                + "c579972cccdc0a6f2e203833fa8778740d0a671ef637b0bd1b37ed9c8f9f2b492d"
        ).contains("v00001/content/data/400018806.jp2"));
        assertTrue(inventory.getManifest().get(
            "2ab46874a9030e55ebc23a7e8fdebdffa25cef1ce23cc417aea7e1e5300131"
                + "1e77a26c4c11c021514d32a00c47546ce7be09f906de997808bff7950a2b744da9"
        ).contains("v00001/content/metadata/400018807_textMD.xml"));

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

    private void assertManifestEntryNotNull(OcflInventory inventory, String sha512, String key) {
        assertNotNull(inventory.getManifest().get(sha512));

        assertEquals(
            inventory.getManifest().get(sha512),
            Arrays.asList(new String[] { key })
        );
    }

}
