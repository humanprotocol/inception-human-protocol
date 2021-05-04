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

import static io.github.reckart.inception.humanprotocol.HumanProtocolController.API_BASE;
import static io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter.PARAM_SECRET_KEY;
import static javax.servlet.DispatcherType.REQUEST;

import java.util.EnumSet;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.web.servlet.ServletContextInitializer;

import io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link HumanProtocolAutoConfiguration#humanProtocolWebInitializer}.
 * </p>
 */
public class HumanProtocolWebInitializer
    implements ServletContextInitializer
{
    private final HumanProtocolProperties properties;

    
    public HumanProtocolWebInitializer(HumanProtocolProperties aProperties)
    {
        properties = aProperties;
    }

    @Override
    public void onStartup(ServletContext aServletContext) throws ServletException
    {
        FilterRegistration humanSignatureValidationFilter = aServletContext
                .addFilter("humanSignatureValidation", HumanSignatureValidationFilter.class);
        humanSignatureValidationFilter.addMappingForUrlPatterns(EnumSet.of(REQUEST), false,
                API_BASE + "/*");
        humanSignatureValidationFilter.setInitParameter(PARAM_SECRET_KEY, properties.getSecretKey());
    }
}