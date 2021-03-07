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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import io.github.reckart.inception.humanprotocol.HumanProtocolController;
import io.github.reckart.inception.humanprotocol.HumanProtocolControllerImpl;
import io.swagger.v3.oas.models.info.Info;

@Lazy(false)
@Configuration
@AutoConfigureBefore({ WebMvcAutoConfiguration.class })
public class HumanProtocolAutoConfiguration
{
    @Lazy(false)
    @ConditionalOnMissingBean
    @Bean
    public HumanProtocolController humanProtocolController(ProjectService aProjectService,
            DocumentService aDocumentService,
            AnnotationSchemaService aSchemaService)
    {
        return new HumanProtocolControllerImpl(aProjectService, aDocumentService, aSchemaService);
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
}
