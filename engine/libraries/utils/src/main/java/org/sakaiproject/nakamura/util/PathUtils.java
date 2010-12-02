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

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.sakaiproject.nakamura.api.resource.SubPathProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Generate a path prefix based on the user id.
 * 
 */
public class PathUtils {

  /**
   *
   */
  private static final Logger logger = LoggerFactory.getLogger(PathUtils.class);

  /**
   * Generate a path using a SHA-1 hash split into path parts to generate a unique path to
   * the user information, that will not result in too many objects in each folder.
   * 
   * @param user
   *          the user for which the path will be generated.
   * @return a structured path fragment for the user.
   */
  @Deprecated
  public static String getUserPrefix(String user, int levels) {
    if (user != null) {
      if (user.length() == 0) {
        user = "anon";
      }
      return getStructuredHash(user, levels, false);
    }
    return null;
  }

  /**
   * Get the prefix for a message.
   * 
   * @return Prefix used to store a message. Defaults to a yyyy/mm/dd structure.
   * @see java.text.SimpleDateFormat for pattern definitions.
   */
  @Deprecated
  public static String getMessagePath() {
    Calendar c = Calendar.getInstance();
    String prefix = "/" + c.get(Calendar.YEAR) + "/" + c.get(Calendar.MONTH) + "/";
    return prefix;
  }

  /**
   * @param target
   *          the target being formed into a structured path.
   * @param b
   * @return the structured path.
   */
  private static String getStructuredHash(String target, int levels, boolean absPath) {
    try {
      // take the first element as the key for the target so that subtrees end up in the
      // same place.
      String[] elements = StringUtils.split(target, '/', 1);
      String pathInfo = removeFirstElement(target);
      target = elements[0];

      target = String.valueOf(target);
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] userHash = md.digest(target.getBytes("UTF-8"));

      char[] chars = new char[(absPath ? 1 : 0) + levels * 3 + target.length()
          + pathInfo.length()];
      int j = 0;
      if (absPath) {
        chars[j++] = '/';
      }
      for (int i = 0; i < levels; i++) {
        byte current = userHash[i];
        int hi = (current & 0xF0) >> 4;
        int lo = current & 0x0F;
        chars[j++] = (char) (hi < 10 ? ('0' + hi) : ('a' + hi - 10));
        chars[j++] = (char) (lo < 10 ? ('0' + lo) : ('a' + lo - 10));
        chars[j++] = '/';
      }
      for (int i = 0; i < target.length(); i++) {
        char c = target.charAt(i);
        if (!Character.isLetterOrDigit(c)) {
          c = '_';
        }
        chars[j++] = c;

      }
      for (int i = 0; i < pathInfo.length(); i++) {
        chars[j++] = pathInfo.charAt(i);
      }
      return new String(chars);
    } catch (NoSuchAlgorithmException e) {
      logger.error(e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Return the path of the parent node.
   *
   * @param resourceReference
   *          A string that represents a path in JCR (can end with /)
   * @return If given "/foo/bar///" will return /foo.
   */
  public static String getParentReference(String resourceReference) {
    char[] ref = resourceReference.toCharArray();
    int i = ref.length - 1;
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    while (i >= 0 && ref[i] != '/') {
      i--;
    }
    while (i >= 0 && ref[i] == '/') {
      i--;
    }
    if (i == -1) {
      return "/";
    }
    return new String(ref, 0, i + 1);
  }

  /**
   * @param path
   *          the original path.
   * @return a pooled hash of the filename
   */
  @Deprecated
  public static String getDatePath(String path, int levels) {
    String hash = getStructuredHash(path, levels, true);
    Calendar c = Calendar.getInstance();
    StringBuilder sb = new StringBuilder();
    sb.append("/").append(c.get(Calendar.YEAR)).append("/").append(c.get(Calendar.MONTH))
        .append(hash);
    return sb.toString();
  }

  /**
   * @param path
   *          the original path.
   * @return a pooled hash of the filename
   */
  public static String getShardPath(String path, int levels) {
    return getStructuredHash(path, levels, true);
  }

  /**
   * Normalizes the input path to an absolute path prepending / and ensuring that the path
   * does not end in /.
   * 
   * @param pathFragment
   *          the path.
   * @return a normalized path.
   */
  public static String normalizePath(String pathFragment) {
    if ( pathFragment == null ) {
      return "";
    }
    char[] source = pathFragment.toCharArray();
    char[] normalized = new char[source.length + 1];
    int i = 0;
    int j = 0;
    if (source.length == 0 || source[i] != '/') {
      normalized[j++] = '/';
    }
    boolean slash = false;
    for (; i < source.length; i++) {
      char c = source[i];
      switch (c) {
      case '/':
        if (!slash) {
          normalized[j++] = c;
        }
        slash = true;
        break;
      default:
        slash = false;
        normalized[j++] = c;
        break;
      }
    }
    if (j > 1 && normalized[j - 1] == '/') {
      j--;
    }
    return new String(normalized, 0, j);
  }

  /**
   * Removes the first element of the path
   * 
   * @param path
   *          the path
   * @return the path with the first element removed.
   */
  public static String removeFirstElement(String path) {
    if (path == null || path.length() == 0) {
      return path;
    }
    char[] p = path.toCharArray();
    int i = 0;
    while (i < p.length && p[i] == '/') {
      i++;
    }
    while (i < p.length && p[i] != '/') {
      i++;
    }
    if (i < p.length) {
      return new String(p, i, p.length - i);
    }
    return "/";
  }

  /**
   * Remove the last path element.
   * 
   * @param path
   *          the path
   * @return the path with the last element removed.
   */
  public static String removeLastElement(String path) {
    if (path == null || path.length() == 0) {
      return path;
    }
    char[] p = path.toCharArray();
    int i = p.length - 1;
    while (i >= 0 && p[i] == '/') {
      i--;
    }
    while (i >= 0 && p[i] != '/') {
      i--;
    }
    if (i > 0) {
      return new String(p, 0, i);
    }
    return "/";

  }

  /**
   * Parses the path into all the parts before the first . in the last path element, and
   * everything after the first . in the last element.
   * 
   * @param relativePath
   * @return
   */
  public static String[] getNodePathParts(String relativePath) {
    char[] c = relativePath.toCharArray();
    int dot = -1;
    for (int i = 0; i < c.length; i++) {
      if (c[i] == '/') {
        dot = -1;
      } else if (c[i] == '.' && dot == -1) {
        dot = i;
      }
    }
    if (dot < 0) {
      return new String[] {relativePath, ""};
    }
    return new String[] {new String(c, 0, dot), new String(c, dot, c.length - dot)};
  }

  /**
   * @param servletPath
   * @param pathInfo
   * @return
   */
  public static String toInternalShardPath(String servletPath, String pathInfo,
      String selector) {
    return PathUtils.normalizePath(servletPath + PathUtils.getShardPath(pathInfo, 4)
        + selector);
  }

  /**
   * @param dest
   * @return
   */
  public static String lastElement(String dest) {
    int i = dest.lastIndexOf('/');
    if ( i == dest.length()-1 ) {
      return "";
    }
    if (i > -1) {
      dest = dest.substring(i+1);
    }
    i = dest.indexOf('.');
    if (i > -1) {
      dest = dest.substring(0, i);
    }
    return dest;
  }
  
  /**
   * Returns a suitable hash path that can be used to create full path's to locations.
   * 
   * @param o
   *          An object that can be adapted to something where a path can get extracted
   *          from. Currently supported: {@link Authorizable}, {@link ItemBasedPrincipal}
   *          and {@link SubPathProducer}
   * @return
   */
  public static String getSubPath(Object o) {
    String sub = null;
    if (o instanceof Authorizable) {
      try {
        Authorizable au = (Authorizable) o;
        Principal p = au.getPrincipal();
        if ( au.hasProperty("path") ) {
          sub = au.getProperty("path")[0].getString();
        } else if ( p instanceof ItemBasedPrincipal ) {
          String path = ((ItemBasedPrincipal) p).getPath();
          int i = path.lastIndexOf("rep:");
          i = path.indexOf('/',i+1);
          sub = path.substring(i);
        } else {
          sub = "/"+au.getID();          
        }
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    } else if (o instanceof ItemBasedPrincipal) {
      try {
        String path = ((ItemBasedPrincipal) o).getPath();
        int i = path.lastIndexOf("rep:");
        i = path.indexOf('/',i+1);
        sub = path.substring(i);
      } catch (RepositoryException e) {
        throw new RuntimeException(e);
      }
    } else if (o instanceof Principal) {
      sub = "/"+((Principal) o).getName();
    } else if (o instanceof SubPathProducer) {
      sub = ((SubPathProducer) o).getSubPath();
    } 
    return PathUtils.normalizePath(sub);
  }

  /**
   * @param messagePathBase
   * @param messageId
   * @param string
   * @return
   */
  public static String toSimpleShardPath(String pathBase, String messageId,
      String pathEnd) {
    char[] shard = "________".toCharArray();
    for( int i = 0; i < messageId.length() && i < shard.length; i++ ) {
      shard[i] = messageId.charAt(i);
    }
    StringBuilder sb = new StringBuilder();
    sb.append(pathBase);
    for ( int i = 0; i < 4; i++ ) {
      sb.append("/").append(new String(shard,i*2,2));
    }
    sb.append("/").append(messageId);
    sb.append(pathEnd);
    return sb.toString();
  }

  public static Object translateAuthorizablePath(Object value) {
    String s = String.valueOf(value);
    if (s != null && s.length() > 4) {
      if (s.charAt(0) == '/' && s.charAt(1) == '_') {
        String id = null;
        if (s.startsWith("/_user/") || s.startsWith("/_group/")) {
          int slash = s.indexOf('/', 2);
          while (slash > 0) {
            int nslash = s.indexOf('/', slash + 1);
            String nid = null;
            if (nslash > 0) {
              nid = s.substring(slash + 1, nslash);
            } else {
              nid = s.substring(slash + 1);
            }
            if (id == null) {
              id = nid;
            } else if (nid.equals(id)) {
              // a quirk of Jackrabbit: a 3-character id has an additional intermediate directory
              // /~ieb equals /_user/i/ie/ieb/ieb
              if (id.length() > 3) {
                return "/~" + id + "/"+ s.substring(slash + 1);
              } else {
                return "/~" + id + s.substring(slash + 1 + id.length());
              }
            } else if (!nid.startsWith(id)) {
              return "/~" + id+ "/" + s.substring(slash + 1);
            }
            slash = nslash;
            id = nid;
          }
          if ( id != null && id.length() > 0) {
            return "/~" + id;
          }
        }
      }
    }
    return value;
  }
}
