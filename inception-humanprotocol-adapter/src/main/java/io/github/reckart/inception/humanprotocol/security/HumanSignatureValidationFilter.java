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

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_ADMIN;
import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_HUMAN_SIGNATURE;
import static io.github.reckart.inception.humanprotocol.SignatureUtils.generateHexSignature;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.IOException;
import java.util.Collections;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.util.lang.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.GenericFilterBean;

import io.github.reckart.inception.humanprotocol.config.HumanProtocolProperties;

public class HumanSignatureValidationFilter
    extends GenericFilterBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String PARAM_HUMAN_API_KEY = "humanApiKey";
    public static final String ANY_KEY = "*";

    public static final String ATTR_SIGNATURE_VALID = "io.github.reckart.inception."
            + "humanprotocol.security.HumanSignatureValidationFilter#signatureValue";

    private String jobFlowKey;

    @Autowired
    public HumanSignatureValidationFilter(HumanProtocolProperties aProperties)
    {
        if (ANY_KEY.equals(aProperties.getJobFlowKey())) {
            jobFlowKey = null;
        }
        else {
            jobFlowKey = aProperties.getJobFlowKey();
        }
    }

    @Override
    public void doFilter(ServletRequest aRequest, ServletResponse aResponse, FilterChain aChain)
        throws IOException, ServletException
    {
        // Only HttpServletRequest can have headers - let other requests pass
        if (!(aRequest instanceof HttpServletRequest)) {
            aChain.doFilter(aRequest, aResponse);
            return;
        }

        // If no secret key is set (i.e. any key is accepted), we always mark the signature as valid
        if (jobFlowKey == null) {
            SecurityContextHolder.getContext()
                    .setAuthentication(new PreAuthenticatedAuthenticationToken("HumanProtocol",
                            null, asList(new SimpleGrantedAuthority(ROLE_ADMIN.toString()))));
            aRequest.setAttribute(ATTR_SIGNATURE_VALID, true);
            aChain.doFilter(aRequest, aResponse);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) aRequest;
        String signature = httpRequest.getHeader(HEADER_X_HUMAN_SIGNATURE);

        if (signature == null) {
            throw new ServletException("Missing signature header [" + HEADER_X_HUMAN_SIGNATURE
                    + "]. Headers found: " + Collections.list(httpRequest.getHeaderNames()));
            // aChain.doFilter(aRequest, aResponse);
            // return;
        }

        HttpServletRequest wrappedRequest = new BufferingHttpServletRequestWrapper(
                (HttpServletRequest) aRequest);

        validateSignature(signature, toByteArray(wrappedRequest.getInputStream()));
        
        SecurityContextHolder.getContext()
                .setAuthentication(new PreAuthenticatedAuthenticationToken("HumanProtocol", null,
                        asList(new SimpleGrantedAuthority(ROLE_ADMIN.toString()))));

//        setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
//                SecurityContextHolder.getContext());

        wrappedRequest.setAttribute(ATTR_SIGNATURE_VALID, true);

        aChain.doFilter(wrappedRequest, aResponse);
    }

    /**
     * Validates the payload against the given hex-encoded signature.
     */
    private void validateSignature(String aSignature, byte[] aPayload)
        throws ServletException, IOException
    {
        try {
            String expected = generateHexSignature(jobFlowKey, aPayload);
            if (!Objects.isEqual(expected, aSignature)) {
                log.warn("Invalid signature. Expected [{}] but got [{}]. {} byte message", expected,
                        aSignature, aPayload.length);
                throw new ServletException("Invalid signature");
            }
        }
        catch (ServletException e) {
            throw e;
        }
        catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
