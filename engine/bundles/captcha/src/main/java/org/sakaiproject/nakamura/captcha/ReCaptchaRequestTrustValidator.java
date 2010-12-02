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
package org.sakaiproject.nakamura.captcha;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.api.captcha.CaptchaService;

import javax.servlet.http.HttpServletRequest;

@Service
@Component
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Validates a request with the reCAPTCHA.net service."),
    @Property(name = RequestTrustValidator.VALIDATOR_NAME, value = "reCAPTCHA.net") })
public class ReCaptchaRequestTrustValidator implements RequestTrustValidator {

  @Reference
  protected transient CaptchaService captchaService;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator#getLevel()
   */
  public int getLevel() {
    return RequestTrustValidator.CREATE_USER;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator#isTrusted(javax.servlet.http.HttpServletRequest)
   */
  public boolean isTrusted(HttpServletRequest request) {
    return captchaService.checkRequest(request);
  }

}
