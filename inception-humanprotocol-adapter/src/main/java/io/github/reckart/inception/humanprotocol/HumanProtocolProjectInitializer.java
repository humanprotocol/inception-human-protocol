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

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.SPAN_TYPE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SENTENCES;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static java.io.File.createTempFile;
import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.workload.dynamic.DynamicWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.dynamic.trait.DynamicWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;
import io.github.reckart.inception.humanprotocol.model.InternationalizedStrings;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.TaskData;
import io.github.reckart.inception.humanprotocol.model.TaskDataItem;

public class HumanProtocolProjectInitializer
    implements ProjectInitializer
{
//    private final Logger log = LoggerFactory.getLogger(getClass());

    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired DocumentService documentService;
    private @Autowired WorkloadManagementService workloadService;
    private @Autowired DynamicWorkloadExtension dynamicWorkload;
    private @Autowired ProjectService projectService;
    private @Autowired UserDao userService;
    private @Autowired InviteService inviteService;
    
    private final JobManifest manifest;

    public HumanProtocolProjectInitializer(JobManifest aManifest)
    {
        manifest = aManifest;
    }

    @Override
    public String getName()
    {
        return "HUMAN Protocol Manifest-based Initializer";
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(TokenLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        initializeProjectRoles(aProject);
        
        initializeProjectDescription(aProject);

        initializeTask(aProject);

        initializeTaskData(aProject);
        
        initializeWorkloadManagement(aProject);
        
        initializeAnnotatorAccess(aProject);
    }

    private void initializeAnnotatorAccess(Project aProject)
    {
        Date expirationDate;
        if (manifest.getExpirationDate() > 0) {
            expirationDate = new Date(manifest.getExpirationDate());
        } else {
            // By default, we use six months until the link expires
            Calendar expirationCalendar = Calendar.getInstance();
            expirationCalendar.add(Calendar.MONTH, 6);
            expirationDate = expirationCalendar.getTime();
        }
        
        inviteService.generateInviteWithExpirationDate(aProject, expirationDate);
        
        ProjectInvite invite = inviteService.readProjectInvite(aProject);
        invite.setGuestAccessible(true);
        invite.setInvitationText("To earn credit for your annotations, please enter your Ethereum "
                + "walled address as user ID below.");
        inviteService.writeProjectInvite(invite);
    }

    private void initializeProjectRoles(Project aProject)
    {
        User currentUser = userService.getCurrentUser();
        projectService.setProjectPermissionLevels(currentUser, aProject, asList(MANAGER, CURATOR));
    }

    private void initializeTaskData(Project aProject) throws IOException
    {
        if (manifest.getTaskdataUri() == null) {
            return;
        }
        
        TaskData taskData = JobManifestUtils.loadTaskData(URI.create(manifest.getTaskdataUri()));

        HttpClient client = HttpClient.newHttpClient();
        for (TaskDataItem item : taskData) {
            URI datapointUri = URI.create(item.getDatapointUri());
            File tmpFile = createTempFile("taskDataItem", getExtension(datapointUri.getPath()));
            try {
                HttpRequest request = HttpRequest.newBuilder().uri(datapointUri).build();
                HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                FileUtils.write(tmpFile, response.body(), UTF_8);

                SourceDocument sourceDocument = new SourceDocument(FilenameUtils
                        .getName(datapointUri.getPath()), aProject, TextFormatSupport.ID);
                documentService.createSourceDocument(sourceDocument);
                try (InputStream is = new FileInputStream(tmpFile)) {
                    documentService.uploadSourceDocument(is, sourceDocument);
                }
            }
            catch (IOException e) {
                throw e;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
            finally {
                if (tmpFile.exists()) {
                    tmpFile.delete();
                }
            }
        }
    }

    private void initializeTask(Project aProject)
    {
        switch (manifest.getRequestType()) {
        case "span_select":
            initializeSpanSelectionTask(aProject);
            break;
        default:
            throw new IllegalArgumentException(
                    "Unsupported request type [" + manifest.getRequestType() + "]");
        }
    }
    
    private void initializeWorkloadManagement(Project aProject)
    {
        WorkloadManager mgr = workloadService.loadOrCreateWorkloadManagerConfiguration(aProject);
        mgr.setType(dynamicWorkload.getId());
        DynamicWorkloadTraits traits = dynamicWorkload.readTraits(mgr);
        // FIXME: Do we have the number of annotators per document in the manifest?
        traits.setDefaultNumberOfAnnotations(3);
        workloadService.saveConfiguration(mgr);
    }
 
    private void initializeSpanSelectionTask(Project aProject)
    {
        TagSet tagset = initializeTagset(aProject);

        AnchoringMode anchoringMode;
        Object anchoringModeValue = manifest.getRequestConfig().getOrDefault("anchoring", "tokens");
        if ("tokens".equals(anchoringModeValue)) {
            anchoringMode = TOKENS;
        }
        else if ("sentences".equals(anchoringModeValue)) {
            anchoringMode = SENTENCES;
        }
        else {
            anchoringMode = TOKENS;
        }

        OverlapMode overlapMode;
        Object overlapModeValue = manifest.getRequestConfig().getOrDefault("overlap", "none");
        if ("none".equals(overlapModeValue)) {
            overlapMode = NO_OVERLAP;
        }
        else if ("any".equals(overlapModeValue)) {
            overlapMode = ANY_OVERLAP;
        }
        else {
            overlapMode = NO_OVERLAP;
        }

        boolean crossSentence = false;
        Object crossSentenceValue = manifest.getRequestConfig().getOrDefault("crossSentence",
                false);
        if (TRUE.equals(crossSentenceValue)) {
            crossSentence = true;
        }

        AnnotationLayer spanLayer = new AnnotationLayer("custom.Span", "Span", SPAN_TYPE, aProject,
                false, anchoringMode, overlapMode);
        spanLayer.setCrossSentence(crossSentence);
        schemaService.createOrUpdateLayer(spanLayer);

        AnnotationFeature stringFeature = new AnnotationFeature(aProject, spanLayer, "value",
                "Value", TYPE_NAME_STRING);
        stringFeature.setTagset(tagset);
        schemaService.createFeature(stringFeature);
    }

    private void initializeProjectDescription(Project aProject)
    {
        StringBuilder description = new StringBuilder();

        if (isNotBlank(manifest.getRequesterDescription())) {
            description.append("#### Requester Description\n");
            description.append(manifest.getRequesterDescription());
            description.append("\n\n");
        }

        if (isNotBlank(manifest.getRequesterQuestion().getOrDefault("en", ""))) {
            description.append("#### Requester Question\n");
            description.append(manifest.getRequesterQuestion().getOrDefault("en", ""));
            description.append("\n\n");
        }

        aProject.setDescription(description.toString());
    }

    private TagSet initializeTagset(Project aProject)
    {
        TagSet tagset = new TagSet(aProject, "Tagset");
        tagset.setCreateTag(false);
        schemaService.createTagSet(tagset);

        List<Tag> tags = new ArrayList<>();
        for (Entry<String, InternationalizedStrings> answer : manifest
                .getRequesterRestrictedAnswerSet().entrySet()) {
            Tag tag = new Tag(tagset, answer.getKey());
            tag.setDescription(answer.getValue().get("en"));
            tags.add(tag);
        }
        schemaService.createTags(tags.stream().toArray(Tag[]::new));

        return tagset;
    }
}
