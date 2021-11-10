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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<String, List<String>> manifest = new ConcurrentHashMap<>();
    private Map<String, OcflVersion> versions = new HashMap<>();

    public OcflInventory withManifest(ConcurrentHashMap<String, List<String>> manifest) {
        this.manifest = manifest;
        return this;
    }

    /**
     * Find path in manifest ending in reduced path, removing entry if found.
     *
     * @param reducedPath reduced path
     * @return path in manifest
     */
    public Optional<String> find(String reducedPath) {
        Optional<Entry<String, List<String>>> manifestEntry = manifest.entrySet()
            .parallelStream()
            .filter(entry -> entry.getValue()
                .stream()
                .anyMatch(value -> value.endsWith(reducedPath)))
            .findFirst();

        if (!manifestEntry.isPresent()) {
            manifestEntry = derefernece(reducedPath);
        }

        if (manifestEntry.isPresent()) {
            manifest.remove(manifestEntry.get().getKey());
        }

        return manifestEntry.map(entry -> entry.getValue().get(0));
    }

    private Optional<Entry<String, List<String>>> derefernece(String reducedPath) {
        return versions.entrySet()
            .parallelStream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
            .map(version -> version.getValue()
                .find(reducedPath)
                .filter(key -> this.manifest.containsKey(key))
                .map(key -> Map.entry(key, this.manifest.get(key))))
            .filter(entry -> entry.isPresent())
            .map(entry -> entry.get())
            .findFirst();
    }
}
