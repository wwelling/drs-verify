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

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import edu.harvard.drs.verify.dto.VerificationError;
import edu.harvard.drs.verify.exception.VerificationException;
import edu.harvard.drs.verify.service.VerifyService;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Verify controller.
 */
@Slf4j
@RestController
@RequestMapping("verify")
public class VerifyController {

    @Autowired
    private VerifyService verifyService;

    /**
     * Verify endpoint.
     *
     * @param id    DRS object id
     * @param input input checksum map
     * @throws IOException either not found or internal server error
     * @throws VerificationException verification failed
     */
    @PostMapping("{id}")
    public void verify(
        @PathVariable(required = true) Long id,
        @RequestBody Map<String, String> input
    ) throws IOException, VerificationException {
        verifyService.verifyIngest(id, input);
    }

    /**
     * Verify update endpoint.
     *
     * @param id    DRS object id
     * @param input input checksum map
     * @throws IOException either not found or internal server error
     * @throws VerificationException verification failed
     */
    @PostMapping("{id}/update")
    public void verifyUpdate(
        @PathVariable(required = true) Long id,
        @RequestBody Map<String, String> input
    ) throws IOException, VerificationException {
        verifyService.verifyUpdate(id, input);
    }

    @ResponseStatus(value = INTERNAL_SERVER_ERROR)
    @ExceptionHandler(IOException.class)
    public String handleInternalServiceError(IOException e) {
        log.error(e.getMessage(), e);
        return e.getMessage();
    }

    @ResponseStatus(value = CONFLICT)
    @ExceptionHandler(VerificationException.class)
    public Map<String, VerificationError> handleVerificationFailed(VerificationException e) {
        return e.getErrors();
    }

    @ResponseStatus(value = NOT_FOUND)
    @ExceptionHandler(NoSuchKeyException.class)
    public String handleNotFound(NoSuchKeyException e) {
        return e.getMessage();
    }

}
