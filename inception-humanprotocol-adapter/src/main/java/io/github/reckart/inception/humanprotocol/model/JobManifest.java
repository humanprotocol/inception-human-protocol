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
package io.github.reckart.inception.humanprotocol.model;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalDouble;

import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_DEFAULT)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class JobManifest
{
    private String jobId;

    private InternationalizedStrings requesterQuestion;
    private String requesterDescription;
    private int requesterMinRepeats = 1;
    private int requesterMaxRepeats;
    private Double requesterAccuracyTarget;
    private Map<String, InternationalizedStrings> requesterRestrictedAnswerSet;

    private long expirationDate;

    private String requestType;
    private Map<String, Object> requestConfig;

    private String taskdataUri;
    private TaskData taskdata;

    public void setJobId(String aJobId)
    {
        jobId = aJobId;
    }

    public String getJobId()
    {
        return jobId;
    }

    public InternationalizedStrings getRequesterQuestion()
    {
        return requesterQuestion;
    }

    public void setRequesterQuestion(InternationalizedStrings aRequesterQuestion)
    {
        requesterQuestion = aRequesterQuestion;
    }

    public String getRequesterDescription()
    {
        return requesterDescription;
    }

    /**
     * @param aRequesterDescription
     *            arbitrary metadata supplied by requester for job description convenience
     *            (optional)
     */
    public void setRequesterDescription(String aRequesterDescription)
    {
        requesterDescription = aRequesterDescription;
    }

    public int getRequesterMinRepeats()
    {
        return requesterMinRepeats;
    }

    /**
     * @param aRequesterMinRepeats
     *            min # of answers to collect per task (optional)
     */
    public void setRequesterMinRepeats(int aRequesterMinRepeats)
    {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, aRequesterMinRepeats);
        
        requesterMinRepeats = aRequesterMinRepeats;
    }

    public int getRequesterMaxRepeats()
    {
        return requesterMaxRepeats;
    }

    /**
     * @param aRequesterMaxRepeats
     *            max # of answers to collect per task (optional)
     */
    public void setRequesterMaxRepeats(int aRequesterMaxRepeats)
    {
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, aRequesterMaxRepeats);
        
        requesterMaxRepeats = aRequesterMaxRepeats;
    }

    public Double getRequesterAccuracyTarget()
    {
        return requesterAccuracyTarget;
    }
    
    public OptionalDouble requesterAccuracyTarget()
    {
        if (requesterAccuracyTarget == null) {
            return OptionalDouble.empty();
        }
        
        return OptionalDouble.of(requesterAccuracyTarget);
    }

    /**
     * @param aRequesterAccuracyTarget
     *            0-1 - stop asking when min repeats is met and task accuracy exceeds this target.
     *            (optional)
     */
    public void setRequesterAccuracyTarget(Double aRequesterAccuracyTarget)
    {
        if (aRequesterAccuracyTarget != null) {
            Validate.inclusiveBetween(0.0d, 1.0d, (double) aRequesterAccuracyTarget);
        }
        
        requesterAccuracyTarget = aRequesterAccuracyTarget;
    }

    public Map<String, InternationalizedStrings> getRequesterRestrictedAnswerSet()
    {
        return requesterRestrictedAnswerSet;
    }

    public void setRequesterRestrictedAnswerSet(
            Map<String, InternationalizedStrings> aRequesterRestrictedAnswerSet)
    {
        requesterRestrictedAnswerSet = aRequesterRestrictedAnswerSet;
    }

    public long getExpirationDate()
    {
        return expirationDate;
    }

    /**
     * @param aExpirationDate expiration date, expressed in seconds (UTC epoch time)
     */
    public void setExpirationDate(long aExpirationDate)
    {
        Validate.inclusiveBetween(0, Long.MAX_VALUE, aExpirationDate);
        
        expirationDate = aExpirationDate;
    }

    public String getTaskdataUri()
    {
        return taskdataUri;
    }

    public void setTaskdataUri(String aTaskdataUri)
    {
        taskdataUri = aTaskdataUri;
    }

    public void setTaskdata(TaskData aTaskdata)
    {
        taskdata = aTaskdata;
    }

    public TaskData getTaskdata()
    {
        return taskdata;
    }

    public String getRequestType()
    {
        return requestType;
    }

    public void setRequestType(String aRequestType)
    {
        requestType = aRequestType;
    }

    public Map<String, Object> getRequestConfig()
    {
        if (requestConfig == null) {
            return Collections.emptyMap();
        }
        return requestConfig;
    }

    public void setRequestConfig(Map<String, Object> aRequestConfig)
    {
        requestConfig = aRequestConfig;
    }
}
