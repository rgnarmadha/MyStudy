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
package org.sakaiproject.nakamura.api.docproxy;

/**
 *
 */
public interface DocProxyConstants {

  /**
   * The resource type that defines an external repository node.
   * 
   * sakai/external-repository
   */
  public static final String RT_EXTERNAL_REPOSITORY = "sakai/external-repository";

  /**
   * The resource type that defines an external repository document node.
   * 
   * sakai/external-repository-document
   */
  public static final String RT_EXTERNAL_REPOSITORY_DOCUMENT = "sakai/external-repository-document";

  /**
   * The property on a node that identifies which processor should be used to interact
   * with the external repository.
   * 
   * sakai:repository-processor
   */
  public static final String REPOSITORY_PROCESSOR = "sakai:repository-processor";

  public static final String EXTERNAL_ID = "sakai:external-id";

  public static final String REPOSITORY_LOCATION = "sakai:repository-location";

  public static final String REPOSITORY_SEARCH_PROPS = "sakai:repository-search-properties";

  public static final String REPOSITORY_REF = "sakai:repository-ref";

}
