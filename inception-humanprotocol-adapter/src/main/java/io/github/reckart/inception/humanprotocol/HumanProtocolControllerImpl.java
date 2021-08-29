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

import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.REQUEST_CONFIG_KEY_PROJECT_TITLE;
import static io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter.ATTR_SIGNATURE_VALID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.swagger.v3.oas.annotations.Operation;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link HumanProtocolAutoConfiguration#humanProtocolController}.
 * </p>
 */
@ResponseBody
@RequestMapping(HumanProtocolController.API_BASE)
public class HumanProtocolControllerImpl
    implements HumanProtocolController
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationContext applicationContext;
    private final ProjectService projectService;
    private final HumanProtocolService hmtService;

    public HumanProtocolControllerImpl(ApplicationContext aApplicationContext,
            ProjectService aProjectService, HumanProtocolService aHmtService)
    {
        applicationContext = aApplicationContext;
        projectService = aProjectService;
        hmtService = aHmtService;
    }

    @Override
    @Operation(summary = "Submit new job")
    @PostMapping(path = "/" + SUBMIT_JOB, //
            consumes = APPLICATION_JSON_VALUE, //
            produces = ALL_VALUE)
    public ResponseEntity<Void> submitJob(
            @RequestAttribute(ATTR_SIGNATURE_VALID) boolean aSignatureValid,
            @RequestBody JobRequest aJobRequest)
        throws Exception
    {
        if (!aSignatureValid) {
            return new ResponseEntity<>(BAD_REQUEST);
        }

        createJob(aJobRequest);

        return new ResponseEntity<>(CREATED);
    }

    @Override
    @Operation(summary = "Submit new job by manifest")
    @PostMapping(path = "/" + SUBMIT_JOB_MANIFEST, //
            consumes = APPLICATION_JSON_VALUE, //
            produces = ALL_VALUE)
    public ResponseEntity<Void> submitJobManifest(
            @RequestParam(PARAM_JOB_ADDRESS) String aJobAddress,
            @RequestParam(PARAM_NETWORK_ID) int aNetworkId,
            @RequestAttribute(ATTR_SIGNATURE_VALID) boolean aSignatureValid,
            @RequestBody JobManifest aJobManifest)
        throws Exception
    {
        if (!aSignatureValid) {
            return new ResponseEntity<>(BAD_REQUEST);
        }

        File tmpManifestFile = null;
        try {
            tmpManifestFile = File.createTempFile("manifest", ".json");
            FileUtils.write(tmpManifestFile, JSONUtil.toPrettyJsonString(aJobManifest), UTF_8);

            JobRequest jobRequest = new JobRequest();
            jobRequest.setJobAddress(aJobAddress);
            jobRequest.setNetworkId(aNetworkId);
            jobRequest.setJobManifest(tmpManifestFile.toURI());
            createJob(jobRequest);
        }
        finally {
            if (tmpManifestFile != null && tmpManifestFile.exists()) {
                tmpManifestFile.delete();
            }
        }

        return new ResponseEntity<>(CREATED);
    }

    public void createJob(JobRequest aJobRequest) throws IOException
    {
        Project project = new Project(aJobRequest.getJobAddress());
        projectService.createProject(project);

        try {
            hmtService.writeJobRequest(project, aJobRequest);

            try (InputStream is = aJobRequest.getJobManifest().toURL().openStream()) {
                hmtService.importJobManifest(project, is);
            }

            JobManifest manifest = hmtService.readJobManifest(project).get();
            if (manifest.getRequesterQuestion() != null) {
                project.setDescription(manifest.getRequesterQuestion().get("en"));
            }

            String name = (String) manifest.getRequestConfig()
                    .get(REQUEST_CONFIG_KEY_PROJECT_TITLE);
            if (isNotBlank(name)) {
                project.setName(name);
            }

            projectService.updateProject(project);

            HumanProtocolProjectInitializer initializer = new HumanProtocolProjectInitializer(
                    manifest);

            AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
            factory.autowireBean(initializer);
            factory.initializeBean(initializer, "transientInitializer");

            projectService.initializeProject(project, asList(initializer));

            hmtService.publishInviteLink(project);
        }
        catch (Exception e) {
            try {
                projectService.removeProject(project);
            }
            catch (Exception ex) {
                log.error("Unable to clean up project after failing to accept job submission", ex);
            }

            throw e;
        }
    }
}
