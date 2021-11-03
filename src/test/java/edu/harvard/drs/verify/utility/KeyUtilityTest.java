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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Key utility tests.
 */
public class KeyUtilityTest {

    @Test
    public void testVerifyIngest() {
        assertEquals(
            "2222/1111/11112222/inventory.json",
            KeyUtility.buildKey(11112222L, "inventory.json")
        );

        assertEquals(
            "2222/1110/1112222/inventory.json",
            KeyUtility.buildKey(1112222L, "inventory.json")
        );

        assertEquals(
            "1000/0000/1/inventory.json",
            KeyUtility.buildKey(1L, "inventory.json")
        );
    }

}
