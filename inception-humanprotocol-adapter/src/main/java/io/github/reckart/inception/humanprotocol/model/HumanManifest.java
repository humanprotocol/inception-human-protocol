package io.github.reckart.inception.humanprotocol.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class HumanManifest
{
    private String jobId;
    private HumanInternationalizedStrings requesterQuestion;
    private String requesterDescription;    
    private int requesterMinRepeats;
    private int requesterMaxRepeats;
    private double requesterAccuracyTarget;
    private Map<String, HumanInternationalizedStrings> requesterRestrictedAnswerSet;
     
    private long expirationDate;
    
    private String taskBidPrice;
    
    private String taskdataUri;

    public void setJobId(String aJobId)
    {
        jobId = aJobId;
    }
    
    public String getJobId()
    {
        return jobId;
    }
    
    public HumanInternationalizedStrings getRequesterQuestion()
    {
        return requesterQuestion;
    }

    public void setRequesterQuestion(HumanInternationalizedStrings aRequesterQuestion)
    {
        requesterQuestion = aRequesterQuestion;
    }

    public String getRequesterDescription()
    {
        return requesterDescription;
    }

    public void setRequesterDescription(String aRequesterDescription)
    {
        requesterDescription = aRequesterDescription;
    }

    public int getRequesterMinRepeats()
    {
        return requesterMinRepeats;
    }

    public void setRequesterMinRepeats(int aRequesterMinRepeats)
    {
        requesterMinRepeats = aRequesterMinRepeats;
    }

    public int getRequesterMaxRepeats()
    {
        return requesterMaxRepeats;
    }

    public void setRequesterMaxRepeats(int aRequesterMaxRepeats)
    {
        requesterMaxRepeats = aRequesterMaxRepeats;
    }

    public double getRequesterAccuracyTarget()
    {
        return requesterAccuracyTarget;
    }

    public void setRequesterAccuracyTarget(double aRequesterAccuracyTarget)
    {
        requesterAccuracyTarget = aRequesterAccuracyTarget;
    }

    public Map<String, HumanInternationalizedStrings> getRequesterRestrictedAnswerSet()
    {
        return requesterRestrictedAnswerSet;
    }

    public void setRequesterRestrictedAnswerSet(
            Map<String, HumanInternationalizedStrings> aRequesterRestrictedAnswerSet)
    {
        requesterRestrictedAnswerSet = aRequesterRestrictedAnswerSet;
    }

    public long getExpirationDate()
    {
        return expirationDate;
    }

    public void setExpirationDate(long aExpirationDate)
    {
        expirationDate = aExpirationDate;
    }

    public String getTaskBidPrice()
    {
        return taskBidPrice;
    }

    public void setTaskBidPrice(String aTaskBidPrice)
    {
        taskBidPrice = aTaskBidPrice;
    }

    public String getTaskdataUri()
    {
        return taskdataUri;
    }

    public void setTaskdataUri(String aTaskdataUri)
    {
        taskdataUri = aTaskdataUri;
    }
}
