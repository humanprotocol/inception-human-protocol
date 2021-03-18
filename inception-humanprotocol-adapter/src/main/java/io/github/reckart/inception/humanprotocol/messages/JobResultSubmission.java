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
package io.github.reckart.inception.humanprotocol.messages;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Outgoing message notifying the requester about the completion of a job.
 * 
 * <pre>
 * <code>
 * {
 *   network_id: int,
 *   job_address: string,
 *   job_data: string:url
 * }
 * </code>
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class JobResultSubmission
{
    private int networkId;
    private String jobAddress;
    private URI jobData;

    public int getNetworkId()
    {
        return networkId;
    }

    public void setNetworkId(int aNetworkId)
    {
        networkId = aNetworkId;
    }

    public String getJobAddress()
    {
        return jobAddress;
    }

    public void setJobAddress(String aJobAddress)
    {
        jobAddress = aJobAddress;
    }

    public URI getJobData()
    {
        return jobData;
    }

    public void setJobData(URI aJobData)
    {
        jobData = aJobData;
    }
}
