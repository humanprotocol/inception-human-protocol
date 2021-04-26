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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newOutputStream;
import static org.apache.commons.io.IOUtils.copyLarge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import io.github.reckart.inception.humanprotocol.config.HumanProtocolAutoConfiguration;
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
    private final RepositoryProperties repositoryProperties;

    public HumanProtocolServiceImpl(RepositoryProperties aRepositoryProperties)
    {
        repositoryProperties = aRepositoryProperties;
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
    public synchronized void importJobManifest(Project aProject, URL aManifestUrl) throws IOException
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
}
