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
package io.github.reckart.inception.humanprotocol.page;

import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class FortmaticResourceReference
    extends JavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final FortmaticResourceReference INSTANCE = new FortmaticResourceReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static FortmaticResourceReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private FortmaticResourceReference()
    {
        super(FortmaticResourceReference.class, "fortmatic_2_0_6.min.js");
    }
}
