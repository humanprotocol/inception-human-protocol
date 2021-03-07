/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package io.github.reckart.inception.humanprotocol.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import io.github.reckart.inception.humanprotocol.JobManifestUtils;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.TaskData;

public class JobManifestUtilsTest
{
    @Test
    public void thatManifestCanBeLoaded() throws Exception {
        JobManifest sut;
        try (InputStream is = Files.newInputStream(Path.of("src/test/resources/manifest/example.json"))) {
            sut = JobManifestUtils.loadManifest(is);
        }
        
        assertThat(sut.getRequesterQuestion().get("en")).contains("box around all");
        assertThat(sut.getRequesterRestrictedAnswerSet().get("human").get("en")).isEqualTo("human");
    }

    @Test
    public void thatTaskDataCanBeLoaded() throws Exception {
        TaskData sut;
        try (InputStream is = Files.newInputStream(Path.of("src/test/resources/manifest/dataset.json"))) {
            sut = JobManifestUtils.loadTaskData(is);
        }
        
        assertThat(sut).hasSize(2);
    }
}
