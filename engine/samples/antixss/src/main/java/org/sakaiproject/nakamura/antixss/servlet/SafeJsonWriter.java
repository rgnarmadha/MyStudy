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
package org.sakaiproject.nakamura.antixss.servlet;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.antixss.AntiXssService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.Writer;

/**
 *
 */
public class SafeJsonWriter extends ExtendedJSONWriter {

  private AntiXssService antiXssService;

  /**
   * @param w
   */
  public SafeJsonWriter(AntiXssService antiXssService, Writer w) {
    super(w);
    this.antiXssService = antiXssService;
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.commons.json.io.JSONWriter#value(java.lang.Object)
   */
  @Override
  public JSONWriter value(Object val) throws JSONException {
    String v = JSONObject.valueToString(val);
    if ( v.charAt(0) == '"' && v.charAt(v.length()-1) == '"' ) {
      v = v.substring(1, v.length()-2);
    }
    return super.value(antiXssService.cleanHtml(v));
  }
  
  
  
  
  

}
