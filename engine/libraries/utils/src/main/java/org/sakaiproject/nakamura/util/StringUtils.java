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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 */
public class StringUtils {

  private static final char[] TOHEX = "0123456789abcdef".toCharArray();
  public static final String UTF8 = "UTF8";
  
  public static final char[] URL_SAFE_ENCODING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    .toCharArray();

  /**
   * @param st
   * @param sep
   * @return an array of the strings between the separator
   */
  public static String[] split(String st, char sep) {

    if (st == null) {
      return new String[0];
    }
    char[] pn = st.toCharArray();
    if (pn.length == 0) {
      return new String[0];
    }
    int n = 1;
    int start = 0;
    int end = pn.length;
    while (start < end && sep == pn[start])
      start++;
    while (start < end && sep == pn[end - 1])
      end--;
    for (int i = start; i < end; i++) {
      if (sep == pn[i]) {
        n++;
      }
    }
    String[] e = new String[n];
    int s = start;
    int j = 0;
    for (int i = start; i < end; i++) {
      if (pn[i] == sep) {
        e[j++] = new String(pn, s, i - s);
        s = i + 1;
      }
    }
    if (s < end) {
      e[j++] = new String(pn, s, end - s);
    }
    return e;
  }

  /**
   * Split a string based on a character with limited number of items returned, this will
   * return nothing if maxElements is 0, this will removing leading and trailing split
   * chars
   * 
   * @param st
   *          the string to split
   * @param sep
   *          the char to split on
   * @param maxElements
   *          the max number of elements to return in the array
   * @return an array of the strings between the separator
   */
  public static String[] split(String st, char sep, int maxElements) {
    if (st == null) {
      return new String[maxElements];
    }
    char[] pn = st.toCharArray();
    int n = 1;
    int start = 0;
    int end = pn.length;
    while (start < end && sep == pn[start])
      start++;
    while (start < end && sep == pn[end - 1])
      end--;
    for (int i = start; i < end; i++) {
      if (sep == pn[i]) {
        n++;
      }
    }
    String[] e = new String[Math.min(maxElements, n)];
    int s = start;
    int j = 0;
    for (int i = start; i < end && j < e.length; i++) {
      if (pn[i] == sep) {
        e[j++] = new String(pn, s, i - s);
        s = i + 1;
      }
    }
    if (s < end && j < e.length) {
      e[j++] = new String(pn, s, end - s);
    }
    if (j == 0) {
      e[0] = "";
    }
    return e;
  }

  public static String sha1Hash(String tohash) throws UnsupportedEncodingException,
      NoSuchAlgorithmException {
    byte[] b = tohash.getBytes("UTF-8");
    MessageDigest sha1 = MessageDigest.getInstance("SHA");
    b = sha1.digest(b);
    return byteToHex(b);
  }

  public static String byteToHex(byte[] base) {
    char[] c = new char[base.length * 2];
    int i = 0;

    for (byte b : base) {
      int j = b;
      j = j + 128;
      c[i++] = TOHEX[j / 0x10];
      c[i++] = TOHEX[j % 0x10];
    }
    return new String(c);
  }

  /**
   * @param owners
   * @param owner
   * @return
   */
  public static String[] addString(String[] a, String v) {
    for (String o : a) {
      if (v.equals(o)) {
        return a;
      }
    }
    String[] na = new String[a.length + 1];
    for (int i = 0; i < a.length; i++) {
      na[i] = a[i];
    }
    na[na.length - 1] = v;
    return na;
  }

  /**
   * @param owners
   * @param owner
   * @return
   */
  public static String[] removeString(String[] a, String v) {
    int i = 0;
    for (String o : a) {
      if (!v.equals(o)) {
        i++;
      }
    }
    if (i == a.length) {
      return a;
    }
    String[] na = new String[i];
    i = 0;
    for (String o : a) {
      if (!v.equals(o)) {
        na[i++] = o;
      }
    }
    return na;
  }

  /**
   * Checks to see if the value is empty
   * 
   * @param firstName
   * @return
   */
  public static boolean isEmpty(String firstName) {
    return (firstName == null || firstName.trim().length() == 0);
  }

  /**
   * Builds a string based on the elements the the array
   * 
   * @param elements
   *          the elements to build the string from
   * @param i
   *          the staring index.
   * @param c
   *          the seperator character
   * @return a joined string starting with the seperator.
   */
  public static String join(String[] elements, int i, char c) {
    StringBuilder sb = new StringBuilder();
    for (int j = i; j < elements.length; j++) {
      sb.append(c).append(elements[j]);
    }
    if (sb.length() == 0) {
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * @param query
   * @return
   */
  public static String escapeJCRSQL(String query) {
    StringBuilder sb = new StringBuilder();
    char[] ca = query.toCharArray();
    for (char c : ca) {
      switch (c) {
      case '\'':
        sb.append("''");
        break;
      case '\"':
        sb.append("\\\"");
        break;
      default:
        sb.append(c);
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Removes all space chars, useful for test comparisons, not much use elsewhere.
   * 
   * @param after
   * @return
   */
  public static String stripBlanks(String before) {
    char[] cb = before.toCharArray();
    char[] ca = new char[cb.length];
    int i = 0;
    for (char c : cb) {
      if (!Character.isSpaceChar(c)) {
        ca[i++] = c;
      }
    }
    return new String(ca, 0, i);
  }

  /**
   * Generate an encoded array of chars using as few chars as possible
   * 
   * @param hash
   *          the hash to encode
   * @param encode
   *          a char array of encodings any length you lik but probably but the shorter it
   *          is the longer the result. Dont be dumb and use an encoding size of < 2.
   * @return
   */
  public static String encode(byte[] hash, char[] encode) {
    StringBuilder sb = new StringBuilder((hash.length*15)/10);
    int x = (int) (hash[0] + 128);
    int xt = 0;
    int i = 0;
    while (i < hash.length) {
      if (x < encode.length) {
        i++;
        if (i < hash.length) {
          if (x == 0) {
            x = (int) (hash[i] + 128);
          } else {
            x = (x + 1) * (int) (hash[i] + 128);
          }
        } else {
          sb.append(encode[x]);
          break;
        }
      }
      xt = x % encode.length;
      x = x / encode.length;
      sb.append(encode[xt]);
    }

    return sb.toString();
  }

}
