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

import static de.tudarmstadt.ukp.clarin.webanno.model.ProjectState.ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil.toPrettyJsonString;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.INVITE_LINK_ENDPOINT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolProperties;
import io.github.reckart.inception.humanprotocol.messages.InviteLinkNotification;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;

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

    public HumanProtocolServiceImpl(RepositoryProperties aRepositoryProperties,
            InviteService aInviteService, HumanProtocolProperties aHmtProperties)
    {
        repositoryProperties = aRepositoryProperties;
        inviteService = aInviteService;
        hmtProperties = aHmtProperties;
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
    public synchronized void importJobManifest(Project aProject, URL aManifestUrl)
        throws IOException
    {
        Path manifestFile = getManifestFile(aProject);

        if (!exists(manifestFile)) {
            createDirectories(manifestFile.getParent());
        }

        try (InputStream is = aManifestUrl.openStream();
                OutputStream os = newOutputStream(manifestFile)) {
            copyLarge(is, os);
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
    
    @Override
    public void publishInviteLink(Project aProject) throws IOException
    {
        Optional<JobRequest> optJobRequest = readJobRequest(aProject);
        if (optJobRequest.isEmpty()) {
            log.trace("{} is not a HMT project - not sending invite link", aProject);
            return;
        }

        if (hmtProperties.getMetaApiUrl() == null) {
            log.warn("No meta API URL has been provided - not sending invite link");
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
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder() //
                .uri(URI.create(hmtProperties.getMetaApiUrl() + INVITE_LINK_ENDPOINT)) //
                .POST(BodyPublishers.ofString(toPrettyJsonString(msg), UTF_8))
                .build();
        
        try {
            HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IOException(
                        "Invite link publication failed with status " + response.statusCode());
            }
        }
        catch (InterruptedException e ) {
            throw new IOException(e);
        }
    }
}
