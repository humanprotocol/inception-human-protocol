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

import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toPrettyJsonString;
import static de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging.KEY_USERNAME;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.ANCHORING_TOKENS;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_EXCHANGE_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_HUMAN_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.INVITE_LINK_ENDPOINT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.OVERLAP_NONE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_ANCHORING;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_CROSS_SENENCE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_OVERLAP;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_PROJECT_TITLE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_VERSION;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.TASK_TYPE_SPAN_SELECT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolController.API_BASE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolController.SUBMIT_JOB;
import static io.github.reckart.inception.humanprotocol.SignatureUtils.generateHexSignature;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.context.WebApplicationContext;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.annotationservice.config.AnnotationSchemaServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.OpenCasStorageSessionForRequestFilter;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config.CasStorageServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.documentservice.config.DocumentServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.config.ProjectServiceAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.config.SecurityAutoConfiguration;
import de.tudarmstadt.ukp.clarin.webanno.security.model.Role;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LoggingFilter;
import de.tudarmstadt.ukp.inception.export.config.DocumentImportExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.project.export.config.ProjectExportServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServicePropertiesImpl;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import io.github.reckart.inception.humanprotocol.HumanProtocolServiceImpl;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolPropertiesImpl;
import io.github.reckart.inception.humanprotocol.messages.InviteLinkNotification;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.InternationalizedStrings;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.TaskData;
import io.github.reckart.inception.humanprotocol.model.TaskDataItem;
import io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration(exclude = LiquibaseAutoConfiguration.class)
@SpringBootTest(webEnvironment = MOCK, properties = { //
        // These properties are required for auto-config, so we need to set them here already
        "repository.path=" + JobSubmissionTest.TEST_OUTPUT_FOLDER, //
        "human-protocol.s3-endpoint=http://dummy", //
        "human-protocol.s3-region=us-west-2", //
        "human-protocol.s3-access-key-id=dummy", //
        "human-protocol.s3-secret-access-key=dummy", //
        "human-protocol.human-api-key=" + JobSubmissionTest.HUMAN_API_KEY, //
        "workload.dynamic.enabled=true", //
        "sharing.invites.enabled=true" })
@EnableWebSecurity
@Import({ //
        CasStorageServiceAutoConfiguration.class, //
        AnnotationSchemaServiceAutoConfiguration.class, //
        ProjectServiceAutoConfiguration.class, //
        ProjectExportServiceAutoConfiguration.class, //
        DocumentServiceAutoConfiguration.class, //
        DocumentImportExportServiceAutoConfiguration.class, //
        ProjectInitializersAutoConfiguration.class, //
        RepositoryAutoConfiguration.class, //
        SecurityAutoConfiguration.class })
@EntityScan({ //
        "de.tudarmstadt.ukp.inception", //
        "de.tudarmstadt.ukp.clarin.webanno.model", //
        "de.tudarmstadt.ukp.clarin.webanno.security.model" })
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JobSubmissionTest
{
    static final String TEST_OUTPUT_FOLDER = "target/test-output/JobSubmissionTest";

    private static final int EXCHANGE_ID = 4242;
    private static final String EXCHANGE_KEY = "de85eb7e-aea9-11eb-8529-0242ac130003";
    static final String HUMAN_API_KEY = "e984c52c-aea9-11eb-8529-0242ac130003";

    private @Autowired RepositoryProperties repositoryProperties;
    private @Autowired WebApplicationContext context;
    private @Autowired UserDao userRepository;
    private @Autowired ProjectService projectService;
    private @Autowired HumanProtocolServiceImpl hmtService;
    private @Autowired HumanProtocolPropertiesImpl hmtProperties;
    private @Autowired InviteService inviteService;
    private @Autowired InviteServicePropertiesImpl inviteProperties;
    private @Autowired DocumentService documentService;

    private MockMvc mvc;
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

        inviteProperties.setInviteBaseUrl("http://nevermind:8080/inception");

        hmtProperties.setJobFlowUrl(metaApiServer.url("/api").toString());
        hmtProperties.setExchangeId(EXCHANGE_ID);
        hmtProperties.setExchangeKey(EXCHANGE_KEY);
        hmtProperties.setJobFlowKey(HUMAN_API_KEY);

        // @formatter:off
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .alwaysDo(print())
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilters(new LoggingFilter(repositoryProperties.getPath().toString()))
                .addFilters(new HumanSignatureValidationFilter(hmtProperties))
                .addFilters(new OpenCasStorageSessionForRequestFilter())
                .build();
        // @formatter:on

        if (!initialized) {
            userRepository.create(new User("admin", Role.ROLE_ADMIN));
            initialized = true;
        }
    }

    @AfterEach
    public void teardown() throws Exception
    {
        metaApiServer.shutdown();
    }

    @Test
    public void thatProjectCreationFromJobAndInviteLinkNotification() throws Exception
    {
        // Expected request(s) for the manifest and for fetching the task data
        JobManifest manifest = generateJobManifestAndEnqueueDataResponses( //
                "This is document 1.", //
                "This is document 2.");
        
         // Expect request posting the invite link information
        metaApiServer.enqueue(new MockResponse().setResponseCode(200));

        assertThat(projectService.listProjects()) //
                .as("Starting with emtpy database (no projects)").hasSize(0);

        JobRequest jobRequest = createJobRequest();
        postJob(createJobRequest());

        // Validate project has been properly created
        assertThat(projectService.existsProjectWithSlug(jobRequest.getJobAddress())) //
                .as("Project has been created from the job manifest using the job address as name")
                .isTrue();

        Project project = projectService.getProjectBySlug(jobRequest.getJobAddress());
        assertThat(project.getSlug()) //
                .as("Project slug does not match") //
                .isEqualTo(jobRequest.getJobAddress());
        assertThat(project.getName()) //
                .as("Project title does not match") //
                .isEqualTo("Test project");
        assertThat(project.getDescription()) //
                .as("Project description has been set from manifest") //
                .isNotEmpty();

        Optional<JobManifest> storedManifest = hmtService.readJobManifest(project);
        assertThat(storedManifest).isPresent();
        assertThat(contentOf(hmtService.getManifestFile(project).toFile()))
                .isEqualTo(toPrettyJsonString(manifest));
        assertThatJson(toPrettyJsonString(hmtService.readJobManifest(project).get()))
                .isEqualTo(toPrettyJsonString(manifest));
        
        assertThat(documentService.listSourceDocuments(project))
            .as("All documents imported")
            .hasSize(manifest.getTaskdata().size());
        

        // Validate project has imported data to be labeled (manifest + 2 documents)
        assertThat(metaApiServer.takeRequest().getPath()).as("Data loaded").isEqualTo("/data");
        assertThat(metaApiServer.takeRequest().getPath()).as("Data loaded").startsWith("/data/");
        assertThat(metaApiServer.takeRequest().getPath()).as("Data loaded").startsWith("/data/");

        // Validate invite link notification
        RecordedRequest linkNotificationRequest = metaApiServer.takeRequest();
        assertThat(linkNotificationRequest.getPath()) //
                .as("Invite link notification recieved") //
                .endsWith(INVITE_LINK_ENDPOINT);
        String serializedNotification = linkNotificationRequest.getBody().readUtf8();
        assertThat(linkNotificationRequest.getHeader(HEADER_X_EXCHANGE_SIGNATURE))
                .isEqualTo(generateHexSignature(EXCHANGE_KEY, serializedNotification));
        assertThat(linkNotificationRequest.getHeader(CONTENT_TYPE))
                .isEqualTo(APPLICATION_JSON_VALUE);
        InviteLinkNotification notification = fromJsonString(InviteLinkNotification.class,
                serializedNotification);
        
        ProjectInvite invite = inviteService.readProjectInvite(project);
        assertThat(notification.getInviteLink()).startsWith(inviteProperties.getInviteBaseUrl());
        assertThat(notification.getInviteLink()).contains("/p/1/join-project");
        assertThat(notification.getInviteLink()).endsWith(invite.getInviteId());
        assertThat(notification.getNetworkId()).isEqualTo(jobRequest.getNetworkId());
        assertThat(notification.getJobAddress()).isEqualTo(jobRequest.getJobAddress());
        assertThat(notification.getExchangeId()).isEqualTo(hmtProperties.getExchangeId());
        assertThat(invite.getMaxAnnotatorCount())
                .isEqualTo(documentService.listSourceDocuments(project).size());
    }
    
    private JobManifest generateSpanSelectTaskJobManifest(TaskData aTaskData) {
        JobManifest manifest = new JobManifest();
        manifest.setJobId(UUID.randomUUID().toString());
        manifest.setTaskdata(aTaskData);
        manifest.setRequesterQuestion(new InternationalizedStrings() // 
                .withString("en", "Identify the rabbit."));
        manifest.setRequestType(TASK_TYPE_SPAN_SELECT);
        manifest.setRequestConfig(Map.of( //
                REQUEST_CONFIG_KEY_PROJECT_TITLE, "Test project", //
                REQUEST_CONFIG_KEY_VERSION, 0, //
                REQUEST_CONFIG_KEY_ANCHORING, ANCHORING_TOKENS, //
                REQUEST_CONFIG_KEY_OVERLAP, OVERLAP_NONE, //
                REQUEST_CONFIG_KEY_CROSS_SENENCE, false));
        return manifest;
    }

    
    private JobManifest generateJobManifestAndEnqueueDataResponses(String... aDocuments)
        throws IOException
    {
        Deque<MockResponse> responses = new LinkedList<>();
        
        TaskData taskData = new TaskData();
        for (String document : aDocuments) {
            responses.add(new MockResponse().setResponseCode(200).setBody(document));
            
            TaskDataItem item = new TaskDataItem();
            item.setTaskKey(UUID.randomUUID().toString());
            item.setDatapointUri(metaApiServer.url("/data/"+item.getTaskKey()).toString());
            item.setDatapointHash(DigestUtils.sha256Hex(document));
            taskData.add(item);
        }
        
        JobManifest manifest = generateSpanSelectTaskJobManifest(taskData);
        
        // This is actually the first request that needs to be served
        responses.push(
                new MockResponse().setResponseCode(200).setBody(toPrettyJsonString(manifest)));
        
        responses.forEach(metaApiServer::enqueue);
        
        return manifest;
    }

    private JobRequest createJobRequest()
    {
        JobRequest jobRequest = new JobRequest();
        jobRequest.setNetworkId(12345);
        jobRequest.setJobAddress("e376b295-637a-4f6f-ba5c-3662a5d57f07");
        jobRequest.setJobManifest(metaApiServer.url("/data").uri());
        return jobRequest;
    }

    private void postJob(JobRequest jobRequest) throws Exception
    {
        String body = toJsonString(jobRequest);
        String signature = generateHexSignature(HUMAN_API_KEY, body);

        // @formatter:off
        mvc.perform(post(API_BASE + "/" + SUBMIT_JOB)
                .with(csrf().asHeader())
                .with(user("admin").roles("ADMIN"))
                .header(HEADER_X_HUMAN_SIGNATURE, signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
        // @formatter:on
    }
    

    @SpringBootConfiguration
    public static class TestContext
    {
        // All handled by auto-config
    }
}
