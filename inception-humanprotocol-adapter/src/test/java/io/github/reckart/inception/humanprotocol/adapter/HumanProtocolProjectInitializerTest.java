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
package io.github.reckart.inception.humanprotocol.adapter;

import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.ANCHORING_TOKENS;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.OVERLAP_NONE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_ANCHORING;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_CROSS_SENENCE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_OVERLAP;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_VERSION;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.TASK_TYPE_SPAN_SELECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;
import io.github.reckart.inception.humanprotocol.HumanProtocolConstants;
import io.github.reckart.inception.humanprotocol.HumanProtocolProjectInitializer;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.model.InternationalizedStrings;
import io.github.reckart.inception.humanprotocol.model.JobManifest;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = { //
        LiquibaseAutoConfiguration.class, //
        HumanProtocolAutoConfiguration.class })
@SpringBootTest( //
        webEnvironment = WebEnvironment.NONE, //
        properties = { //
                "repository.path=" + ResultsSubmissionTest.TEST_OUTPUT_FOLDER, //
                "documentmetadata.enabled=true", //
                "workload.dynamic.enabled=true", //
                "sharing.invites.enabled=true" })
@Import({ //
        CasStorageServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        ProjectExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class HumanProtocolProjectInitializerTest
{
    private @Autowired ProjectService projectService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired ApplicationContext applicationContext;

    @Test
    public void thatSpanSelectionTaskInitializationWorks() throws Exception
    {
        Project project = new Project("test-span-selection");
        projectService.createProject(project);

        JobManifest manifest = new JobManifest();
        manifest.setRequesterQuestion(new InternationalizedStrings() //
                .withString("en", "Identify the rabbit."));
        manifest.setRequestType(TASK_TYPE_SPAN_SELECT);
        manifest.setRequestConfig(Map.of( //
                REQUEST_CONFIG_KEY_VERSION, 0, //
                REQUEST_CONFIG_KEY_ANCHORING, ANCHORING_TOKENS, //
                REQUEST_CONFIG_KEY_OVERLAP, OVERLAP_NONE, //
                REQUEST_CONFIG_KEY_CROSS_SENENCE, false));

        initializeProject(project, manifest);

        assertThat(schemaService.listAnnotationLayer(project))
                .extracting(AnnotationLayer::getName, AnnotationLayer::getType)
                .containsExactly(tuple("custom.Span", WebAnnoConst.SPAN_TYPE));
        assertThat(schemaService.listAnnotationFeature(project)).hasSize(1);
    }

    @Test
    public void thatDocumentTaggingTaskInitializationWorks() throws Exception
    {
        Project project = new Project("test-document-tagging");
        projectService.createProject(project);

        JobManifest manifest = new JobManifest();
        manifest.setRequesterQuestion(new InternationalizedStrings() //
                .withString("en", "Identify the rabbit."));
        manifest.setRequestType(HumanProtocolConstants.TASK_TYPE_DOCUMENT_CLASSIFICATION);
        manifest.setRequestConfig(Map.of( //
                REQUEST_CONFIG_KEY_VERSION, 0));

        initializeProject(project, manifest);

        assertThat(schemaService.listAnnotationLayer(project))
                .extracting(AnnotationLayer::getName, AnnotationLayer::getType)
                .containsExactly(tuple("custom.DocumentTag", DocumentMetadataLayerSupport.TYPE));
        assertThat(schemaService.listAnnotationFeature(project)).hasSize(1);
    }

    private void initializeProject(Project aProject, JobManifest aManifest) throws IOException
    {
        HumanProtocolProjectInitializer sut = new HumanProtocolProjectInitializer(aManifest);
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(sut);
        factory.initializeBean(sut, "transientInitializer");
        sut.configure(aProject);
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config
    }
}
