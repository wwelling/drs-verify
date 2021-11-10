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

package edu.harvard.drs.verify.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * OCFL inventory tests.
 */
public class OcflInventoryTest {

    @Test
    public void testOcflInventoryFind() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = Path.of(
            "src/test/resources/inventory/1254624/inventory.json"
        ).toFile();
        OcflInventory inventory = objectMapper.readValue(file, OcflInventory.class);

        assertTrue(inventory.find("metadata/400000252_structureMap.xml").isPresent());
        assertTrue(inventory.find("descriptor/400000252_mets.xml").isPresent());
        assertTrue(inventory.find("metadata/400000254_textMD.xml").isPresent());
        assertTrue(inventory.find("data/400000254.txt").isPresent());
        assertFalse(inventory.find("data/974358.pdf").isPresent());
    }

    @Test
    public void testOcflInventoryFindWithLogicalPath()
        throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = Path.of(
            "src/test/resources/inventory/1254654/inventory.json"
        ).toFile();
        OcflInventory inventory = objectMapper.readValue(file, OcflInventory.class);

        assertTrue(inventory.find("metadata/400005076_aes57.xml").isPresent());
        assertTrue(inventory.find("descriptor/400005067_mets.xml").isPresent());
        assertTrue(inventory.find("data/400005079.zip").isPresent());
        assertTrue(inventory.find("data/400005070.wav").isPresent());
        assertTrue(inventory.find("metadata/400005067_structureMap.xml").isPresent());
        assertTrue(inventory.find("data/400005074.wav").isPresent());
        assertTrue(inventory.find("data/400005078.mp3").isPresent());
        assertTrue(inventory.find("metadata/400005077_aes57.xml").isPresent());
        assertTrue(inventory.find("data/400005069.xml").isPresent());
        assertTrue(inventory.find("data/400005072.adl").isPresent());
        // logical path for metadata/400005072_textMD.xml manifest entry
        assertTrue(inventory.find("metadata/400005075_textMD.xml").isPresent());
        assertTrue(inventory.find("metadata/400005079_containerMD.xml").isPresent());
        assertTrue(inventory.find("data/400005075.adl").isPresent());
        assertTrue(inventory.find("metadata/400005073_aes57.xml").isPresent());
        assertTrue(inventory.find("metadata/400005070_aes57.xml").isPresent());
        assertTrue(inventory.find("metadata/400005069_textMD.xml").isPresent());
        assertTrue(inventory.find("metadata/400005071_aes57.xml").isPresent());
        assertTrue(inventory.find("data/400005073.wav").isPresent());
        assertTrue(inventory.find("data/400005076.mp3").isPresent());
        assertTrue(inventory.find("metadata/400005074_aes57.xml").isPresent());
        assertTrue(inventory.find("data/400005077.mp3").isPresent());
        assertTrue(inventory.find("metadata/400005078_aes57.xml").isPresent());
    }

}
