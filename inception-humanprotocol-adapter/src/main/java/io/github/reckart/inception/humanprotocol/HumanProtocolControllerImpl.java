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

import static io.github.reckart.inception.humanprotocol.JobManifestUtils.loadManifest;
import static io.github.reckart.inception.humanprotocol.security.HumanSignatureValidationFilter.ATTR_SIGNATURE_VALID;
import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.github.reckart.inception.humanprotocol.messages.JobRequest;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.swagger.v3.oas.annotations.Operation;

@ResponseBody
@RequestMapping(HumanProtocolController.API_BASE)
public class HumanProtocolControllerImpl
    implements HumanProtocolController
{
    private final ApplicationContext applicationContext;
    private final ProjectService projectService;

    public HumanProtocolControllerImpl(ApplicationContext aApplicationContext,
            ProjectService aProjectService)
    {
        applicationContext = aApplicationContext;
        projectService = aProjectService;
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

        JobManifest manifest = loadManifest(aJobRequest.getJobManifest());
        createJob(manifest);
        return new ResponseEntity<>(CREATED);
    }

    public void createJob(JobManifest aManifest) throws IOException
    {
        Project project = new Project(aManifest.getJobId());
        project.setDescription(aManifest.getRequesterQuestion().get("en"));
        projectService.createProject(project);
        
        HumanProtocolProjectInitializer initializer = new HumanProtocolProjectInitializer(aManifest);
        
        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(initializer);
        factory.initializeBean(initializer, "transientInitializer");
        
        projectService.initializeProject(project, asList(initializer));
    }
}
