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

import org.springframework.http.ResponseEntity;

import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;

public interface HumanProtocolController
{
    static final String API_BASE = "/human-protocol/v1";
    static final String SUBMIT_JOB = "submitJob";
    static final String SUBMIT_JOB_MANIFEST = "submitJobManifest";

    static final String PARAM_JOB_ADDRESS = "jobAddress";
    static final String PARAM_NETWORK_ID = "networkId";

    ResponseEntity<Void> submitJob(boolean aSignatureValue, JobRequest aJobRequest)
        throws Exception;

    ResponseEntity<Void> submitJobManifest(String aJobAddress, int aNetworkId,
            boolean aSignatureValid, JobManifest aJobRequest)
        throws Exception;
}
