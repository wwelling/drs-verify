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

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * OCFL Version entry.
 */
@Data
@Builder
public class VersionEntry {
    private String version;
    private String sha512Key;
    private List<String> values;

    public String getFirstValue() {
        return !values.isEmpty() ? values.get(0) : null;
    }

    public VersionEntry withValues(List<String> values) {
        this.values = values;
        return this;
    }
}
