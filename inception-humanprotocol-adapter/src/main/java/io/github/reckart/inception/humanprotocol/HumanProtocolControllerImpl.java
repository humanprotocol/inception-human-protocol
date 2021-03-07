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

import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.ALL_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.github.reckart.inception.humanprotocol.model.HumanManifest;
import io.swagger.v3.oas.annotations.Operation;

@ResponseBody
@RequestMapping(HumanProtocolController.API_BASE)
public class HumanProtocolControllerImpl
    implements HumanProtocolController
{
    private final ProjectService projectService;
    private final AnnotationSchemaService schemaService;

    public HumanProtocolControllerImpl(ProjectService aProjectService,
            AnnotationSchemaService aSchemaService)
    {
        projectService = aProjectService;
        schemaService = aSchemaService;
    }

    @Override
    @Operation(summary = "Submit new job")
    @PostMapping(path = "/" + SUBMIT_JOB, //
    consumes = APPLICATION_JSON_VALUE, //
            produces = ALL_VALUE)
    public ResponseEntity<Void> submitJob(@RequestBody HumanManifest aManifest) throws Exception
    {
        Project project = new Project(aManifest.getJobId());
        project.setDescription(aManifest.getRequesterQuestion().get("en"));
        projectService.createProject(project);
        projectService.initializeProject(project,
                asList(new HumanManifestProjectInitializer(aManifest, schemaService)));
        return new ResponseEntity<>(CREATED);
    }
}
