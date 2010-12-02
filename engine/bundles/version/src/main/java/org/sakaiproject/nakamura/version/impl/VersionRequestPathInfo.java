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
package org.sakaiproject.nakamura.version.impl;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestPathInfo;
import org.sakaiproject.nakamura.util.StringUtils;

/**
 * 
 */
public class VersionRequestPathInfo implements RequestPathInfo {

  private final String selectorString;

  private final String[] selectors;

  private final String extension;

  private final String suffix;

  private final String resourcePath;

  private final static String[] NO_SELECTORS = new String[0];

  public VersionRequestPathInfo(RequestPathInfo sourceRequestPathInfo) {

    resourcePath = sourceRequestPathInfo.getResourcePath();
    suffix = sourceRequestPathInfo.getSuffix();

    selectorString = getSelectorString(sourceRequestPathInfo.getSelectorString(), sourceRequestPathInfo.getExtension());
    extension = getExtension(sourceRequestPathInfo.getSelectorString(), sourceRequestPathInfo.getExtension());



    selectors = StringUtils.split(selectorString, '.');

  }

  private static String getExtension(String selector, String extension) {
    String suffix = removeVersionName(selector, extension);
    if ( suffix == null ) {
      return null;
    }
    int i = suffix.lastIndexOf('.');
    if (i > 0) {
      return suffix.substring(i+1);
    } else {
      return suffix;
    }
  }

  private String getSelectorString(String selector, String extension) {
    String suffix = removeVersionName(selector, extension);
    if ( suffix == null ) {
      return null;
    }
    int i = suffix.lastIndexOf('.');
    if (i > 0) {
      return suffix.substring(0, i);
    } else {
      return null;
    }
  }

  private VersionRequestPathInfo(String resourcePath, String selectorString,
      String extension, String suffix) {
    this.resourcePath = resourcePath;
    this.selectorString = selectorString;
    this.selectors = (selectorString != null) ? selectorString.split("\\.")
        : NO_SELECTORS;
    this.extension = extension;
    this.suffix = suffix;
  }

  public VersionRequestPathInfo merge(RequestPathInfo baseInfo) {
    if (getExtension() == null) {
      return new VersionRequestPathInfo(getResourcePath(), baseInfo.getSelectorString(),
          baseInfo.getExtension(), baseInfo.getSuffix());
    }

    return this;
  }

  public VersionRequestPathInfo merge(RequestDispatcherOptions options) {

    if (options != null) {

      // set to true if any option is set
      boolean needCreate = false;

      // replacement selectors
      String selectors = options.getReplaceSelectors();
      if (selectors != null) {
        needCreate = true;
      } else {
        selectors = getSelectorString();
      }

      // additional selectors
      String selectorsAdd = options.getAddSelectors();
      if (selectorsAdd != null) {
        if (selectors != null) {
          selectors += "." + selectorsAdd;
        } else {
          selectors = selectorsAdd;
        }
        needCreate = true;
      }

      // suffix replacement
      String suffix = options.getReplaceSuffix();
      if (suffix != null) {
        needCreate = true;
      } else {
        suffix = getSuffix();
      }

      if (needCreate) {
        return new VersionRequestPathInfo(getResourcePath(), selectors, getExtension(),
            suffix);
      }
    }

    return this;
  }

  @Override
  public String toString() {
    return "VersonRequestPathInfo: path='" + resourcePath + "'" + ", selectorString='"
        + selectorString + "'" + ", extension='" + extension + "'" + ", suffix='"
        + suffix + "'";
  }

  public String getExtension() {
    return extension;
  }

  @SuppressWarnings(justification="Although its possible to modify this array, its internal to this bundle and we dont ", value={"EI_EXPOSE_REP"})
  public String[] getSelectors() {
    return selectors;
  }

  public String getSelectorString() {
    return selectorString;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getResourcePath() {
    return resourcePath;
  }

  /**
   * @param suffix
   * @return
   */
  protected static String removeVersionName(String selector, String extension) {
    String suffix = "";
    if ( selector != null ) {
      suffix = selector;
    }
    if ( extension != null ) {
      suffix = suffix + "." + extension;
    }

    if (suffix != null && suffix.startsWith("version.")) {
      char[] sc = suffix.toCharArray();
      int i = "version.".length();
      if (sc[i] == '.') {
        i++;
      }
      if (sc[i] == ',') {
        i++;
        while (i < sc.length && sc[i] != ',') {
          i++;
        }
      }
      while (i < sc.length && sc[i] != '.') {
        i++;
      }
      // i is now pointing to the start of the new suffix.
      i++;
      if (i >= suffix.length()) {
        return null;
      } else {
        return suffix.substring(i);
      }
    }
    return suffix;
  }

  /**
   * @param suffix
   * @return
   */
  protected static String getVersionName(String selector, String extension) {
    String suffix = "";
    if ( selector != null ) {
      suffix = selector;
    }
    if ( extension != null ) {
      suffix = suffix + "." + extension;
    }

    if (suffix.startsWith("version.")) {
      char[] ca = suffix.toCharArray();
      int i = "version.".length();
      int j = i;
      if (i < ca.length && ca[i] == '.') {
        i++;
      }

      if (i < ca.length && ca[i] == ',') {
        i++;
        j = i;
        while (i < ca.length && ca[i] != ',')
          i++;
      } else {
        j = i;
        while (i < ca.length && ca[i] != '.')
          i++;
      }
      return new String(ca, j, i - j);
    }
    return null;
  }

}
