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

package edu.harvard.drs.verify.utility;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.leftPad;
import static org.apache.commons.lang3.StringUtils.reverse;

/**
 * Key utility.
 */
public final class KeyUtility {

    /**
     * Private key utility constructor.
     */
    private KeyUtility() { }

    /**
     * Return key with reverse URN NSS path appended to key.
     *
     * @param id   nss id
     * @param path remaining path
     * @return full s3 key
     */
    public static String buildKey(Long id, String path) {
        String reversedNss = reverse(leftPad(valueOf(id), 8, "0"));

        return format(
            "%s/%s/%s/%s",
            reversedNss.substring(0, 4),
            reversedNss.substring(4, 8),
            id,
            path
        );
    }

    /**
     * Reduce key to everything after content directory.
     *
     * @param contentDirectory content directory
     * @param key              key
     * @return reduced key
     */
    public static String reduceKey(String contentDirectory, String key) {
        return key.substring(key.indexOf(contentDirectory) + contentDirectory.length() + 1);
    }

}
