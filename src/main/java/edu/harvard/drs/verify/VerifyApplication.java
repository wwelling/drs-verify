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

package edu.harvard.drs.verify;

import edu.harvard.drs.verify.config.AwsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

/**
 * DRS Verify Application.
 */
@EnableCaching
@SpringBootApplication
@EnableConfigurationProperties(AwsConfig.class)
public class VerifyApplication {

    /**
     * DRS Verify entry point.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(VerifyApplication.class, args);
    }

}
