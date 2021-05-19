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
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_EXCHANGE_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.JOB_RESULTS_ENDPOINT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolService.RESULTS_KEY_SUFFIX;
import static io.github.reckart.inception.humanprotocol.SignatureUtils.generateHexSignature;
import static java.lang.String.format;
import static java.lang.String.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileSystemUtils;

import com.adobe.testing.s3mock.junit5.S3MockExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.docimexport.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.curation.storage.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.config.DashboardAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import io.github.reckart.inception.humanprotocol.HumanProtocolServiceImpl;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolPropertiesImpl;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.messages.JobResultSubmission;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
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
        "repository.path=" + ResultsSubmissionTest.TEST_OUTPUT_FOLDER, //
        "human-protocol.s3-endpoint=http://dummy", //
        "human-protocol.s3-region=us-west-2", //
        "human-protocol.s3-access-key-id=dummy", //
        "human-protocol.s3-secret-access-key=dummy", //
        "human-protocol.human-api-key=" + ResultsSubmissionTest.HUMAN_API_KEY, //
        "workload.dynamic.enabled=true", //
        "sharing.invites.enabled=true" })
@EnableWebSecurity
@Import({ //
        ProjectExportServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
public class ResultsSubmissionTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/ResultsSubmissionTest";

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent()
            .withProperty("spring.autoconfigure.exclude", join(",", //
                    HumanProtocolAutoConfiguration.class.getName(), //
                    ProjectExportServiceAutoConfiguration.class.getName(), //
                    DocumentServiceAutoConfiguration.class.getName(), //
                    DocumentImportExportServiceAutoConfiguration.class.getName(), //
                    SecurityAutoConfiguration.class.getName(), //
                    ProjectServiceAutoConfiguration.class.getName(), //
                    RepositoryAutoConfiguration.class.getName(), //
                    AnnotationSchemaServiceAutoConfiguration.class.getName(), //
                    ProjectInitializersAutoConfiguration.class.getName(), //
                    CasStorageServiceAutoConfiguration.class.getName(), //
                    CurationDocumentServiceAutoConfiguration.class.getName(), //
                    LiquibaseAutoConfiguration.class.getName(), //
                    DashboardAutoConfiguration.class.getName(), //
                    DynamicWorkloadManagerAutoConfiguration.class.getName(), //
                    InviteServiceAutoConfiguration.class.getName()))
            .withSecureConnection(false).build();
    private final S3Client s3Client = S3_MOCK.createS3ClientV2();

    private static final String EXCHANGE_KEY = "de85eb7e-aea9-11eb-8529-0242ac130003";
    static final String HUMAN_API_KEY = "e984c52c-aea9-11eb-8529-0242ac130003";
    private static final int EXCHANGE_ID = 4242;
    private static final int NETWORK_ID = 54342;
    private static final String JOB_ADDRESS = "eadd-dead-beef-feed";
    private static final String BUCKET = "my-bucket";

    static @TempDir File repositoryDir;
    static @TempDir File workDir;

    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired HumanProtocolServiceImpl hmtService;
    private @Autowired HumanProtocolPropertiesImpl hmtProperties;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired ProjectExportService projectExportService;

    private MockWebServer metaApiServer;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @BeforeAll
    public static void setupClass()
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));
    }

    @BeforeEach
    public void setup() throws Exception
    {
        MDC.put(KEY_USERNAME, "USERNAME");

        metaApiServer = new MockWebServer();
        metaApiServer.start();

        hmtProperties.setHumanApiUrl(metaApiServer.url("/api").toString());
        hmtProperties.setExchangeId(EXCHANGE_ID);
        hmtProperties.setExchangeKey(EXCHANGE_KEY);
        hmtProperties.setHumanApiKey(HUMAN_API_KEY);
        hmtProperties.setS3Bucket(BUCKET);

        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;
        }
    }

    @Test
    void thatUploadIsTriggeredOnAnnotationsComplete() throws Exception
    {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());

        // Expect results submission message
        metaApiServer.enqueue(new MockResponse().setResponseCode(200));

        Project project = prepareProject();

        // Trigger project submissions via event
        applicationEventPublisher
                .publishEvent(new ProjectStateChangedEvent(this, project, ANNOTATION_IN_PROGRESS));

        // Validate that the submitted results are as expected
        Project copyOfProject = fetchProjectFromBucket();
        assertThat(copyOfProject.getName()).isEqualTo("copy_of_" + project.getName());

        // Validate invite link notification
        RecordedRequest jobResultsNotificationRequest = metaApiServer.takeRequest();
        assertThat(jobResultsNotificationRequest.getPath()) //
                .as("Invite link notification recieved") //
                .endsWith(JOB_RESULTS_ENDPOINT);
        String serializedNotification = jobResultsNotificationRequest.getBody().readUtf8();
        assertThat(jobResultsNotificationRequest.getHeader(HEADER_X_EXCHANGE_SIGNATURE))
                .isEqualTo(generateHexSignature(EXCHANGE_KEY, serializedNotification));
        assertThat(jobResultsNotificationRequest.getHeader(HttpHeaders.CONTENT_TYPE))
                .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
        JobResultSubmission notification = fromJsonString(JobResultSubmission.class,
                serializedNotification);
        assertThat(notification.getJobAddress()).isEqualTo(JOB_ADDRESS);
        assertThat(notification.getNetworkId()).isEqualTo(NETWORK_ID);
        assertThat(notification.getExchangeId()).isEqualTo(hmtProperties.getExchangeId());
        assertThat(notification.getJobData().toString())
                .endsWith(format("/%s/%s/results.zip", hmtProperties.getS3Bucket(), JOB_ADDRESS));
    }

    private Project prepareProject() throws IOException
    {
        Project project = new Project("test");
        project.setState(ANNOTATION_FINISHED);
        projectService.createProject(project);

        JobManifest jobManifest = new JobManifest();
        hmtService.writeJobManifest(project, jobManifest);

        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobAddress(JOB_ADDRESS);
        jobRequest.setNetworkId(NETWORK_ID);
        hmtService.writeJobRequest(project, jobRequest);
        return project;
    }

    private Project fetchProjectFromBucket() throws IOException, ProjectExportException
    {
        File projectExportFile = new File(workDir, "export.zip");
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(GetObjectRequest
                .builder().bucket(BUCKET).key(JOB_ADDRESS + "/" + RESULTS_KEY_SUFFIX).build())) {
            try (OutputStream os = new FileOutputStream(projectExportFile)) {
                IOUtils.copyLarge(response, os);
            }
        }

        ProjectImportRequest importRequest = new ProjectImportRequest(false);
        Project copyOfProject = projectExportService.importProject(importRequest,
                new ZipFile(projectExportFile));

        return copyOfProject;
    }

    @SpringBootConfiguration
    public static class TestContext
    {
        @Bean
        public S3Client humanProtocolS3Client()
        {
            return S3_MOCK.createS3ClientV2();
        }
    }
}