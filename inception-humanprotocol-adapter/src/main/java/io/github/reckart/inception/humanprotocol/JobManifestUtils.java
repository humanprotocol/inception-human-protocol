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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import io.github.reckart.inception.humanprotocol.model.JobManifest;
import io.github.reckart.inception.humanprotocol.model.TaskData;

public class JobManifestUtils
{
    public static JobManifest loadManifest(InputStream aInputStream) throws IOException
    {
        return JSONUtil.fromJsonStream(JobManifest.class, aInputStream);
    }

    public static JobManifest loadManifest(File aFile) throws IOException
    {
        try (InputStream is = Files.newInputStream(aFile.toPath())) {
            return loadManifest(is);
        }
    }

    public static JobManifest loadManifest(URI aUri) throws IOException
    {
        return fetch(aUri, JobManifest.class);
    }

    public static TaskData loadTaskData(InputStream aInputStream) throws IOException
    {
        return JSONUtil.fromJsonStream(TaskData.class, aInputStream);
    }

    public static TaskData loadTaskData(File aFile) throws IOException
    {
        try (InputStream is = Files.newInputStream(aFile.toPath())) {
            return loadTaskData(is);
        }
    }

    public static TaskData loadTaskData(URI aUri) throws IOException
    {
        return fetch(aUri, TaskData.class);
    }

    public static <R> R fetch(URI aUri, Class<R> aClass)  throws IOException {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(aUri).build();
            HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
            try (InputStream is = response.body()) {
                return JSONUtil.fromJsonStream(aClass, is);
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }
}
