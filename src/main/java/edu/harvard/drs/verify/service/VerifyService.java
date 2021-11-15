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
import static java.lang.String.format;

import edu.harvard.drs.verify.dto.OcflInventory;
import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Verify service.
 */
@Slf4j
@Service
@RequestScope
public class VerifyService {

    private final S3Service s3Service;

    /**
     * Verify service constructor autowired.
     *
     * @param s3Service S3 service
     */
    @Autowired
    public VerifyService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Verify ingest.
     *
     * @param id    object id
     * @param input expected checksum map
     * @throws IOException failed to get inventory
     * @throws VerificationException failed verification
     */
    public void verifyIngest(Long id, Map<String, String> input) throws IOException, VerificationException {
        log.info("Veryfing ingest object {}", id);

        verify(id, input);
    }

    /**
     * Verify update.
     *
     * @param id    object id
     * @param input expected checksum map
     * @throws IOException failed to get inventory
     * @throws VerificationException failed verification
     */
    public void verifyUpdate(Long id, Map<String, String> input) throws IOException, VerificationException {
        log.info("Veryfing update object {}", id);

        verify(id, input, true);
    }

    private OcflInventory verify(Long id, Map<String, String> input) throws IOException, VerificationException {
        return verify(id, input, false);
    }

    private OcflInventory verify(Long id, Map<String, String> input, boolean update)
        throws IOException, VerificationException {
        OcflInventory inventory = s3Service.fetchInventory(id);

        Map<String, VerificationError> errors = new ConcurrentHashMap<>();

        input.entrySet()
            .parallelStream()
            .forEach(entry -> {
                String statePath = entry.getKey();

                Optional<String> manifestKey = inventory.find(statePath);
                if (manifestKey.isPresent()) {
                    String key = buildKey(id, manifestKey.get());

                    String expected = entry.getValue();

                    try {
                        String actual = s3Service.getHeadObjectEtag(key);

                        if (!expected.equals(actual)) {
                            VerificationError error = VerificationError.builder()
                                .error("Checksums do not match")
                                .expected(expected)
                                .actual(actual)
                                .build();

                            errors.put(statePath, error);
                        }

                    } catch (Exception e) {
                        log.error(format("Failed to get head obect of manifest entry %s", key), e);
                        errors.put(statePath, VerificationError.from(e.getMessage()));
                    }
                } else {
                    errors.put(statePath, VerificationError.from("Not found in inventory manifest"));
                }
            });

        if (!update) {
            inventory.getVersions()
                .get(inventory.getHead())
                .getState()
                .values()
                .stream()
                .flatMap(Collection::stream)
                .filter(statePath -> !input.containsKey(statePath))
                .forEach(statePath -> {
                    errors.put(statePath, VerificationError.from("Missing input checksum"));
                });
        }

        if (!errors.isEmpty()) {
            throw new VerificationException(errors);
        }

        return inventory;
    }

}
