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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("human-protocol")
public class HumanProtocolPropertiesImpl
    implements HumanProtocolProperties
{
    private String secretKey;
    private String metaApiUrl;
    private String apiKey;
    private int exchangeId;
    private String s3Username;
    private String s3Password;
    private String s3Region;

    @Override
    public String getSecretKey()
    {
        return secretKey;
    }

    public void setSecretKey(String aSecretKey)
    {
        secretKey = aSecretKey;
    }

    @Override
    public String getMetaApiUrl()
    {
        return metaApiUrl;
    }

    public void setMetaApiUrl(String aMetaApiUrl)
    {
        metaApiUrl = aMetaApiUrl;
    }

    @Override
    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String aApiKey)
    {
        apiKey = aApiKey;
    }

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
}
