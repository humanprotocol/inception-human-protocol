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

import static io.github.reckart.inception.humanprotocol.HumanProtocolConstants.HEADER_X_HUMAN_SIGNATURE;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.IOException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.util.lang.Objects;

public class HumanSignatureValidationFilter
    implements Filter
{
    private static final String PARAM_SECRET_KEY = "secretKey";

    public static final String ATTR_SIGNATURE_VALID = "io.github.reckart.inception."
            + "humanprotocol.security.HumanSignatureValidationFilter#signatureValue";
    
    private byte[] secretKey;

    public HumanSignatureValidationFilter(byte[] aSecretKey)
    {
        secretKey = aSecretKey;
    }

    public HumanSignatureValidationFilter(String aSecretKey)
    {
        secretKey = decodeBase64(aSecretKey);
    }

    @Override
    public void init(FilterConfig aFilterConfig) throws ServletException
    {
        Filter.super.init(aFilterConfig);
        
        secretKey = decodeBase64(aFilterConfig.getInitParameter(PARAM_SECRET_KEY));
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

        HttpServletRequest httpRequest = (HttpServletRequest) aRequest;
        String signature = httpRequest.getHeader(HEADER_X_HUMAN_SIGNATURE);

        if (signature == null) {
            aChain.doFilter(aRequest, aResponse);
            return;
        }

        HttpServletRequest wrappedRequest = new BufferingHttpServletRequestWrapper(
                (HttpServletRequest) aRequest);

        validateSignature(signature, toByteArray(wrappedRequest.getInputStream()));

        wrappedRequest.setAttribute(ATTR_SIGNATURE_VALID, true);
        
        aChain.doFilter(wrappedRequest, aResponse);
    }

    /**
     * Validates the payload against the given base64 signature.
     */
    private void validateSignature(String aSignature, byte[] aPayload)
        throws ServletException, IOException
    {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hmacSha256 = mac.doFinal(aPayload);
            if (!Objects.isEqual(encodeBase64String(hmacSha256), aSignature)) {
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
