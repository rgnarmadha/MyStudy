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
package org.sakaiproject.nakamura.api.files;

public interface FilesConstants {
  /**
   * The resource type for a sakai link. sakai/link
   */
  public static final String RT_SAKAI_LINK = "sakai/link";
  /**
   * The resource type for a sakai tag. sakai/tag
   */
  public static final String RT_SAKAI_TAG = "sakai/tag";
  /**
   * The path to the filestore for users.
   */
  public static final String USER_FILESTORE = "/_user/files";

  /**
   * sakai:tags
   */
  public static final String SAKAI_TAGS = "sakai:tags";
  /**
   * sakai:tag-uuid
   */
  public static final String SAKAI_TAG_UUIDS = "sakai:tag-uuid";
  /**
   * sakai:tag-name - Intended to identify the name of a tag
   */
  public static final String SAKAI_TAG_NAME = "sakai:tag-name";

  /**
   * sakai:file
   */
  public static final String SAKAI_FILE = "sakai:file";
  /**
   * sakai:link
   */
  public static final String SAKAI_LINK = "sakai:link";
  /**
   * The mixin required on the node to do a tag.
   */
  public static final String REQUIRED_MIXIN = "sakai:propertiesmix";
  /**
   * FileHandlerProcessor
   */
  public static final String LINK_HANDLER = "LinkHandler";

  public static final String REG_PROCESSOR_NAMES = "sakai.files.handler";

  /**
   * The OSGi topic for tagging a file.
   */
  public static final String TOPIC_FILES_TAG = "org/sakaiproject/nakamura/files/tag";

  /**
   * The OSGi topic for linking a file.
   */
  public static final String TOPIC_FILES_LINK = "org/sakaiproject/nakamura/files/link";

  /**
   * The sling:resourceType for pooled content.
   */
  public static final String POOLED_CONTENT_RT = "sakai/pooled-content";
  /**
   * The jcr:primaryType for pooled content.
   */
  public static final String POOLED_CONTENT_NT = "sakai:pooled-content";
  /**
   * The jcr:primaryType for the pooled content members nodetype.
   */
  public static final String POOLED_CONTENT_USER_RT = "sakai/pooled-content-user";

  /**
   * The name of the node that contains the viewers and and managers of the pooled content.
   */
  public static final String POOLED_CONTENT_MEMBERS_NODE = "/members";
  /**
   * The name for the managers property on the pooled content members node.
   */
  public static final String POOLED_CONTENT_USER_MANAGER = "sakai:pooled-content-manager";
  /**
   * The name for the viewers property on the pooled content members node.
   */
  public static final String POOLED_CONTENT_USER_VIEWER = "sakai:pooled-content-viewer";
  /**
   * The name of the property that holds the filename.
   */
  public static final String POOLED_CONTENT_FILENAME = "sakai:pooled-content-file-name";

  /**
   * Property on the file node indicating who the content was created for.
   */
  public static final String POOLED_CONTENT_CREATED_FOR = "sakai:pool-content-created-for";

}
