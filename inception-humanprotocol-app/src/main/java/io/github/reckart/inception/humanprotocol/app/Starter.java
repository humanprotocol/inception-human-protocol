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
package io.github.reckart.inception.humanprotocol.app;

import static de.tudarmstadt.ukp.clarin.webanno.support.SettingsUtil.getApplicationHome;

import java.io.File;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;

import de.tudarmstadt.ukp.inception.INCEpTION;

@SpringBootApplication
@AutoConfigurationPackage(basePackages = { "io.github.reckart.inception.humanprotocol" })
@EntityScan(basePackages = { "io.github.reckart.inception.humanprotocol" })
@ComponentScan(basePackages = { "io.github.reckart.inception.humanprotocol" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = ".*AutoConfiguration"),
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
        @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public class Starter
    extends INCEpTION
{
    @Bean
    public WicketApplicationInitConfiguration branding()
    {
        return new WicketApplicationInitConfiguration()
        {
            @Override
            public void init(WebApplication aWebApplication)
            {
                File brandingCss = new File(getApplicationHome(), "branding.css");

                if (!brandingCss.exists()) {
                    return;
                }

                aWebApplication.getComponentInstantiationListeners().add(component -> {
                    if (component instanceof Page) {
                        component.add(new Behavior()
                        {
                            private static final long serialVersionUID = 5519463574787275765L;

                            @Override
                            public void renderHead(Component aComponent, IHeaderResponse aResponse)
                            {
                                aResponse.render(
                                        CssHeaderItem.forReference(new FileSystemResourceReference(
                                                "branding.css", brandingCss.toPath())));
                            }
                        });
                    }
                });
            }
        };
    }

    public static void main(String[] args) throws Exception
    {
        run(args, Starter.class);
    }
}
