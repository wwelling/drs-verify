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

package edu.harvard.drs.verify.controller;

import static java.lang.String.format;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import edu.harvard.drs.verify.AmazonS3TestHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * DRS verify controller tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(PER_CLASS)
@ExtendWith({ S3MockExtension.class })
public class VerifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Setup verify controller tests.
     *
     * @param s3 s3 client
     * @throws IOException something went wrong
     */
    @BeforeAll
    public void setup(final S3Client s3) throws IOException {
        AmazonS3TestHelper.setup(s3);
    }

    @AfterAll 
    public void cleanup(final S3Client s3) {
        AmazonS3TestHelper.cleanup(s3);
    }

    /**
     * Verify a set of objects.
     *
     * @param id object id
     * @throws Exception something went wrong
     */
    @ParameterizedTest
    @ValueSource(longs = { 100000020L, 101000305L, 101081248L, 1254624L, 1254654L, 1254709L })
    public void shouldVerify(Long id) throws Exception {
        Path path = Paths.get(format("src/test/resources/inventory/%s/verify.json", id));
        String content = new String(Files.readAllBytes(path));
        this.mockMvc.perform(post(format("/verify/%s", id))
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldVerifyBadRequestMalformed() throws Exception {
        String content = "{"
            + "\"descriptor/400016240_mets.xml\": {"
            + "\"data/400016242.doc\": \"f9f645a42c784c2b3d2fe93ccbaf1992\""
            + "}"
            + "\"metadata/400016240_structureMap.xml\": \"06328e877392db47a2b59bfa9614470c\","
            + "\"metadata/400016240_mods.xml\": \"2cffede56db677e4924b24622374ac3b\""
            + "}";
        this.mockMvc.perform(post("/verify/100000020")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVerifyBadRequest() throws Exception {
        this.mockMvc.perform(post("/verify/100000020")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVerifyConflict() throws Exception {
        String content = "{"
            + "\"descriptor/400016240_mets.xml\": \"88004448277e0ca3229808bd8fa403ab\","
            + "\"data/400016242.doc\": \"f9f645a42c784c2b3d2fe93ccbaf1992\","
            + "\"metadata/400016242_documentMD.xml\": \"68322df10a439fc9b03bb6e69c72749f\","
            + "\"metadata/400016240_structureMap.xml\": \"06328e877392db47a2b59bfa9614470c\","
            + "\"metadata/400016240_mods.xml\": \"2cffede56db677e4924b24622374ac3b\""
            + "}";
        this.mockMvc.perform(post("/verify/100000020")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());
    }

    @Test
    public void shouldVerifyNotFound() throws Exception {
        String content = "{}";
        this.mockMvc.perform(post("/verify/4265456")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void shouldVerifyUpdate() throws Exception {
        String content = "{"
            + "\"descriptor/400016240_mets.xml\": \"88004448277e0ca3229808bd8fa40327\","
            + "\"metadata/400016240_structureMap.xml\": \"06328e877392db47a2b59bfa9614470c\","
            + "\"metadata/400016240_mods.xml\": \"2cffede56db677e4924b24622374ac3b\""
            + "}";
        this.mockMvc.perform(post("/verify/100000020/update")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    public void shouldVerifyUpdateBadRequestMalformed() throws Exception {
        String content = "{"
            + "\"descriptor/400016240_mets.xml\": {"
            + "\"data/400016242.doc\": \"f9f645a42c784c2b3d2fe93ccbaf1992\""
            + "}"
            + "}";
        this.mockMvc.perform(post("/verify/100000020/update")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVerifyUpdateBadRequest() throws Exception {
        this.mockMvc.perform(post("/verify/100000020/update")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldVerifyUpdateConflict() throws Exception {
        String content = "{"
            + "\"descriptor/400016240_mets.xml\": \"88004448277e0ca3229808bd8fa403ab\","
            + "\"data/400016242.doc\": \"f9f645a42c784c2b3d2fe93ccbaf1992\","
            + "\"metadata/400016240_mods.xml\": \"2cffede56db677e4924b24622374ac3b\""
            + "}";
        this.mockMvc.perform(post("/verify/100000020/update")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict());
    }

    @Test
    public void shouldVerifyUpdateNotFound() throws Exception {
        String content = "{}";
        this.mockMvc.perform(post("/verify/4265456/update")
            .content(content)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

}
