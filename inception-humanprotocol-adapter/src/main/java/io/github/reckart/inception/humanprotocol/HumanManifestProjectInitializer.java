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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import io.github.reckart.inception.humanprotocol.model.HumanInternationalizedStrings;
import io.github.reckart.inception.humanprotocol.model.HumanManifest;

public class HumanManifestProjectInitializer
    implements ProjectInitializer
{
    private final AnnotationSchemaService schemaService;

    private final HumanManifest manifest;

    public HumanManifestProjectInitializer(HumanManifest aManifest, AnnotationSchemaService aSchemaService)
    {
        manifest = aManifest;
        schemaService = aSchemaService;
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
        aProject.setDescription(manifest.getRequesterDescription());

        TagSet tagset = initializeTagset(aProject);
    }

    private TagSet initializeTagset(Project aProject)
    {
        TagSet tagset = new TagSet(aProject, "Tagset");
        tagset.setCreateTag(false);
        schemaService.createTagSet(tagset);
        
        List<Tag> tags = new ArrayList<>();
        for (Entry<String, HumanInternationalizedStrings> answer : manifest
                .getRequesterRestrictedAnswerSet().entrySet()) {
            Tag tag = new Tag(tagset, answer.getKey());
            tag.setDescription(answer.getValue().get("en"));
            tags.add(tag);
        }
        schemaService.createTags(tags.stream().toArray(Tag[]::new));
        
        return tagset;
    }
}
