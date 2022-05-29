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
package io.github.reckart.inception.humanprotocol;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toPrettyJsonString;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_EXCHANGE_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.INVITE_LINK_ENDPOINT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.JOB_RESULTS_ENDPOINT;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.TASK_TYPE_SPAN_SELECT;
import static io.github.reckart.inception.humanprotocol.SignatureUtils.generateHexSignature;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newOutputStream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.IOUtils.copyLarge;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import org.apache.uima.UIMAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportException;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.xmi.XmiFormatSupport;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.ThresholdBasedMergeStrategy;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.project.export.ProjectExportService;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolProperties;
import io.github.reckart.inception.humanprotocol.messages.InviteLinkNotification;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.messages.JobResultSubmission;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.PayoutItem;
import io.github.reckart.inception.humanprotocol.model.Payouts;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link HumanProtocolAutoConfiguration#humanProtocolService}.
 * </p>
 */
public class HumanProtocolServiceImpl
    implements HumanProtocolService
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RepositoryProperties repositoryProperties;
    private final HumanProtocolProperties hmtProperties;
    private final InviteService inviteService;
    private final S3Client s3Client;
    private final ProjectExportService projectExportService;
    private final DocumentService documentService;
    private final AnnotationSchemaService annotationService;
    private final ProjectService projectService;
    private final CurationMergeService curationMergeService;
    private final CurationDocumentService curationDocumentService;

    public HumanProtocolServiceImpl(ProjectExportService aProjectExportService,
            InviteService aInviteService, ProjectService aProjectService,
            DocumentService aDocumentService, AnnotationSchemaService aAnnotationService,
            CurationMergeService aCurationMergeService,
            CurationDocumentService aCurationDocumentService,
            @Autowired(required = false) S3Client aS3Client,
            RepositoryProperties aRepositoryProperties, HumanProtocolProperties aHmtProperties)
    {
        repositoryProperties = aRepositoryProperties;
        inviteService = aInviteService;
        projectService = aProjectService;
        documentService = aDocumentService;
        hmtProperties = aHmtProperties;
        s3Client = aS3Client;
        projectExportService = aProjectExportService;
        curationMergeService = aCurationMergeService;
        curationDocumentService = aCurationDocumentService;
        annotationService = aAnnotationService;
    }

    @Override
    public synchronized Optional<JobManifest> readJobManifest(Project aProject) throws IOException
    {
        Path manifestFile = getManifestFile(aProject);

        if (!exists(manifestFile)) {
            return Optional.empty();
        }

        return Optional.of(JobManifestUtils.loadManifest(manifestFile.toFile()));
    }

    @Override
    public synchronized Optional<JobRequest> readJobRequest(Project aProject) throws IOException
    {
        Path jobRequstFile = getJobRequestFile(aProject);

        if (!exists(jobRequstFile)) {
            return Optional.empty();
        }

        try (InputStream is = Files.newInputStream(jobRequstFile)) {
            return Optional.of(JSONUtil.fromJsonStream(JobRequest.class, is));
        }
    }

    @Override
    public synchronized void writeJobRequest(Project aProject, JobRequest aJobRequest)
        throws IOException
    {
        Path jobRequstFile = getJobRequestFile(aProject);

        if (!exists(jobRequstFile)) {
            createDirectories(jobRequstFile.getParent());
        }

        try (Writer out = Files.newBufferedWriter(jobRequstFile, UTF_8)) {
            out.write(JSONUtil.toPrettyJsonString(aJobRequest));
        }
    }

    @Override
    public synchronized void importJobManifest(Project aProject, InputStream aManifestSource)
        throws IOException
    {
        Path manifestFile = getManifestFile(aProject);

        if (!exists(manifestFile)) {
            createDirectories(manifestFile.getParent());
        }

        try (OutputStream os = newOutputStream(manifestFile)) {
            copyLarge(aManifestSource, os);
        }
    }

    @Override
    public synchronized void writeJobManifest(Project aProject, JobManifest aManifest)
        throws IOException
    {
        Path jobManifestFile = getManifestFile(aProject);

        if (!exists(jobManifestFile)) {
            createDirectories(jobManifestFile.getParent());
        }

        try (Writer out = Files.newBufferedWriter(jobManifestFile, UTF_8)) {
            out.write(JSONUtil.toPrettyJsonString(aManifest));
        }
    }

    public Path getManifestFile(Project aProject)
    {
        return repositoryProperties.getPath().toPath().resolve("hmt").resolve("job-manifest.json");
    }

    public Path getJobRequestFile(Project aProject)
    {
        return repositoryProperties.getPath().toPath().resolve("hmt").resolve("job-request.json");
    }

    private String getExportKey(JobRequest aJobRequest)
    {
        return String.format("%s/%s", aJobRequest.getJobAddress(), RESULTS_KEY_SUFFIX);
    }

    public Payouts getPayouts(Project aProject)
    {
        Payouts payouts = new Payouts();

        List<User> annotators = projectService.listProjectUsersWithPermissions(aProject, ANNOTATOR);
        for (User annotator : annotators) {
            PayoutItem payoutItem = new PayoutItem();
            payoutItem.setWallet(annotator.getUiName());

            payoutItem.setTaskIds(documentService //
                    .listAnnotationDocumentsWithStateForUser(aProject, annotator, FINISHED).stream()
                    .map(ann -> ann.getDocument().getName()) //
                    .collect(toList()));

            payouts.add(payoutItem);
        }

        return payouts;
    }

    private void autoCurateDocuments(Project aProject, JobManifest aJobManifest)
        throws IOException, UIMAException
    {
        var minRepeats = aJobManifest.getRequesterMinRepeats();
        var confidenceThreshold = aJobManifest.requesterAccuracyTarget().orElseThrow(
                () -> new IllegalArgumentException("Manifest does not define a target accuracy"));
        var topRanks = 1;
        var mergeStrategy = new ThresholdBasedMergeStrategy(minRepeats, confidenceThreshold,
                topRanks);

        var typeSystem = annotationService.getFullProjectTypeSystem(aProject);

        var documents = documentService.listSourceDocuments(aProject);
        int i = 0;
        for (SourceDocument doc : documents) {
            i++;
            log.trace("Auto-curating {} [{} of {}] using {}", doc, i, documents.size(),
                    mergeStrategy);

            var finishedAnnDocuments = documentService.listFinishedAnnotationDocuments(doc);
            if (finishedAnnDocuments.isEmpty()) {
                continue;
            }

            try (var session = CasStorageSession.openNested()) {
                var casByUser = documentService.readAllCasesSharedNoUpgrade(finishedAnnDocuments);

                var curationCas = documentService.createOrReadInitialCas(doc, FORCE_CAS_UPGRADE,
                        UNMANAGED_ACCESS, typeSystem);
                curationMergeService.mergeCasses(doc, CURATION_USER, curationCas, casByUser,
                        mergeStrategy);

                curationDocumentService.writeCurationCas(curationCas, doc, false);
            }

            documentService.setSourceDocumentState(doc, CURATION_FINISHED);
        }

        projectService.setProjectState(aProject, ProjectState.CURATION_FINISHED);
    }

    public void publishResults(Project aProject, JobRequest aJobRequest, JobManifest aJobManifest)
        throws ProjectExportException, IOException
    {
        if (!hmtProperties.isS3BucketInformationAvailable() || s3Client == null) {
            log.warn("No S3 bucket information has been provided - not publishing results");
            return;
        }

        File exportedProjectFile = null;
        String exportKey = getExportKey(aJobRequest);

        try {
            ProjectExportTaskMonitor monitor = new ProjectExportTaskMonitor(aProject, null,
                    "publish");
            FullProjectExportRequest exportRequest = new FullProjectExportRequest(aProject,
                    XmiFormatSupport.ID, true);
            exportRequest.setFilenameTag("_project");

            exportedProjectFile = projectExportService.exportProject(exportRequest, monitor);

            s3Client.putObject(PutObjectRequest.builder() //
                    .bucket(hmtProperties.getS3Bucket()) //
                    .key(exportKey)//
                    .build(), RequestBody.fromFile(exportedProjectFile));
            log.info("Published results to S3");
        }
        catch (InterruptedException e) {
            log.warn("Sending results notification aborted: " + e.getMessage(), e);
            return;
        }
        finally {
            deleteQuietly(exportedProjectFile);
        }

        if (hmtProperties.getJobFlowUrl() == null) {
            log.warn(
                    "No HUMAN Protocol Job Flow URL has been provided - not sending results notification");
            return;
        }

        JobResultSubmission resultNotification = new JobResultSubmission();
        resultNotification.setNetworkId(aJobRequest.getNetworkId());
        resultNotification.setJobAddress(aJobRequest.getJobAddress());
        resultNotification.setExchangeId(hmtProperties.getExchangeId());
        resultNotification.setJobData(URI.create(format("%s/%s/%s", hmtProperties.getS3Endpoint(),
                hmtProperties.getS3Bucket(), exportKey)));
        resultNotification.setPayouts(getPayouts(aProject));

        postSignedMessageToHumanApi(JOB_RESULTS_ENDPOINT, resultNotification);
        log.info("Notified HUMAN Protocol Job Flow about the results");
    }

    @Override
    public void publishInviteLink(Project aProject) throws IOException
    {
        Optional<JobRequest> optJobRequest = readJobRequest(aProject);
        if (optJobRequest.isEmpty()) {
            log.trace("{} is not a HMT project - not sending invite link", aProject);
            return;
        }

        if (hmtProperties.getJobFlowUrl() == null) {
            log.warn("No HUMAN Protocol Job Flow URL has been provided - not sending invite link");
            return;
        }

        JobRequest jobRequest = optJobRequest.get();
        ProjectInvite invite = inviteService.readProjectInvite(aProject);
        String inviteLinkUrl = inviteService.getFullInviteLinkUrl(invite);

        InviteLinkNotification msg = new InviteLinkNotification();
        msg.setInviteLink(inviteLinkUrl);
        msg.setExchangeId(hmtProperties.getExchangeId());
        msg.setJobAddress(jobRequest.getJobAddress());
        msg.setNetworkId(jobRequest.getNetworkId());

        postSignedMessageToHumanApi(INVITE_LINK_ENDPOINT, msg);

        log.info("Notified HUMAN Protocol Job Flow about the invite link");
    }

    private void postSignedMessageToHumanApi(String aEndpoint, Object aMessage) throws IOException
    {
        String serializedMessage = toPrettyJsonString(aMessage);
        String signature;
        try {
            signature = generateHexSignature(hmtProperties.getExchangeKey(), serializedMessage);
        }
        catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IOException("Unable to generate message signature", ex);
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(hmtProperties.getJobFlowUrl() + aEndpoint)) //
                .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .header(HEADER_X_EXCHANGE_SIGNATURE, signature)
                .POST(BodyPublishers.ofString(serializedMessage, UTF_8)).build();

        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString(UTF_8));
            if (response.statusCode() != 200) {
                throw new IOException("Posting message failed with status " + response.statusCode()
                        + ": " + response.body());
            }
        }
        catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @EventListener
    public void onProjectStateChange(ProjectStateChangedEvent aEvent)
    {
        if (aEvent.getNewState() != ANNOTATION_FINISHED) {
            return;
        }

        try {
            Project project = aEvent.getProject();

            Optional<JobManifest> optManifest = readJobManifest(project);
            Optional<JobRequest> optJobRequest = readJobRequest(project);

            if (optManifest.isEmpty() || optJobRequest.isEmpty()) {
                log.trace("{} is not a HUMAN Protocol project - not triggering submission",
                        project);
                return;
            }

            JobManifest manifest = optManifest.get();

            if (TASK_TYPE_SPAN_SELECT.equals(manifest.getRequestType())
                    && manifest.requesterAccuracyTarget().isPresent()) {
                autoCurateDocuments(project, manifest);
            }

            publishResults(project, optJobRequest.get(), manifest);
        }
        catch (Exception e) {
            log.error("Unable to trigger submission", e);
        }
    }
}
