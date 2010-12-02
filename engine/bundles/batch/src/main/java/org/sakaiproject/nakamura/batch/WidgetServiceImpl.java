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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.sakaiproject.nakamura.api.batch.WidgetService;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service()
@Component(immediate = true, metatype = true)
public class WidgetServiceImpl implements WidgetService {

  @Property(value = { "bundles" }, description = "The directorynames that should be ignored when outputting a widget.", cardinality = 2147483647)
  static final String WIDGET_IGNORE_NAMES = "sakai.batch.widgetize.ignore_names";

  @Property(value = { "text/plain", "text/css", "text/html", "application/json",
      "application/xml" }, description = "The mimetypes of files that should be outputted.", cardinality = 2147483647)
  static final String WIDGET_VALID_MIMETYPES = "sakai.batch.widgetize.valid_mimetypes";

  @Property(value = { "/devwidgets" }, cardinality = 2147483647, description = "The directorynames that contain widgets. These have to be absolute paths in JCR.")
  static final String WIDGET_FOLDERS = "sakai.batch.widgets.widget_folders";
  private List<String> widgetFolders;

  @Reference
  protected transient CacheManagerService cacheManagerService;

  /**
   * The name for the cache that holds all the HTML, CSS, .. files for widgets
   */
  static final String CACHE_NAME_WIDGET_FILES = WidgetServiceImpl.class.getName()
      + "_files";

  /**
   * The name for the cache that holds all widget config files.
   */
  static final String CACHE_NAME_WIDGET_CONFIGS = WidgetServiceImpl.class.getName()
      + "_configs";

  private static final Logger LOGGER = LoggerFactory.getLogger(WidgetServiceImpl.class);

  private List<String> skipDirectories;
  private List<String> validMimetypes;
  private Detector detector;

  @SuppressWarnings("rawtypes")
  @Activate
  protected void activate(Map properties) {
    AutoDetectParser parser = new AutoDetectParser();
    detector = parser.getDetector();

    modified(properties);
  }

  @SuppressWarnings("rawtypes")
  @Modified
  protected void modified(Map properties) {
    init(properties);
  }

  @SuppressWarnings("rawtypes")
  private void init(Map props) {
    String[] names = OsgiUtil
        .toStringArray(props.get(WIDGET_IGNORE_NAMES), new String[0]);
    String[] types = OsgiUtil.toStringArray(props.get(WIDGET_VALID_MIMETYPES),
        new String[0]);
    String[] folders = OsgiUtil.toStringArray(props.get(WIDGET_FOLDERS), new String[0]);

    skipDirectories = Arrays.asList(names);
    validMimetypes = Arrays.asList(types);
    widgetFolders = Arrays.asList(folders);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.batch.WidgetService#getWidget(java.lang.String,
   *      java.util.Locale, org.apache.sling.api.resource.ResourceResolver)
   */
  public ValueMap getWidget(String path, Locale locale, ResourceResolver resolver) {
    if (path == null) {
      throw new IllegalArgumentException("The path cannot be null.");
    }
    if (resolver == null) {
      throw new IllegalArgumentException("The resource resolver cannot be null.");
    }
    if (locale == null) {
      locale = Locale.getDefault();
    }

    // Get the resource that should represent this widget.
    // We use resources rather than Nodes because most of the time the UI will use the
    // FsResource tool for development.
    Resource resource = resolver.getResource(path);

    // Make sure that this is a proper widget.
    if (!checkValidWidget(resource)) {
      throw new IllegalArgumentException(
          "The provided path does not point to a valid widget.");
    }

    // Check if we have something in the cache.
    String widgetName = resource.getName();
    Cache<Map<String, ValueMap>> cache = cacheManagerService.getCache(
        CACHE_NAME_WIDGET_FILES, CacheScope.INSTANCE);

    Map<String, ValueMap> widgetCache = cache.get(widgetName);
    if (widgetCache == null) {
      widgetCache = new HashMap<String, ValueMap>();
    } else {
      ValueMap map = widgetCache.get(locale.toString());
      if (map != null) {
        // The locale for this widget is in the cache.
        // We can just return it.
        return map;
      }
    }

    try {
      // There is nothing in the cache, create it and put it in there.
      StringWriter sw = new StringWriter();
      ExtendedJSONWriter writer = new ExtendedJSONWriter(sw);
      writer.object();
      outputWidget(resource, writer, locale);
      writer.endObject();
      sw.flush();
      String content = sw.toString();
      ValueMap map = new JsonValueMap(content);

      // Put the map in the cache.
      widgetCache.put(locale.toString(), map);
      cache.put(widgetName, widgetCache);

      return map;
    } catch (JSONException e) {
      throw new RuntimeException("Could not parse this widget to JSON.");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.batch.WidgetService#getWidgetConfigs(org.apache.sling.api.resource.ResourceResolver)
   */
  public Map<String, ValueMap> getWidgetConfigs(ResourceResolver resolver) {
    // Check the cache to see if we have anything cached already.
    Cache<Map<String, ValueMap>> cache = cacheManagerService.getCache(
        CACHE_NAME_WIDGET_CONFIGS, CacheScope.INSTANCE);
    Map<String, ValueMap> configs = cache.get("configs");
    if (configs != null) {
      // There is something in here, return it.
      return configs;
    }

    // We will store all the found widgets in this map.
    // The key will be the name of widget.
    Map<String, ValueMap> validWidgets = new HashMap<String, ValueMap>();
    for (String folder : widgetFolders) {
      processWidgetFolder(folder, resolver, validWidgets);
    }
    // Stick the map in the cache so it can be retrieved later on.
    cache.put("configs", validWidgets);
    return validWidgets;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.batch.WidgetService#getWidgetFolders()
   */
  public List<String> getWidgetFolders() {
    return widgetFolders;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.batch.WidgetService#updateWidget(java.lang.String)
   */
  public void updateWidget(String path) {
    LOGGER.debug("Update widget at: " + path);

    // Invalidate the files cache.
    // Find the name of the widget.
    String widget = null;
    for (String folder : getWidgetFolders()) {
      if (!path.equals(folder) && path.startsWith(folder)) {
        widget = path.substring(folder.length() + 1);
        int lastIndex = widget.indexOf("/");
        if (lastIndex != -1) {
          widget = widget.substring(0, lastIndex);
        }
        break;
      }
    }
    if (widget != null) {
      // Get the cache for this widget.
      Cache<Map<String, ValueMap>> cache = cacheManagerService.getCache(
          CACHE_NAME_WIDGET_FILES, CacheScope.INSTANCE);

      if (cache != null) {
        // Remove it from the cache.
        // When it get's hit the next time, the servlet will ask for it and it will be
        // placed back in the cache then.
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Invalidating cache for '" + widget + "'");
        }
        cache.remove(widget);
      }
    }

    // Invalidate the configs cache.
    Cache<Map<String, ValueMap>> configCache = cacheManagerService.getCache(
        CACHE_NAME_WIDGET_CONFIGS, CacheScope.INSTANCE);
    configCache.clear();
  }

  // --- Implementation

  /**
   * @param resource
   * @param writer
   * @param b
   * @throws JSONException
   */
  protected void outputWidget(Resource resource, ExtendedJSONWriter writer, Locale locale)
      throws JSONException {

    // Output language bundles
    writer.key("bundles");
    writer.object();
    outputLanguageBundle(resource, writer, "default");
    outputLanguageBundle(resource, writer, locale.toString());
    writer.endObject();

    // Output widget files
    Iterator<Resource> children = resource.listChildren();
    while (children.hasNext()) {
      Resource child = children.next();
      String childName = child.getName();
      // Check if we can output this resource.
      if (skipDirectories.contains(childName)) {
        continue;
      }
      writer.key(childName);
      outputResource(child, writer, true);
    }

  }

  /**
   * @param resource
   * @param writer
   * @param string
   * @throws JSONException
   */
  protected void outputLanguageBundle(Resource resource, ExtendedJSONWriter writer,
      String bundle) throws JSONException {
    // Bundle files are located at
    // - </path/to/widget>/bundles/default.json
    // - </path/to/widget>/bundles/en_US.json

    String path = resource.getPath() + "/bundles/" + bundle + ".json";
    Resource bundleResource = resource.getResourceResolver().getResource(path);

    writer.key(bundle);
    if (bundleResource == null || bundleResource instanceof NonExistingResource) {
      // If no bundle is found we output an empty object.
      writer.object();
      writer.endObject();
    } else {
      getJsonResource(bundleResource, writer);
    }
  }

  /**
   * @param resource
   * @param writer
   * @throws JSONException
   */
  protected void getJsonResource(Resource resource, ExtendedJSONWriter writer)
      throws JSONException {
    String content = "";
    try {
      InputStream stream = resource.adaptTo(InputStream.class);

      content = IOUtils.readFully(stream, "UTF-8");
      writer.valueMap(new JsonValueMap(content));
    } catch (JSONException e) {
      writer.value(content);
    } catch (IOException e) {
      // If everything failed horribly we output an empty object.
      writer.object();
      writer.endObject();
    }
  }

  /**
   * @param resource
   * @param writer
   * @throws JSONException
   */
  protected void outputResource(Resource resource, ExtendedJSONWriter writer,
      boolean fetchChildren) throws JSONException {
    writer.object();

    InputStream stream = resource.adaptTo(InputStream.class);
    if (stream != null) {
      try {
        writer.key("content");

        // Get the mimetype of this stream
        BufferedInputStream bufStream = new BufferedInputStream(stream);
        Metadata metadata = new Metadata();
        MediaType type = detector.detect(bufStream, metadata);

        if (validMimetypes.contains(type.toString())) {

          // This Node (be it FsResource or not) is a valid file.
          // Output it.
          String content = IOUtils.readFully(bufStream, "UTF-8");
          if ("application/json".equals(type.toString())) {
            getJsonResource(resource, writer);
          } else {
            writer.value(content);
          }
        } else {
          writer.value(false);
        }
      } catch (IOException e) {
        writer.value(false);
      }

    } else {
      if (fetchChildren) {
        Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
          Resource child = children.next();
          String childName = child.getName();
          // Check if we can output this resource.
          if (skipDirectories.contains(childName)) {
            continue;
          }
          writer.key(childName);
          outputResource(child, writer, true);
        }
      }
    }

    writer.endObject();
  }

  /**
   * Checks if a resource is a valid widget. It checks the following things:
   * <ul>
   * <li>The resource is not null.</li>
   * <li>The resource lives under one of the pre-configured widget folders.</li>
   * <li>There is a configuration file called config.json under this widget.</li>
   * <li>The content of this file is proper json.</li>
   * </ul>
   * 
   * @param resource
   *          The resource that should resemble a widget.
   * @return Whether or not the resource is a valid Sakai widget.
   */
  protected boolean checkValidWidget(Resource resource) {
    // A null resource is obviously not a valid widget.
    if (resource == null || resource instanceof NonExistingResource) {
      return false;
    }

    // Check if this resource lives in one of the pre-configured widget folders.
    String resourcePath = resource.getPath();
    boolean found = false;
    for (String widgetFolder : widgetFolders) {
      if (resourcePath.startsWith(widgetFolder)) {
        found = true;
        break;
      }
    }
    if (!found) {
      // This resource lives somewhere else.
      // We're not handling this one.
      return false;
    }

    // Check for a configuration file under this widget.
    // If there is no config than it is not a valid widget.
    // http://confluence.sakaiproject.org/display/3AK/Sakai+3+Widget+Specification+Proposal
    ResourceResolver resolver = resource.getResourceResolver();
    String configPath = resource.getPath() + "/config.json";
    Resource configResource = resolver.getResource(configPath);
    if (configResource == null || configResource instanceof NonExistingResource) {
      return false;
    }

    try {
      // Get the content of this resource and parse it to json.
      StringWriter sw = new StringWriter();
      ExtendedJSONWriter jsonWriter = new ExtendedJSONWriter(sw);
      getJsonResource(configResource, jsonWriter);
    } catch (JSONException e) {
      // If this file cannot be parsed to JSON than it isn't valid.
      return false;
    }

    return true;
  }

  /**
   * Processes a widget folder. Every widget (with a valid json config) will be placed in
   * the validWidgets map.
   * 
   * @param folder
   *          The absolute path in JCR (or FsResource) to process.
   * @param resolver
   *          The {@link ResourceResolver} that can be used to resolve resources.
   * @param validWidgets
   *          The hashmap where the widgets should be placed in to.
   */
  protected void processWidgetFolder(String folder, ResourceResolver resolver,
      Map<String, ValueMap> validWidgets) {
    Resource folderResource = resolver.getResource(folder);

    if (folderResource != null && !(folderResource instanceof NonExistingResource)) {
      // List all the subfolders (these should all be widgets.)
      Iterator<Resource> widgets = folderResource.listChildren();
      while (widgets.hasNext()) {
        Resource widget = widgets.next();
        String widgetName = widget.getName();
        // Get the config for this widget.
        // If none is found or isn't valid JSON then it is ignored.
        String configPath = widget.getPath() + "/config.json";
        Resource config = resolver.getResource(configPath);
        if (config != null && !(config instanceof NonExistingResource)) {
          // Try to parse it to JSON.
          try {
            InputStream stream = config.adaptTo(InputStream.class);
            JsonValueMap map = new JsonValueMap(stream);
            validWidgets.put(widgetName, map);
          } catch (Exception e) {
            LOGGER.warn("Exception when trying to parse the 'config.json' for {}, Error Message {} ", widgetName, e.getMessage());
          }
        }
      }
    }
  }

}
