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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * OCFL inventory.
 */
@Data
public class OcflInventory {
    private String id;
    private String type;
    private String digestAlgorithm;
    private String head;
    private String contentDirectory;
    private Map<String, Map<String, List<String>>> fixity = new HashMap<>();
    private Map<String, List<String>> manifest = new HashMap<>();
    private Map<String, OcflVersion> versions = new HashMap<>();
}
