/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * Utility to calculate signatures for information.
 */
public class Signature {
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  /**
   * Calculate an RFC2104 compliant HMAC (Hash-based Message Authentication Code)
   * 
   * @param data
   *          The data to be signed. This data is pushed through a hex converter in this
   *          method, so there is no need to do this before generating the HMAC.
   * @param key
   *          The signing key.
   * @return The Base64-encoded RFC 2104-compliant HMAC signature.
   * @throws java.security.SignatureException
   *           when signature generation fails
   */
  public static String calculateRFC2104HMAC(String data, String key)
      throws java.security.SignatureException {
	  return calculateRFC2104HMACWithEncoding(data, key, false);
  }
  
  public static String calculateRFC2104HMACWithEncoding(String data, String key, boolean urlSafe) 
  	throws java.security.SignatureException {
    if (data == null) {
      throw new IllegalArgumentException("String data == null");
    }
    if (key == null) {
      throw new IllegalArgumentException("String key == null");
    }
	  try {
		  // Get an hmac_sha1 key from the raw key bytes
		  byte[] keyBytes = key.getBytes("UTF-8");
		  SecretKeySpec signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);

		  // Get an hmac_sha1 Mac instance and initialize with the signing key
		  Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
		  mac.init(signingKey);

		  // Compute the hmac on input data bytes
		  byte[] rawHmac = mac.doFinal(data.getBytes("UTF-8"));

		  // Convert raw bytes to encoding
		  byte[] base64Bytes = Base64.encodeBase64(rawHmac, false, urlSafe);
		  String result = new String(base64Bytes, "UTF-8");

		  return result;

	  } catch (NoSuchAlgorithmException e) {
		  throw new SignatureException("Failed to generate HMAC : " + e.getMessage(), e);
	  } catch (InvalidKeyException e) {
		  throw new SignatureException("Failed to generate HMAC : " + e.getMessage(), e);
	  } catch (UnsupportedEncodingException e) {
		  throw new SignatureException("Failed to generate HMAC : " + e.getMessage(), e);
	  }
  }
}