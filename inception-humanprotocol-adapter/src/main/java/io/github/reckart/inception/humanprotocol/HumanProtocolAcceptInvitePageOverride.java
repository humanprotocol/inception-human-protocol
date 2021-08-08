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

import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.sharing.AcceptInvitePage.PAGE_PARAM_INVITE_ID;

import org.apache.wicket.protocol.http.WebApplication;
import org.springframework.core.annotation.Order;

import com.giffing.wicket.spring.boot.context.extensions.ApplicationInitExtension;
import com.giffing.wicket.spring.boot.context.extensions.WicketApplicationInitConfiguration;

import io.github.reckart.inception.humanprotocol.page.AcceptInvitePage;

// @ApplicationInitExtension
@Order(ApplicationInitExtension.DEFAULT_PRECEDENCE + 100)
public class HumanProtocolAcceptInvitePageOverride
    implements WicketApplicationInitConfiguration
{
    @Override
    public void init(WebApplication aWebApplication)
    {
        // Unmount the default accept-invite page and add a custom one which supports wallet-based
        // login
        aWebApplication.unmount(NS_PROJECT + "/0/join-project/0");
        aWebApplication.mountPage(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/join-project/${"
                + PAGE_PARAM_INVITE_ID + "}", AcceptInvitePage.class);
        
        // Disable COEP specifically for the accept-invite page so we can use w3modal on that page
        aWebApplication.getSecuritySettings().getCrossOriginEmbedderPolicyConfiguration()
                .addExemptedPath("regex:" + NS_PROJECT + "/[^/]+/join-project/.+");
    }
}
