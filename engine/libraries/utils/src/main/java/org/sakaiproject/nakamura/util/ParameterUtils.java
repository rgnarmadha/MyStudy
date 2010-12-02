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

import org.sakaiproject.nakamura.util.parameters.ContainerRequestParameter;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;
import org.sakaiproject.nakamura.util.parameters.Util;

import java.util.Map;

/**
 *
 */
public class ParameterUtils {

  /**
   * Get a Sling parameter map
   * 
   * @param encoding
   *          The encoding that should be used. "ISO-8859-1" is adviced.
   * @param pMap
   *          The map of parameters that should be used.
   * @return A Sling parametermap
   */
  public static ParameterMap getContainerParameters(String encoding, Map<?, ?> pMap) {

    ParameterMap parameters = new ParameterMap();

    // Set the parameters.
    for (Map.Entry<?, ?> entry : pMap.entrySet()) {

      final String name = (String) entry.getKey();
      final String[] values = (String[]) entry.getValue();

      for (int i = 0; i < values.length; i++) {
        parameters.addParameter(name, new ContainerRequestParameter(values[i], encoding));
      }

    }

    // apply any form encoding (from '_charset_') in the parameter map
    Util.fixEncoding(parameters);

    return parameters;
  }
}
