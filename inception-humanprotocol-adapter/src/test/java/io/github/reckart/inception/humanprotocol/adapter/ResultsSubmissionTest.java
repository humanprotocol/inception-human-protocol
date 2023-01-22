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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.CUSTOM_SPAN_LAYER;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_EXCHANGE_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.JOB_RESULTS_ENDPOINT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.TASK_TYPE_SPAN_SELECT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.VALUE_FEATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolService.RESULTS_KEY_SUFFIX;
import static io.github.reckart.inception.humanprotocol.SignatureUtils.generateHexSignature;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileSystemUtils;

import com.adobe.testing.s3mock.junit5.S3MockExtension;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.config.AnnotationAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.diag.config.CasDoctorAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.text.config.TextFormatsAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.config.AnnotationUIAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.annotation.storage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationDocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.curation.config.CurationServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.diam.editor.config.DiamAutoConfig;
import de.tudarmstadt.ukp.inception.documents.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.log.config.EventLoggingAutoConfiguration;
import de.tudarmstadt.ukp.inception.preferences.config.PreferencesServiceAutoConfig;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.scheduling.config.SchedulingServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.search.config.SearchServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.config.DashboardAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.docanno.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.config.WorkloadManagementAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.dynamic.config.DynamicWorkloadManagerAutoConfiguration;
import io.github.reckart.inception.humanprotocol.HumanProtocolProjectInitializer;
import io.github.reckart.inception.humanprotocol.HumanProtocolServiceImpl;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolPropertiesImpl;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.messages.JobResultSubmission;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.PayoutItem;
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
@EnableAutoConfiguration(exclude = { LiquibaseAutoConfiguration.class })
@SpringBootTest( //
        webEnvironment = MOCK, //
        properties = { //
                "repository.path=" + ResultsSubmissionTest.TEST_OUTPUT_FOLDER, //
                "human-protocol.s3-endpoint=http://dummy", //
                "human-protocol.s3-region=us-west-2", //
                "human-protocol.s3-access-key-id=dummy", //
                "human-protocol.s3-secret-access-key=dummy", //
                "human-protocol.human-api-key=" + ResultsSubmissionTest.HUMAN_API_KEY, //
                "documentmetadata.enabled=true", //
                "workload.dynamic.enabled=true", //
                "sharing.invites.enabled=true", //
                "search.enabled=false" })
@EnableWebSecurity
@Import({ //
        SchedulingServiceAutoConfiguration.class, //
        SecurityAutoConfiguration.class, //
        CasStorageServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        DocumentMetadataLayerSupportAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        TextFormatsAutoConfiguration.class, //
        ProjectExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class ResultsSubmissionTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/ResultsSubmissionTest";

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent()
            .withProperty("spring.autoconfigure.exclude", join(",", //
                    SearchServiceAutoConfiguration.class.getName(), //
                    PreferencesServiceAutoConfig.class.getName(), //
                    AnnotationUIAutoConfiguration.class.getName(), //
                    CasDoctorAutoConfiguration.class.getName(), //
                    SchedulingServiceAutoConfiguration.class.getName(), //
                    CurationServiceAutoConfiguration.class.getName(), //
                    DocumentMetadataLayerSupportAutoConfiguration.class.getName(), //
                    HumanProtocolAutoConfiguration.class.getName(), //
                    ProjectExportServiceAutoConfiguration.class.getName(), //
                    DocumentServiceAutoConfiguration.class.getName(), //
                    DocumentImportExportServiceAutoConfiguration.class.getName(), //
                    SecurityAutoConfiguration.class.getName(), //
                    ProjectServiceAutoConfiguration.class.getName(), //
                    RepositoryAutoConfiguration.class.getName(), //
                    AnnotationAutoConfiguration.class.getName(), //
                    DiamAutoConfig.class.getName(), //
                    BratAnnotationEditorAutoConfiguration.class.getName(), //
                    AnnotationSchemaServiceAutoConfiguration.class.getName(), //
                    ProjectInitializersAutoConfiguration.class.getName(), //
                    CasStorageServiceAutoConfiguration.class.getName(), //
                    CurationDocumentServiceAutoConfiguration.class.getName(), //
                    LiquibaseAutoConfiguration.class.getName(), //
                    DashboardAutoConfiguration.class.getName(), //
                    WorkloadManagementAutoConfiguration.class.getName(), //
                    DynamicWorkloadManagerAutoConfiguration.class.getName(), //
                    EventLoggingAutoConfiguration.class.getName(), //
                    InviteServiceAutoConfiguration.class.getName()))
            .withSecureConnection(false).build();

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
    private @Autowired DocumentService documentService;
    private @Autowired HumanProtocolServiceImpl hmtService;
    private @Autowired HumanProtocolPropertiesImpl hmtProperties;
    private @Autowired ApplicationEventPublisher applicationEventPublisher;
    private @Autowired ProjectExportService projectExportService;
    private @Autowired ApplicationContext applicationContext;
    private @Autowired AnnotationSchemaService annotationService;

    private S3Client s3Client;
    private MockWebServer metaApiServer;

    // If this is not static, for some reason the value is re-set to false before a
    // test method is invoked. However, the DB is not reset - and it should not be.
    // So we need to make this static to ensure that we really only create the user
    // in the DB and clean the test repository once!
    private static boolean initialized = false;

    @BeforeEach
    public void setup() throws Exception
    {
        FileSystemUtils.deleteRecursively(new File(TEST_OUTPUT_FOLDER));

        MDC.put(KEY_USERNAME, "USERNAME");

        metaApiServer = new MockWebServer();
        metaApiServer.start();

        hmtProperties.setJobFlowUrl(metaApiServer.url("/api").toString());
        hmtProperties.setExchangeId(EXCHANGE_ID);
        hmtProperties.setExchangeKey(EXCHANGE_KEY);
        hmtProperties.setJobFlowKey(HUMAN_API_KEY);
        hmtProperties.setS3Bucket(BUCKET);

        // We set dummy values for the following parameters so that isS3BucketInformationAvailable()
        // returns true - but since we inject a test client, these values are not actually used
        // by the test.
        hmtProperties.setS3AccessKeyId("dummy");
        hmtProperties.setS3SecretAccessKey("dummy");
        hmtProperties.setS3Endpoint("dummy");

        assertThat(hmtProperties.isS3BucketInformationAvailable()).isTrue();

        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;
        }

        s3Client = S3_MOCK.createS3ClientV2();
        s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }

    @AfterEach
    public void teardown() throws Exception
    {
        s3Client.close();
        metaApiServer.close();
    }

    @Test
    public void thatUploadIsTriggeredOnAnnotationsComplete() throws Exception
    {
        JobManifest jobManifest = new JobManifest();
        jobManifest.setRequestType(TASK_TYPE_SPAN_SELECT);
        Project project = prepareProject(jobManifest);

        // Expect results submission message
        metaApiServer.enqueue(new MockResponse().setResponseCode(200));

        // Trigger project submissions via event
        project.setState(ANNOTATION_FINISHED);
        applicationEventPublisher
                .publishEvent(new ProjectStateChangedEvent(this, project, ANNOTATION_IN_PROGRESS));

        // Validate that the submitted results are as expected
        Project copyOfProject = fetchProjectFromBucket();
        assertThat(copyOfProject.getSlug()).isEqualTo(project.getSlug() + "-1");
        assertThat(copyOfProject.getName()).isEqualTo(project.getName());

        // Validate invite link notification
        RecordedRequest jobResultsNotificationRequest = metaApiServer.takeRequest();
        String serializedNotification = jobResultsNotificationRequest.getBody().readUtf8();
        assertThat(jobResultsNotificationRequest.getPath()) //
                .as("Invite link notification recieved") //
                .endsWith(JOB_RESULTS_ENDPOINT);
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
        assertThat(notification.getPayouts()) //
                .usingRecursiveFieldByFieldElementComparator() //
                .containsExactly( //
                        new PayoutItem("anno1", "doc1", "doc2"), //
                        new PayoutItem("anno2", "doc1", "doc2"));
    }

    @Test
    public void thatAutoMergingIsPerformedBeforeSubmission() throws Exception
    {
        JobManifest jobManifest = new JobManifest();
        jobManifest.setRequesterAccuracyTarget(0.75d);
        jobManifest.setRequestType(TASK_TYPE_SPAN_SELECT);
        Project project = prepareProject(jobManifest);

        SourceDocument doc1 = documentService.getSourceDocument(project, "doc1");
        SourceDocument doc2 = documentService.getSourceDocument(project, "doc2");

        try (var session = CasStorageSession.open()) {
            addCustomSpanAnnotation(doc1, "anno1", "X");
            addCustomSpanAnnotation(doc1, "anno2", "X");
            addCustomSpanAnnotation(doc2, "anno1", "X");
            addCustomSpanAnnotation(doc2, "anno2", "Y");
        }

        assertThat(documentService.listAnnotationDocuments(project)) //
                .extracting( //
                        AnnotationDocument::getName, //
                        AnnotationDocument::getUser, //
                        AnnotationDocument::getState)
                .containsExactly( //
                        tuple("doc1", "anno1", AnnotationDocumentState.FINISHED),
                        tuple("doc1", "anno2", AnnotationDocumentState.FINISHED),
                        tuple("doc2", "anno1", AnnotationDocumentState.FINISHED),
                        tuple("doc2", "anno2", AnnotationDocumentState.FINISHED));

        // Expect results submission message
        metaApiServer.enqueue(new MockResponse().setResponseCode(200));

        // Trigger project submissions via event
        project.setState(ANNOTATION_FINISHED);
        applicationEventPublisher
                .publishEvent(new ProjectStateChangedEvent(this, project, ANNOTATION_IN_PROGRESS));

        // Validate that the submitted results are as expected
        Project copyOfProject = fetchProjectFromBucket();

        assertThat(annotationService.findLayer(copyOfProject, CUSTOM_SPAN_LAYER)).isNotNull();

        SourceDocument copyDoc1 = documentService.getSourceDocument(copyOfProject, "doc1");
        SourceDocument copyDoc2 = documentService.getSourceDocument(copyOfProject, "doc2");

        assertThat(documentService.listSourceDocuments(copyOfProject))
                .extracting(SourceDocument::getName, SourceDocument::getState) //
                .containsExactly( //
                        tuple("doc1", SourceDocumentState.CURATION_FINISHED), //
                        tuple("doc2", SourceDocumentState.CURATION_FINISHED));

        try (var session = CasStorageSession.open()) {
            CAS curatedCas1 = documentService.readAnnotationCas(copyDoc1, CURATION_USER);
            Type spanType1 = curatedCas1.getTypeSystem().getType(CUSTOM_SPAN_LAYER);
            assertThat(spanType1).isNotNull();
            assertThat(curatedCas1.<Annotation> select(spanType1).asList()) //
                    .extracting( //
                            Annotation::getCoveredText, //
                            a -> getFeature(a, VALUE_FEATURE, String.class))
                    .containsExactly( //
                            tuple("Test.", "X"));

            CAS curatedCas2 = documentService.readAnnotationCas(copyDoc2, CURATION_USER);
            Type spanType2 = curatedCas2.getTypeSystem().getType(CUSTOM_SPAN_LAYER);
            assertThat(spanType2).isNotNull();
            assertThat(curatedCas2.<Annotation> select(spanType2).asList()) //
                    .extracting( //
                            Annotation::getCoveredText, //
                            a -> getFeature(a, VALUE_FEATURE, String.class))
                    .isEmpty();
        }
    }

    private void addCustomSpanAnnotation(SourceDocument aDoc, String aUser, String aLabel)
        throws IOException, CASException
    {
        CAS cas = documentService.readAnnotationCas(aDoc, aUser);
        Type spanType = cas.getTypeSystem().getType(CUSTOM_SPAN_LAYER);
        AnnotationFS annotation = cas.createAnnotation(spanType, 0, cas.getDocumentText().length());
        FSUtil.setFeature(annotation, "value", aLabel);
        cas.addFsToIndexes(annotation);
        documentService.writeAnnotationCas(cas, aDoc, aUser, false);
    }

    private Project prepareProject(JobManifest aJobManifest) throws Exception
    {
        Project project = new Project("test");
        projectService.createProject(project);

        JobRequest jobRequest = new JobRequest();
        jobRequest.setJobAddress(JOB_ADDRESS);
        jobRequest.setNetworkId(NETWORK_ID);
        hmtService.writeJobRequest(project, jobRequest);

        hmtService.writeJobManifest(project, aJobManifest);

        HumanProtocolProjectInitializer initializer = new HumanProtocolProjectInitializer(
                aJobManifest);
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(initializer);
        factory.initializeBean(initializer, "transientInitializer");
        projectService.initializeProject(project, asList(initializer));

        User anno1 = createAnnotatorUser(project, "anno1");
        User anno2 = createAnnotatorUser(project, "anno2");

        SourceDocument doc1 = createSourceDocument(project, "doc1", "Test.");
        createAnnotationDocument(doc1, anno1, FINISHED);
        createAnnotationDocument(doc1, anno2, FINISHED);

        SourceDocument doc2 = createSourceDocument(project, "doc2", "Test.");
        createAnnotationDocument(doc2, anno1, FINISHED);
        createAnnotationDocument(doc2, anno2, FINISHED);

        return project;
    }

    private User createAnnotatorUser(Project aProject, String aUsername)
    {
        User anno = userRepository.create(new User(aUsername, ROLE_USER));
        projectService.assignRole(aProject, anno, ANNOTATOR);
        return anno;
    }

    private SourceDocument createSourceDocument(Project aProject, String aDocumentName,
            String aDocumentText)
        throws IOException, UIMAException
    {
        Supplier<InputStream> testDocumentStream = () -> new ByteArrayInputStream(
                aDocumentText.getBytes(UTF_8));
        SourceDocument doc = documentService.createSourceDocument(
                new SourceDocument(aDocumentName, aProject, TextFormatSupport.ID));
        documentService.uploadSourceDocument(testDocumentStream.get(), doc);
        return doc;
    }

    private AnnotationDocument createAnnotationDocument(SourceDocument aDocument, User aAnnotator,
            AnnotationDocumentState aState)
    {
        AnnotationDocument annDoc = new AnnotationDocument(aAnnotator.getUsername(), aDocument);
        annDoc.setState(aState);
        documentService.createAnnotationDocument(annDoc);
        return annDoc;
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