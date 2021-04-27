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
package io.github.reckart.inception.humanprotocol;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;

public interface HumanProtocolService
{
    Optional<JobManifest> readJobManifest(Project aProject) throws IOException;

    /**
     * Import a job manifest directly into the project from a remote URL. This ensures that any
     * information that is not represented in the {@link JobManifest} class is still present in the
     * persisted manifest and can (if need be) accessed at a later time, e.g. when the
     * {@link JobManifest} has been extended to support the new information.
     */
    void importJobManifest(Project aProject, URL aManifestUrl) throws IOException;

    Optional<JobRequest> readJobRequest(Project aProject) throws IOException;

    void writeJobRequest(Project aProject, JobRequest aJobRequest) throws IOException;

    void publishInviteLink(Project aProject) throws IOException;
}
