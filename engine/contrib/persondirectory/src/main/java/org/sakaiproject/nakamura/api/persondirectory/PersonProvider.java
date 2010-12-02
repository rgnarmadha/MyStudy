/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.api.persondirectory;

import java.util.Map;

import javax.jcr.Node;

/**
 * Provider interface for looking up a person and attributes associated to them.
 */
public interface PersonProvider {

  /**
   * Get a particular section of attributes for a person.
   * 
   * @param parameters
   *          The section node being accessed.
   * @return A {@link Map} with all found associated attributes. null if the UID is not
   *         found.
   */
  Map<String, Object> getProfileSection(Node parameters) throws PersonProviderException;
}
