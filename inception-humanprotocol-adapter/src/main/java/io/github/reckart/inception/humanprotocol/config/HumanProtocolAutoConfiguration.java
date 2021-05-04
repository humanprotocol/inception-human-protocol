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

import org.springdoc.core.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import io.github.reckart.inception.humanprotocol.HumanProtocolController;
import io.github.reckart.inception.humanprotocol.HumanProtocolControllerImpl;
import io.github.reckart.inception.humanprotocol.HumanProtocolService;
import io.github.reckart.inception.humanprotocol.HumanProtocolServiceImpl;
import io.swagger.v3.oas.models.info.Info;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(HumanProtocolPropertiesImpl.class)
@AutoConfigureBefore({ WebMvcAutoConfiguration.class })
public class HumanProtocolAutoConfiguration
{
    @Bean
    public HumanProtocolController humanProtocolController(ApplicationContext aApplicationContext,
            ProjectService aProjectService, HumanProtocolService aHmtService)
    {
        return new HumanProtocolControllerImpl(aApplicationContext, aProjectService, aHmtService);
    }

    @Bean
    public GroupedOpenApi humanProtocolDocket()
    {
        return GroupedOpenApi.builder().group("human-protocol")
                .pathsToMatch(HumanProtocolController.API_BASE + "/**") //
                .addOpenApiCustomiser(openApi -> { //
                    openApi.info(new Info() //
                            .title("Human Protocol API") //
                            .version("1"));
                }).build();
    }

    @Bean
    public HumanProtocolWebInitializer humanProtocolWebInitializer(HumanProtocolProperties aProperties)
    {
        return new HumanProtocolWebInitializer(aProperties);
    }

    @Bean
    public HumanProtocolService humanProtocolService(RepositoryProperties aRepositoryProperties,
            ProjectExportService aProjectExportService, InviteService aInviteService, HumanProtocolProperties aHmtProperties,
            S3Client aS3Client)
    {
        return new HumanProtocolServiceImpl(aProjectExportService, aInviteService,
                aS3Client, aRepositoryProperties, aHmtProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public S3Client humanProtocolS3Client(HumanProtocolProperties aHmtProperties)
    {
        return S3Client.builder() //
                .region(Region.of(aHmtProperties.getS3Region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials
                        .create(aHmtProperties.getS3Username(), aHmtProperties.getS3Password())))
                .httpClient(ApacheHttpClient.builder().build())
                .build();
    }
}
