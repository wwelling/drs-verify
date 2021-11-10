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
import java.util.Optional;
import lombok.Data;

/**
 * OCFL version.
 */
@Data
public class OcflVersion {
    private String created;
    private String message;
    private OcflUser user;
    private Map<String, List<String>> state = new HashMap<>();

    /**
     * Find version entry key in state containing reduced key.
     *
     * @param reducedKey reduced key
     * @return version entry key in state
     */
    public Optional<String> find(String reducedKey) {
        return state.entrySet()
            .parallelStream()
            .filter(entry -> entry.getValue()
                .stream()
                .anyMatch(value -> value.equals(reducedKey)))
            .findFirst()
            .map(entry -> entry.getKey());
    }
}
