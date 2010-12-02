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
package org.sakaiproject.nakamura.batch;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public abstract class AbstractWidgetServletTest {

  @Mock
  protected ResourceResolver resolver;
  @Mock
  protected SlingHttpServletResponse response;
  @Mock
  protected SlingHttpServletRequest request;
  @Mock
  protected CacheManagerService cacheManagerService;

  protected String path;
  protected StringWriter stringWriter;
  protected PrintWriter printWriter;
  protected WidgetServiceImpl widgetService;

  public void setUp() throws IOException {
    // Init mocks
    MockitoAnnotations.initMocks(this);

    Map<String, String[]> properties = new HashMap<String, String[]>();
    properties.put(WidgetServiceImpl.WIDGET_IGNORE_NAMES, new String[] { "bundles" });
    properties.put(WidgetServiceImpl.WIDGET_VALID_MIMETYPES, new String[] { "text/plain",
        "text/css", "text/html", "application/json", "application/xml" });
    properties.put(WidgetServiceImpl.WIDGET_FOLDERS, new String[] { "/widgets" });

    widgetService = new WidgetServiceImpl();
    widgetService.cacheManagerService = cacheManagerService;
    widgetService.activate(properties);

    when(request.getResourceResolver()).thenReturn(resolver);

    // For test cases we will always assume that a locale of nl_NL is request.
    RequestParameter localeParam = mock(RequestParameter.class);
    when(localeParam.getString()).thenReturn("nl_NL");
    when(request.getRequestParameter("locale")).thenReturn(localeParam);

    // Mock the response print writer.
    stringWriter = new StringWriter();
    printWriter = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(printWriter);

    // Mock all the test resources as "Sling Resources".
    File file = new File(getClass().getResource("/widgets").getPath());
    mockResource("/widgets", file);
  }

  /**
   *
   */
  private Resource mockResource(String path, File file) {
    // Get the inputstream for this file (null if directory.)
    InputStream in = getStream(file);

    // Mock the resource
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(InputStream.class)).thenReturn(in);
    when(resource.adaptTo(File.class)).thenReturn(file);
    when(resource.getResourceResolver()).thenReturn(resolver);
    when(resource.getPath()).thenReturn(path);

    // Add the resource to the resource resolver.
    when(resolver.getResource(path)).thenReturn(resource);

    // Mock all the children
    List<Resource> resources = mockFileChildren(path, file);
    when(resolver.listChildren(resource)).thenReturn(resources.iterator());

    // If not using ResourceUtil
    when(resource.listChildren()).thenReturn(resources.iterator());
    when(resource.getName()).thenReturn(file.getName());
    return resource;
  }

  /**
   * @param file
   */
  private List<Resource> mockFileChildren(String path, File file) {
    List<Resource> resources = new ArrayList<Resource>();
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      for (File child : children) {
        String childPath = child.getName();
        if (!path.equals("/")) {
          childPath = "/" + childPath;
        }
        Resource resource = mockResource(path + childPath, child);
        resources.add(resource);
      }
    }
    return resources;
  }

  /**
   * @param file
   * @return
   */
  protected InputStream getStream(File file) {
    if (!file.isDirectory() && file.canRead()) {
      try {
        return new FileInputStream(file);
      } catch (IOException ioe) {
        // Swallow it
      }
    }
    return null;
  }

}
