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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import io.github.reckart.inception.humanprotocol.model.HumanManifest;

public class HumanManifestUtils
{
    public static HumanManifest loadManifest(InputStream aInputStream) throws IOException
    {
        return JSONUtil.fromJsonStream(HumanManifest.class, aInputStream);
    }

    public static HumanManifest loadManifest(File aFile) throws IOException
    {
        try (InputStream is = Files.newInputStream(aFile.toPath())) {
            return loadManifest(is);
        }
    }
}
