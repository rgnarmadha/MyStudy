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
 */package org.sakaiproject.nakamura.util;


/**
 * Some of this code comes from Apache Axis. Header has been left as is, some
 * modifications have been made.
 *
 * Abstract class for XML String encoders.
 *
 * The new encoding mechanism fixes the following bugs/issues:
 * http://nagoya.apache.org/bugzilla/show_bug.cgi?id=15133
 * http://nagoya.apache.org/bugzilla/show_bug.cgi?id=15494
 * http://nagoya.apache.org/bugzilla/show_bug.cgi?id=19327
 *
 * @author <a href="mailto:jens@void.fm">Jens Schumann</a>
 * @author <a href="mailto:dims@yahoo.com">Davanum Srinivas</a>
 *
 */
public class XmlUtils {
  /**
   * The schema language being used.
   */

  protected static final String AMP = "&amp;";
  protected static final String QUOTE = "&quot;";
  protected static final String LESS = "&lt;";
  protected static final String GREATER = "&gt;";
  protected static final String LF = "\n";
  protected static final String CR = "\r";
  protected static final String TAB = "\t";

  /**
   * Encode a string
   *
   * @param xmlString
   *          string to be encoded
   * @return encoded string
   */
  public static String encode(String xmlString) {
    if (xmlString == null) {
      return "";
    }
    char[] characters = xmlString.toCharArray();
    StringBuilder out = null;
    char character;

    for (int i = 0; i < characters.length; i++) {
      character = characters[i];
      switch (character) {
      // we don't care about single quotes since axis will
      // use double quotes anyway
      case '&':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(AMP);
        break;
      case '"':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(QUOTE);
        break;
      case '<':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(LESS);
        break;
      case '>':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(GREATER);
        break;
      case '\n':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(LF);
        break;
      case '\r':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(CR);
        break;
      case '\t':
        if (out == null) {
          out = getInitialByteArray(xmlString, i);
        }
        out.append(TAB);
        break;
      default:
        if (character < 0x20) {
          throw new IllegalArgumentException("Invalid XML Character "
              + Integer.toHexString(character) + " in " + xmlString);
        } else {
          if (out != null) {
            out.append(character);
          }
        }
        break;
      }
    }
    if (out == null) {
      return xmlString;
    }
    return out.toString();
  }

  protected static StringBuilder getInitialByteArray(String aXmlString, int pos) {
    return new StringBuilder(aXmlString.substring(0, pos));
  }

}
