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

import static java.lang.String.format;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
     * Find key in manifest ending in reduced key removing if found.
     *
     * @param reducedKey reduced key
     * @return key in manifest
     */
    public Optional<String> find(String reducedKey) {
        Optional<VersionEntry> versionEntry = manifest.entrySet()
            .parallelStream()
            .filter(entry -> entry.getValue()
                .stream()
                .anyMatch(value -> value.endsWith(reducedKey)))
            .findFirst()
            .map(entry -> VersionEntry.builder()
                .sha512Key(entry.getKey())
                .values(entry.getValue())
                .version(head)
                .build());

        if (!versionEntry.isPresent()) {
            versionEntry = derefernece(reducedKey);
        }

        if (versionEntry.isPresent()) {
            manifest.remove(versionEntry.get().getSha512Key());
        }

        return versionEntry.map(entry -> entry.getFirstValue());
    }

    private Optional<VersionEntry> derefernece(String reducedKey) {
        return versions.entrySet()
            .parallelStream()
            .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder())) 
            .map(version -> version.getValue()
                .find(reducedKey)
                .map(entry -> VersionEntry.builder()
                    .sha512Key(entry.getKey())
                    .values(entry.getValue())
                    .version(version.getKey())
                    .build()))
            .filter(entry -> entry.isPresent())
            .map(entry -> entry.get())
            .findFirst()
            .map(entry ->  entry.withValues(
                entry.getValues()
                    .stream()
                    .map(value -> format("%s/%s/%s", entry.getVersion(), contentDirectory, value))
                    .collect(Collectors.toList())
            ));
    }
}
