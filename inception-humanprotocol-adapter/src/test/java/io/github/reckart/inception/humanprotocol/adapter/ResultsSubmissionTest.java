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

import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static io.github.reckart.inception.humanprotocol.HumanProtocolService.EXPORT_KEY;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import javax.persistence.EntityManager;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.adobe.testing.s3mock.junit5.S3MockExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.ChainLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistryImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.RelationLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.SpanLayerSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.AnnotationSchemaServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentImportExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.DocumentServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServicePropertiesImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.ProjectExportServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.ProjectServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDaoImpl;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.ApplicationContextProvider;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.config.DashboardAutoConfiguration;
import io.github.reckart.inception.humanprotocol.HumanProtocolServiceImpl;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Tests and demonstrates the usage of the {@link S3MockExtension} for the SDK v2.
 */
@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@SpringBootTest(webEnvironment = MOCK, properties = { //
        "workload.dynamic.enabled=true", //
        "sharing.invites.enabled=true"})
@EnableWebSecurity
@EntityScan({ "de.tudarmstadt.ukp.clarin.webanno.model", "de.tudarmstadt.ukp.inception",
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class ResultsSubmissionTest
{
    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent()
            .withProperty("spring.autoconfigure.exclude",
                    join(",", HumanProtocolAutoConfiguration.class.getName(),
                            CasStorageServiceAutoConfiguration.class.getName(),
                            LiquibaseAutoConfiguration.class.getName(),
                            DashboardAutoConfiguration.class.getName(),
                            InviteServiceAutoConfiguration.class.getName()))
            .withSecureConnection(false).build();
    private final S3Client s3Client = S3_MOCK.createS3ClientV2();

    private final String BUCKET_NAME = "my-project-address";
    
    static @TempDir File repositoryDir;
    static @TempDir File workDir;
    
    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired HumanProtocolServiceImpl hmtService;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired ProjectExportService projectExportService;
    
    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @BeforeEach
    public void setup() throws Exception
    {
        repositoryProperties.setPath(repositoryDir);
        MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
        MDC.put(KEY_USERNAME, "USERNAME");

        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;
        }
    }
    
    @Test
    void thatUploadIsTriggeredOnAnnotationsComplete() throws Exception
    {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        
        Project project = prepareProject();

        // Trigger project submissions via event
        applicationEventPublisher
                .publishEvent(new ProjectStateChangedEvent(this, project, ANNOTATION_IN_PROGRESS));

        Project copyOfProject = fetchProjectFromBucket();
        
        assertThat(copyOfProject.getName()).isEqualTo("copy_of_" + project.getName());
    }
    
    private Project prepareProject() throws IOException {
        Project project = new Project("test");
        project.setState(ANNOTATION_FINISHED);
        projectService.createProject(project);
        
        JobManifest jobManifest = new JobManifest();
        hmtService.writeJobManifest(project, jobManifest);

        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobAddress(BUCKET_NAME);
        hmtService.writeJobRequest(project, jobRequest);
        return project;
    }
    
    private Project fetchProjectFromBucket() throws IOException, ProjectExportException {
        File projectExportFile = new File(workDir, "export.zip");
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(BUCKET_NAME).key(EXPORT_KEY).build())) {
            try (OutputStream os = new FileOutputStream(projectExportFile)) {
                IOUtils.copyLarge(response, os);
            }
        }
        
        ProjectImportRequest importRequest = new ProjectImportRequest(false);
        Project copyOfProject = projectExportService.importProject(importRequest,
                new ZipFile(projectExportFile));
        
        return copyOfProject;
    }
    
    @Configuration
    public static class TestContext
    {
        private @Autowired ApplicationEventPublisher applicationEventPublisher;
        private @Autowired EntityManager entityManager;

        @Bean
        public S3Client humanProtocolS3Client() {
            return S3_MOCK.createS3ClientV2();
        }
        
        @Bean
        public ProjectService projectService(UserDao aUserRepository,
                RepositoryProperties aRepositoryProperties,
                @Lazy @Autowired(required = false) List<ProjectInitializer> aInitializerProxy)
        {
            return new ProjectServiceImpl(aUserRepository, applicationEventPublisher,
                    aRepositoryProperties, aInitializerProxy);
        }

        @Bean
        public UserDao userRepository()
        {
            return new UserDaoImpl();
        }

        @Bean
        public TokenLayerInitializer tokenLayerInitializer(
                AnnotationSchemaService aAnnotationSchemaService)
        {
            return new TokenLayerInitializer(aAnnotationSchemaService);
        }

        @Bean
        public DocumentService documentService(RepositoryProperties aRepositoryProperties,
                CasStorageService aCasStorageService,
                DocumentImportExportService aImportExportService, ProjectService aProjectService)
        {
            return new DocumentServiceImpl(aRepositoryProperties, aCasStorageService,
                    aImportExportService, aProjectService, applicationEventPublisher,
                    entityManager);
        }

        @Bean
        public AnnotationSchemaService annotationService(LayerSupportRegistry aLayerSupportRegistry)
        {
            return new AnnotationSchemaServiceImpl(aLayerSupportRegistry, featureSupportRegistry(),
                    entityManager);
        }

        @Bean
        public FeatureSupportRegistry featureSupportRegistry()
        {
            return new FeatureSupportRegistryImpl(Collections.emptyList());
        }

        @Bean
        public DocumentImportExportService importExportService(
                RepositoryProperties aRepositoryProperties, CasStorageService aCasStorageService,
                AnnotationSchemaService aAnnotationSchemaService)
        {
            return new DocumentImportExportServiceImpl(aRepositoryProperties,
                    asList(new TextFormatSupport()), aCasStorageService, aAnnotationSchemaService,
                    new DocumentImportExportServicePropertiesImpl());
        }

        @Bean
        public ProjectExportService exportService(ProjectService aProjectService)
        {
            return new ProjectExportServiceImpl(null, null, aProjectService);
        }

        @Bean
        public RepositoryProperties repositoryProperties()
        {
            return new RepositoryProperties();
        }

        @Bean
        public ApplicationContextProvider contextProvider()
        {
            return new ApplicationContextProvider();
        }

        @Bean
        public LayerSupportRegistry layerSupportRegistry(
                FeatureSupportRegistry aFeatureSupportRegistry)
        {
            return new LayerSupportRegistryImpl(
                    asList(new SpanLayerSupport(aFeatureSupportRegistry, null, null),
                            new RelationLayerSupport(aFeatureSupportRegistry, null, null),
                            new ChainLayerSupport(aFeatureSupportRegistry, null, null)));
        }
    }
}