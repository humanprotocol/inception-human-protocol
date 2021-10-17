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
package io.github.reckart.inception.humanprotocol.config;

import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.INFURA_PATTERN;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.UUID_PATTERN;
import static io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter.ANY_KEY;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;

import javax.validation.constraints.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("human-protocol")
@Validated
public class HumanProtocolPropertiesImpl
    implements HumanProtocolProperties
{
    private int exchangeId;

    @Pattern(regexp = UUID_PATTERN, message = "Invalid UUID")
    private String exchangeKey;

    private String jobFlowUrl;

    @Pattern(regexp = "(" + UUID_PATTERN + ")|([" + ANY_KEY + "])", message = "Invalid UUID")
    private String jobFlowKey;

    private String s3Region;
    private String s3Endpoint;
    private String s3AccessKeyId;
    private String s3SecretAccessKey;
    private String s3Bucket;
    
    @Pattern(regexp = INFURA_PATTERN, message = "Invalid Infura ID")
    private String infuraId;

    @Override
    public int getExchangeId()
    {
        return exchangeId;
    }

    public void setExchangeId(int aExchangeId)
    {
        exchangeId = aExchangeId;
    }

    @Override
    public String getExchangeKey()
    {
        return exchangeKey;
    }

    public void setExchangeKey(String aKey)
    {
        exchangeKey = aKey;
    }

    @Override
    public String getJobFlowUrl()
    {
        return jobFlowUrl;
    }

    public void setJobFlowUrl(String aUrl)
    {
        jobFlowUrl = aUrl;
    }

    @Override
    public String getJobFlowKey()
    {
        return jobFlowKey;
    }

    public void setJobFlowKey(String aKey)
    {
        jobFlowKey = aKey;
    }

    @Override
    public String getS3AccessKeyId()
    {
        return s3AccessKeyId;
    }

    public void setS3AccessKeyId(String sS3AccessKeyId)
    {
        s3AccessKeyId = sS3AccessKeyId;
    }

    @Override
    public String getS3SecretAccessKey()
    {
        return s3SecretAccessKey;
    }

    public void setS3SecretAccessKey(String aS3SecretAccessKey)
    {
        s3SecretAccessKey = aS3SecretAccessKey;
    }

    @Override
    public String getS3Bucket()
    {
        return s3Bucket;
    }

    public void setS3Bucket(String aS3Bucket)
    {
        s3Bucket = aS3Bucket;
    }

    @Override
    public String getS3Endpoint()
    {
        return s3Endpoint;
    }

    public void setS3Endpoint(String aS3Endpoint)
    {
        s3Endpoint = aS3Endpoint;
    }

    @Override
    public String getS3Region()
    {
        return s3Region;
    }

    public void setS3Region(String aS3Region)
    {
        s3Region = aS3Region;
    }

    @Override
    public boolean isS3BucketInformationAvailable()
    {
        return isNoneBlank(s3Endpoint, s3Region, s3Bucket, s3AccessKeyId, s3SecretAccessKey);
    }
    
    public void setInfuraId(String aInfuraId)
    {
        infuraId = aInfuraId;
    }
    
    @Override
    public String getInfuraId()
    {
        return infuraId;
    }
}
