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
package org.sakaiproject.nakamura.site.create;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Value;

/**
 *
 */
public class GroupToCreate {

  private Principal principal;
  private Value[] managers;
  private Value[] viewers;
  private Map<String, Object> properties;

  /**
   * @return the principal
   */
  public Principal getPrincipal() {
    return principal;
  }

  /**
   * @return the managers
   */
  public Value[] getManagers() {
    return managers;
  }

  /**
   * @return the viewers
   */
  public Value[] getViewers() {
    return viewers;
  }

  /**
   * @return the properties
   */
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
   * @param principal
   *          the principal to set
   */
  public void setPrincipal(Principal principal) {
    this.principal = principal;
  }

  /**
   * @param managers
   *          the managers to set
   */
  public void setManagers(Value[] managers) {
    this.managers = managers;
  }

  /**
   * @param viewers
   *          the viewers to set
   */
  public void setViewers(Value[] viewers) {
    this.viewers = viewers;
  }

  /**
   * @param properties
   *          the properties to set
   */
  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

}
