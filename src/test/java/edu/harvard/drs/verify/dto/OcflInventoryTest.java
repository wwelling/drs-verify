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
    public void testOcflInventoryTestContains() throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = Path.of(
            "src/test/resources/inventory/1254624/inventory.json"
        ).toFile();
        OcflInventory inventory = objectMapper.readValue(file, OcflInventory.class);

        assertTrue(inventory.contains("v00001/content/metadata/400000252_structureMap.xml"));
        assertTrue(inventory.contains("v00001/content/descriptor/400000252_mets.xml"));
        assertTrue(inventory.contains("v00001/content/metadata/400000254_textMD.xml"));
        assertTrue(inventory.contains("v00001/content/data/400000254.txt"));
        assertFalse(inventory.contains("v00001/content/data/974358.pdf"));
    }

}
