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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class SignatureUtils
{
    private static final String HMAC_SHA256 = "HmacSHA256";

    public static String generateHexSignature(String aSecretKey, String aPayload)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        return generateHexSignature(UUID.fromString(aSecretKey), aPayload.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateHexSignature(String aSecretKey, byte[] aPayload)
        throws NoSuchAlgorithmException, InvalidKeyException, DecoderException
    {
        return generateHexSignature(UUID.fromString(aSecretKey), aPayload);
    }

    public static String generateHexSignature(UUID aSecretKey, String aPayload)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        return generateHexSignature(uuidToBytes(aSecretKey), aPayload.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateHexSignature(UUID aSecretKey, byte[] aPayload)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        return generateHexSignature(uuidToBytes(aSecretKey), aPayload);
    }

    public static String generateHexSignature(byte[] aSecretKey, byte[] aPayload)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(aSecretKey, HMAC_SHA256));
        return Hex.encodeHexString(mac.doFinal(aPayload));
    }

    public static byte[] uuidToBytes(UUID aKey)
    {
        ByteBuffer buf = ByteBuffer.wrap(new byte[16]);
        buf.putLong(aKey.getMostSignificantBits());
        buf.putLong(aKey.getLeastSignificantBits());
        return buf.array();
    }
}
