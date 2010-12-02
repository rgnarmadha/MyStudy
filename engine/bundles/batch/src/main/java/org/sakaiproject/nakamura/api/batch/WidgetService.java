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
package org.sakaiproject.nakamura.api.batch;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * An interface to fetch all/some of the widgets.
 */
public interface WidgetService {

  /**
   * @return Get tje list of widget folders that are defined in the admin console.
   */
  public List<String> getWidgetFolders();

  /**
   * @param resolver
   *          A {@link ResourceResolver} that can be used to retrieve widgets.
   * @return A Map of all the widget configs. The key represents the widgetname.
   */
  public Map<String, ValueMap> getWidgetConfigs(ResourceResolver resolver);

  /**
   * Bundles all of the widget files and the required locale.
   * 
   * @param path
   *          The path to the widget.
   * @param locale
   *          The locale that should be outputted. If this is null, the system's default
   *          locale will be used.
   * @param resolver
   *          A ResourceResolver that can be used to retrieve all the widget files.
   * @return A Map that will contain the output for a widget.
   */
  public ValueMap getWidget(String path, Locale locale, ResourceResolver resolver);

  /**
   * When a resource changes (either on disk or in JCR) this method should be called. This
   * will ensure that the cache is kept up to date.
   * 
   * @param path
   *          The path to the resource.
   */
  public void updateWidget(String path);

}
