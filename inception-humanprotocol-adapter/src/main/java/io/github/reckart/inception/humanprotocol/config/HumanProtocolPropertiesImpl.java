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

import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.UUID_PATTERN;

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
    
    private String humanApiUrl;
    
    @Pattern(regexp = UUID_PATTERN, message = "Invalid UUID")
    private String humanApiKey;
    
    private String s3Username;
    private String s3Password;
    private String s3Region;
    private String s3Bucket;

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
    public String getHumanApiUrl()
    {
        return humanApiUrl;
    }

    public void setHumanApiUrl(String aUrl)
    {
        humanApiUrl = aUrl;
    }

    @Override
    public String getHumanApiKey()
    {
        return humanApiKey;
    }

    public void setHumanApiKey(String aKey)
    {
        humanApiKey = aKey;
    }

    @Override
    public String getS3Username()
    {
        return s3Username;
    }

    public void setS3Username(String aS3Username)
    {
        s3Username = aS3Username;
    }

    @Override
    public String getS3Password()
    {
        return s3Password;
    }

    public void setS3Password(String aS3Password)
    {
        s3Password = aS3Password;
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
    public String getS3Bucket()
    {
        return s3Bucket;
    }

    public void setS3Bucket(String aS3Bucket)
    {
        s3Bucket = aS3Bucket;
    }
}
