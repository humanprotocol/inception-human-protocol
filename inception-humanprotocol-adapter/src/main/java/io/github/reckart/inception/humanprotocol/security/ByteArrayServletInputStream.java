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
package io.github.reckart.inception.humanprotocol.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class ByteArrayServletInputStream
    extends ServletInputStream
{
    private ByteArrayInputStream stream;

    public ByteArrayServletInputStream(byte[] aBuffer)
    {
        stream = new ByteArrayInputStream(aBuffer);
    }

    @Override
    public int read() throws IOException
    {
        return stream.read();
    }

    @Override
    public boolean isFinished()
    {
        return stream.available() == 0;
    }

    @Override
    public boolean isReady()
    {
        return !isFinished();
    }

    @Override
    public void setReadListener(ReadListener listener)
    {
        throw new UnsupportedOperationException();
    }
}
