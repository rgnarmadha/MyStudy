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
package org.sakaiproject.nakamura.antixss;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.sakaiproject.nakamura.api.antixss.AntiXssService;
import org.sakaiproject.nakamura.util.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

/**
 *
 */
@Component(description="Provides Anti XSS, anti Spam services")
@Service(value=AntiXssService.class)
public class AntiXssServiceImpl implements AntiXssService {

  
  @Property(value = "res://org/sakaiproject/nakamura/antixss/defaultpolicy.xml")
  protected static final String POLICY_FILE_LOCATION = "sakai.antixss.policyurl";
  private AntiSamy antiSammy;
  private Policy policy;

  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext componentContext) throws IOException, PolicyException {
    Dictionary<String, Object> config = componentContext.getProperties();
    String policyUrl = (String) config.get(POLICY_FILE_LOCATION);
    InputStream in = ResourceLoader.openResource(policyUrl, this.getClass().getClassLoader());
    policy = Policy.getInstance(in);
    in.close();
    antiSammy = new AntiSamy();
  }
  
  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.antixss.AntiXssService#cleanHtml(java.lang.String)
   */
  public String cleanHtml(String value) {
    try {
      CleanResults cr = antiSammy.scan(value, policy);
      return cr.getCleanHTML();
    } catch (ScanException e) {
      return "filtered: "+e.getMessage();
    } catch (PolicyException e) {
      return "filtered: "+e.getMessage();
    }
  }
  
}
