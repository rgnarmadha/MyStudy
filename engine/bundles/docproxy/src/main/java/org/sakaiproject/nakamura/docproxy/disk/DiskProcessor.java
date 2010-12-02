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
package org.sakaiproject.nakamura.docproxy.disk;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.EXTERNAL_ID;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_REF;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY_DOCUMENT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.util.IOUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * This is a proof of concept for the the External Repository processors.
 * 
 * This processor will write/read files to disk, DO NOT ENABLE THIS SERVICE ON A PUBLIC
 * OX!
 */
@Component(enabled = false, immediate = true, metatype = true)
@Service(value = ExternalRepositoryProcessor.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Proof-of-concept implementation of the Document Proxy API."),
    @Property(name = "service.note", value = "This processor should NOT be run in production. It is extremely likely this can be abused to hack in the system.") })
public class DiskProcessor implements ExternalRepositoryProcessor {

  protected static final String TYPE = "disk";
  protected static final Logger LOGGER = LoggerFactory.getLogger(DiskProcessor.class);

  @Property(name = "createJCRNodes", description = "Wether or not nodes should be created in JCR for newly uploaded files.", boolValue = false)
  protected boolean createJCRNodes = false;

  protected void activate(ComponentContext context) {
    @SuppressWarnings("rawtypes")
    Dictionary properties = context.getProperties();
    createJCRNodes = (Boolean) properties.get("createJCRNodes");
  }

  /**
   * {@inheritDoc}
   * 
   * @throws DocProxyException
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocument(javax.jcr.Node,
   *      java.lang.String)
   */
  public ExternalDocumentResult getDocument(Node node, String path)
      throws DocProxyException {
    File f = getFile(node, path);
    return new DiskDocumentResult(f);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getDocumentMetadata(javax.jcr.Node,
   *      java.lang.String)
   */
  public ExternalDocumentResultMetadata getDocumentMetadata(Node node, String path)
      throws DocProxyException {
    File f = getFile(node, path);
    return new DiskDocumentResult(f);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#getType()
   */
  public String getType() {
    return TYPE;
  }

  public Iterator<ExternalDocumentResult> search(Node node,
      Map<String, Object> searchProperties) throws DocProxyException {
    // We will search in the same directory (and subs) as the README dir.
    File defaultFile = getRootFile(node);

    String startWith = "";
    String endsWith = "";
    boolean matchStartName = false;
    boolean matchEndName = false;
    if (searchProperties != null) {
      if (searchProperties.get("starts-with") != null
          && !searchProperties.get("starts-with").equals("")) {
        startWith = searchProperties.get("starts-with").toString();
        matchStartName = true;
      }
      if (searchProperties.get("ends-with") != null
          && !searchProperties.get("ends-with").equals("")) {
        endsWith = searchProperties.get("ends-with").toString();
        matchEndName = true;
      }
    }

    final String start = startWith;
    final String end = endsWith;
    final boolean doStartWith = matchStartName;
    final boolean doEndsWith = matchEndName;

    // The filter we will be using,
    FilenameFilter filter = new FilenameFilter() {

      public boolean accept(File dir, String name) {
        boolean accept = true;
        // We don't want any files starting with a . (hidden files)
        if (name.startsWith(".")) {
          return false;
        }

        // We don't show our property files.
        if (name.endsWith(".json")) {
          return false;
        }

        // If our files should start with a certain string.
        if (doStartWith && !name.startsWith(start)) {
          accept = false;
        }
        if (doEndsWith && !name.endsWith(end)) {
          accept = false;
        }
        return accept;
      }
    };

    List<ExternalDocumentResult> results = new ArrayList<ExternalDocumentResult>();
    // Get children.
    getChildren(defaultFile, results, filter);
    return results.iterator();
  }

  /**
   * Iterate over all the children of a "file".
   * 
   * @param parentFile
   *          The file to list the children for.
   * @param results
   *          The list to add the results in
   * @param filter
   *          The filter to match the files against.
   */
  private void getChildren(File file, List<ExternalDocumentResult> results,
      FilenameFilter filter) {
    File[] files = file.listFiles();
    for (File f : files) {
      if (f.isDirectory()) {
        getChildren(f, results, filter);
      }
      if (filter.accept(f.getParentFile(), f.getName())) {
        results.add(new DiskDocumentResult(f));
      }
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor#updateDocument(javax.jcr.Node,
   *      java.lang.String, java.util.Map, java.io.InputStream, long)
   */
  public Map<String, Object> updateDocument(Node node, String path,
      Map<String, Object> properties, InputStream documentStream, long streamLength)
      throws DocProxyException {
    // Get the file for this node.
    File file = getFile(node, path);

    // Write the file stream
    if (documentStream != null) {
      File newFile = writeStreamToFile(documentStream, file);
      if (properties == null) {
        properties = new HashMap<String, Object>();
        properties.put(EXTERNAL_ID, newFile.toURI());
      }
    }

    // Write any properties
    if (properties != null) {
      try {
        // Write the json file
        File propertiesFile = getFile(node, path + ".json");

        // Retrieve previous properties
        DiskDocumentResult result = new DiskDocumentResult(file);
        JSONObject obj = new JSONObject(result.getProperties());

        // Write/Update new ones
        for (Entry<String, Object> entry : properties.entrySet()) {
          Object val = entry.getValue();
          if (val.getClass().isArray()) {
            String[] arr = (String[]) val;
            if (arr.length == 1) {
              obj.put(entry.getKey(), arr[0]);
            } else {
              JSONArray jsonArr = new JSONArray();
              for (String s : arr) {
                jsonArr.put(s);
              }
              obj.put(entry.getKey(), jsonArr);
            }
          } else {
            obj.put(entry.getKey(), val);
          }
        }
        String json = obj.toString();
        ByteArrayInputStream jsonStream = new ByteArrayInputStream(json.getBytes("UTF-8"));
        writeStreamToFile(jsonStream, propertiesFile);
      } catch (UnsupportedEncodingException e) {
        throw new DocProxyException(500, "Unable to save properties.");
      } catch (JSONException e) {
        throw new DocProxyException(500, "Unable to retrieve properties from request.");
      }

      // This implementation will also leave a node in JCR.
      // Note: This is optional.
      if (createJCRNodes) {
        updateJCRNode(node, path, properties);
      }
    }

    return properties;
  }

  /**
   * @param node
   * @param path
   * @param properties
   * @throws DocProxyException
   */
  private void updateJCRNode(Node node, String path, Map<String, Object> properties)
      throws DocProxyException {
    try {
      // construct that path to the JCR node.
      String base = node.getPath();
      if (!path.startsWith("/")) {
        base += "/";
      }
      base += path;

      Session session = node.getSession();
      Node documentNode = JcrUtils.deepGetOrCreateNode(session, base);
      //documentNode.addMixin("sakai:propertiesmix");
      // Set the reference to the external repository config node.
      documentNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY,
          RT_EXTERNAL_REPOSITORY_DOCUMENT);
      documentNode.setProperty(REPOSITORY_REF, node.getIdentifier());

      // Write all the properties on the node.
      ValueFactory vf = session.getValueFactory();
      Set<Entry<String, Object>> entries = properties.entrySet();
      for (Entry<String, Object> entry : entries) {
        String key = entry.getKey();
        Object val = entry.getValue();
        Value value = null;

        if (val instanceof Long) {
          long l = Long.parseLong(val.toString());
          value = vf.createValue(l);
        } else if (val instanceof Double) {
          double d = Double.parseDouble(val.toString());
          value = vf.createValue(d);
        } else if (val instanceof String) {
          value = vf.createValue(val.toString());
        } else if (val instanceof Calendar) {
          Calendar c = (Calendar) val;
          value = vf.createValue(c);
        } else if (val instanceof Boolean) {
          Boolean b = (Boolean) val;
          value = vf.createValue(b);
        } else {
          // Default to a string value..
          value = vf.createValue(val.toString());
        }
        documentNode.setProperty(key, value);
      }

      // Save session
      if (session.hasPendingChanges()) {
        session.save();
      }

    } catch (RepositoryException e) {
      throw new DocProxyException(500,
          "Streaming to ext repo succeeded, writing back into JCR failed.");
    }
  }

  protected File writeStreamToFile(InputStream documentStream, File file)
      throws DocProxyException {

    try {
      // Create path to file..
      List<String> parts = new ArrayList<String>();
      File pathFile = file;
      while (!pathFile.exists()) {
        parts.add(pathFile.getName());
        pathFile = pathFile.getParentFile();
      }

      // Create all the files in the list
      Collections.reverse(parts);
      String start = pathFile.getAbsolutePath();
      for (int i = 0; i < parts.size(); i++) {
        String part = parts.get(i);
        start += "/" + part;
        File pathToFile = new File(start);
        if (i < (parts.size() - 1)) {
          pathToFile.mkdir();
        } else {
          pathToFile.createNewFile();
        }
      }
    } catch (IOException e) {
      throw new DocProxyException(500, "Could not create path to file.");
    }

    // Remove old file.
    if (file.exists()) {
      boolean deleted = file.delete();
      if (!deleted) {
        throw new DocProxyException(500, "Unable to update file.");
      }
    }
    // Create new file.
    File newFile = new File(file.getAbsolutePath());
    try {
      newFile.createNewFile();
    } catch (IOException e) {
      throw new DocProxyException(500, "Unable to create new file.");
    }

    // Check if we can write.
    if (!newFile.canWrite()) {
      throw new DocProxyException(500, "No write access on file.");
    }

    // Write content to new file
    try {
      FileOutputStream out = new FileOutputStream(newFile);
      IOUtils.stream(documentStream, out);
    } catch (IOException e) {
      throw new DocProxyException(500, "Unable to update file.");
    }

    return newFile;
  }

  /**
   * Get the actual java File associated with a JCR node.
   * 
   * @param node
   *          The node in JCR.
   * @param path
   * @return
   * @throws DocProxyException
   *           When we were unable to get the file or read a property from the node.
   */
  protected File getFile(Node node, String path) throws DocProxyException {
    try {
      String basePath = node.getProperty(DocProxyConstants.REPOSITORY_LOCATION)
          .getString();
      while (path.startsWith("/") || path.startsWith(".")) {
        path = path.substring(1);
      }

      if (basePath.endsWith("/")) {
        path = basePath + path;
      } else {
        path = basePath + "/" + path;
      }

      return getFile(path);
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "Unable to read from node property.");
    }
  }

  /**
   * @param path
   *          The absolute path on disk.
   * @return The {@link File} on that path.
   */
  protected File getFile(String path) throws DocProxyException {
    File f = new File(path);
    return f;
  }

  protected File getRootFile(Node node) throws DocProxyException {
    try {
      String basePath = node.getProperty(DocProxyConstants.REPOSITORY_LOCATION)
          .getString();
      File defaultFile = new File(basePath);
      return defaultFile;
    } catch (RepositoryException e) {
      throw new DocProxyException(500, "Failed to mount repo.");
    }
  }

  public void removeDocument(Node node, String path) throws DocProxyException {
    throw new UnsupportedOperationException("DiskProcessor does not support removing documents.");
  }

}
