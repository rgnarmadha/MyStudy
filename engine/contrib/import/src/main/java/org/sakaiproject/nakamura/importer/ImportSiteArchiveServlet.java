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
package org.sakaiproject.nakamura.importer;

import com.ctc.wstx.stax.WstxInputFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@SuppressWarnings("restriction")
@SlingServlet(methods = { "POST" }, resourceTypes = { "sling/servlet/default" }, selectors = { "sitearchive" })
@Properties(value = {
    @Property(name = "service.description", value = "Imports one or more SiteArchive ZIP files from Sakai 2"),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "ImportSiteArchiveServlet", shortDescription = "Imports one or more SiteArchive ZIP files from Sakai 2", description = { "Imports one or more SiteArchive ZIP files from Sakai 2" }, bindings = @ServiceBinding(type = BindingType.TYPE, selectors = @ServiceSelector(name = "sitearchive", description = "Upload one or more ZIP files."), bindings = "sling/servlet/default"), methods = { @ServiceMethod(name = "POST", description = { "Upload one or more SiteArchive ZIP files from Sakai 2" }, parameters = {
    @ServiceParameter(name = "site", description = "Required: A site must be specified, and it must be an absolute path pointing to a site."),
    @ServiceParameter(name = "Filedata", description = "Required: the parameter that holds the actual data for the file that should be uploaded. This can be multivalued.") }, response = {
    @ServiceResponse(code = 200, description = "All files were processed without error."),
    @ServiceResponse(code = 400, description = "site parameter was not provided"),
    @ServiceResponse(code = 400, description = "site parameter was not absolute"),
    @ServiceResponse(code = 400, description = "Filedata parameter was not provided."),
    @ServiceResponse(code = 415, description = "The uploaded file was not a valid ZIP file."),
    @ServiceResponse(code = 500, description = "Unexpected error.") }) })
public class ImportSiteArchiveServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = 1678771348231033621L;
  public static final Logger LOG = LoggerFactory
      .getLogger(ImportSiteArchiveServlet.class);

  @Reference
  transient SlingRepository slingRepository;

  @Reference
  transient ClusterTrackingService clusterTrackingService;

  private transient XMLInputFactory xmlInputFactory = null;
  private transient Base64 base64 = new Base64();
  private final String[] supportedVersions = { "Sakai 1.0" };
  private transient SimpleDateFormat sdf = new SimpleDateFormat(
      "yyyyMMddHHmmssSSS");

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    xmlInputFactory = new WstxInputFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT+0"));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException {
    final RequestParameter siteParam = request.getRequestParameter("site");
    if (siteParam == null || !siteParam.getString().startsWith("/")) {
      final String errorMessage = "A site must be specified, and it must be an absolute path pointing to a site.";
      sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage,
          new IllegalArgumentException(errorMessage), response);
      return;
    }
    final String sitePath = siteParam.getString();

    final RequestParameter[] files = request.getRequestParameters("Filedata");
    if (files == null || files.length < 1) {
      final String errorMessage = "Missing Filedata parameter.";
      sendError(HttpServletResponse.SC_BAD_REQUEST, errorMessage,
          new IllegalArgumentException(errorMessage), response);
      return;
    }
    final Session session = request.getResourceResolver()
        .adaptTo(Session.class);
    for (RequestParameter p : files) {
      LOG.info("Processing file: " + p.getFileName() + ": "
          + p.getContentType() + ": " + p.getSize() + " bytes");
      try {
        // create temporary local file of zip contents
        final File tempZip = File.createTempFile("siteArchive", ".zip");
        tempZip.deleteOnExit(); // just in case
        final InputStream in = p.getInputStream();
        final FileOutputStream out = new FileOutputStream(tempZip);
        final byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        in.close();
        out.close();
        // process the zip file
        ZipFile zip = null;
        try {
          zip = new ZipFile(tempZip);
        } catch (ZipException e) {
          sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
              "Invalid zip file: " + p.getFileName() + ": "
                  + p.getContentType() + ": " + p.getSize(), null, response);
        }
        if (zip != null) {
          for (ZipEntry entry : Collections.list(zip.entries())) {
            if (entry.getName().startsWith("__MACOSX")
                || entry.getName().endsWith(".DS_Store")) {
              ; // skip entry
            } else {
              if ("content.xml".equals(entry.getName())) {
                processContentXml(zip.getInputStream(entry), sitePath, session,
                    zip);
              }
            }
          }
          zip.close();
        }
        // delete temporary file
        if (tempZip.delete()) {
          LOG.debug("{}: temporary zip file deleted.", tempZip
              .getAbsolutePath());
        } else {
          LOG.warn("Could not delete temporary file: {}", tempZip
              .getAbsolutePath());
        }
      } catch (IOException e) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
            .getLocalizedMessage(), e, response);
      } catch (XMLStreamException e) {
        sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
            .getLocalizedMessage(), e, response);
      }
    }
    response.setStatus(HttpServletResponse.SC_OK);
    return;
  }

  private void processContentXml(InputStream in, String sitePath,
      Session session, ZipFile zip) throws XMLStreamException {
    Map<String, Resource> resources = new HashMap<String, Resource>();
    String currentResourceId = null;
    XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(in);
    for (int event = reader.next(); event != XMLStreamReader.END_DOCUMENT; event = reader
        .next()) {
      String localName = null;
      switch (event) {
      case XMLStreamReader.START_ELEMENT:
        localName = reader.getLocalName();
        if ("archive".equalsIgnoreCase(localName)) {
          final String system = reader.getAttributeValue(null, "system");
          boolean supportedVersion = false;
          for (String version : supportedVersions) {
            if (version.equalsIgnoreCase(system)) {
              supportedVersion = true;
            }
          }
          if (!supportedVersion) {
            throw new Error("Not a supported version: " + system);
          }
          break;
        }
        if ("collection".equalsIgnoreCase(localName)
            || "resource".equalsIgnoreCase(localName)) {
          // grab the resource's attributes
          Resource resource = new Resource();
          for (int i = 0; i < reader.getAttributeCount(); i++) {
            resource.attributes.put(reader.getAttributeLocalName(i)
                .toLowerCase(), reader.getAttributeValue(i));
          }
          currentResourceId = resource.getId();
          resources.put(currentResourceId, resource);
          break;
        }
        if ("property".equalsIgnoreCase(localName)) {
          Resource resource = resources.get(currentResourceId);
          final String name = reader.getAttributeValue(null, "name");
          String value = reader.getAttributeValue(null, "value");
          if (value != null && !"".equals(value)) {
            if (reader.getAttributeValue(null, "enc")
                .equalsIgnoreCase("BASE64")) {
              value = new String(base64.decode(value));
            }
            resource.properties.put(name, value);
          }
          break;
        }
        break;
      case XMLStreamReader.END_ELEMENT:
        localName = reader.getLocalName();
        if ("collection".equalsIgnoreCase(localName)
            || "resource".equalsIgnoreCase(localName)) {
          makeResource(resources.get(currentResourceId), sitePath, session, zip);
        }
        break;
      } // end switch
    } // end for
    reader.close();
  }

  private void sendError(int errorCode, String message, Throwable exception,
      HttpServletResponse response) {
    if (!response.isCommitted()) {
      try {
        LOG.error(errorCode + ": " + message, exception);
        response.sendError(errorCode, message);
      } catch (IOException e) {
        throw new Error(e);
      }
    } else {
      LOG.error(errorCode + ": " + message, exception);
      throw new Error(message, exception);
    }
  }

  private void makeResource(Resource resource, String sitePath,
      Session session, ZipFile zip) {
    if (resource == null) {
      throw new IllegalArgumentException("Illegal Resource");
    }
    final String resourceType = resource.getType();
    if ("org.sakaiproject.content.types.folder".equalsIgnoreCase(resourceType)) {
      // folders are not currently supported in K2 - so ignore them
      // final Node node = makeNode(destination, session);
      // applyMetaData(node, resource, session);
    } else if ("org.sakaiproject.content.types.fileUpload"
        .equalsIgnoreCase(resourceType)
        || "org.sakaiproject.content.types.TextDocumentType"
            .equalsIgnoreCase(resourceType)
        || "org.sakaiproject.content.types.HtmlDocumentType"
            .equalsIgnoreCase(resourceType)) {
      final String relativeId = resource.getRelativeId();
      String fileName = null;
      if (relativeId.contains("/")) {
        // folders are not currently supported in K2 - strip the folders.
        fileName = relativeId.substring(relativeId.lastIndexOf("/") + 1);
      } else {
        fileName = relativeId;
      }
      final Node node = copyFile(resource.attributes.get("body-location"),
          fileName, sitePath, resource.attributes.get("content-type"), session,
          zip);
      applyMetaData(node, resource, session);
    } else if ("org.sakaiproject.content.types.urlResource"
        .equalsIgnoreCase(resourceType)) {
      // ignore urlResources for now - until BigStore refactor is complete
      // final String nodeName = destination.replace(":", "");
      // final Node node = makeNode(nodeName, session);
      // try {
      // applyMetaData(node, resource, session);
      // node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
      // "sling:redirect");
      // node.setProperty("sling:target", resource.properties
      // .get("DAV:displayname"));
      // } catch (Exception e) {
      // throw new Error(e);
      // }
    } else {
      LOG.error("Missing handler for type: " + resourceType + ": " + resource);
    }
  }

  private Node makeNode(String path, Session session) {
    if (!"/".equals(path) && path.endsWith("/")) { // strip trailing slash
      path = path.substring(0, path.lastIndexOf("/"));
    }
    Node node = null;
    try {
      node = JcrUtils.deepGetOrCreateNode(session, path);
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      throw new Error(e);
    }
    return node;
  }

  private Node copyFile(String zipEntryName, String fileName, String sitePath,
      String contentType, Session session, ZipFile zip) {
    final String id = uniqueId();
    final String path = FilesConstants.USER_FILESTORE + "/" + id;
    Node node = null;
    try {
      final InputStream in = zip.getInputStream(zip.getEntry(zipEntryName));
      node = makeNode(path, session);
      node.setProperty(JcrConstants.JCR_NAME, fileName);
      node.setProperty(JcrConstants.JCR_MIMETYPE, contentType);
      ValueFactory valueFactory = session.getValueFactory();
      Binary content = valueFactory.createBinary(in);
      node.setProperty(JcrConstants.JCR_CONTENT, content);
      final String linkPath = sitePath + "/_files/" + fileName;
      FileUtils.createLink(node, linkPath, sitePath, slingRepository);
    } catch (RepositoryException e) {
      throw new Error(e);
    } catch (IOException e) {
      throw new Error(e);
    }
    return node;
  }

  private void applyMetaData(Node node, Resource resource, Session session) {
    final Calendar calendar = Calendar.getInstance(TimeZone
        .getTimeZone("GMT+0"));
    try {
      final boolean isNtFile = JcrConstants.NT_FILE.equals(node.getProperty(
          JcrConstants.JCR_PRIMARYTYPE).getString());
      if (!isNtFile) { // only set these properties if not nt:file; i.e. they
        // will already be set otherwise.
        // sakai:id
        // node.setProperty(FilesConstants.SAKAI_ID, uniqueId());

        // sakai:user
        // node.setProperty(FilesConstants.SAKAI_USER, session.getUserID());

        // jcr:mimeType
        final String mimeType = resource.attributes.get("content-type");
        if (mimeType != null && !"".equals(mimeType)) {
          node.setProperty(JcrConstants.JCR_MIMETYPE, mimeType);
        }
      }

      // loop through all properties
      for (String key : resource.properties.keySet()) {
        final String value = resource.properties.get(key);
        if (value == null || "".equals(value)) {
          continue; // ignore empty values
        }
        // sakai:filename
        else if (!isNtFile && "DAV:displayname".equals(key)) {
          node.setProperty("sakai:filename", value);
          continue;
        }
        // sakai:description
        else if ("CHEF:description".equals(key)) {
          node.setProperty("sakai:description", value);
          continue;
        }
        // jcr:created
        else if ("DAV:creationdate".equals(key)
            && !node.isNodeType(JcrConstants.NT_FILE)) {
          // cannot set jcr:created on files; i.e. nt:file
          calendar.setTime(sdf.parse(value));
          node.setProperty(JcrConstants.JCR_CREATED, calendar);
          continue;
        }
        // jcr:lastModified
        else if ("DAV:getlastmodified".equals(key)) {
          calendar.setTime(sdf.parse(value));
          node.setProperty(JcrConstants.JCR_LASTMODIFIED, calendar);
          continue;
        }
        // map Dublin Core Metadata
        else if (key.startsWith("http://purl.org/dc/")) {
          // remap namespace
          final String purl = key.replace("http://purl.org/dc/", "purl:");
          node.setProperty(purl, value);
          continue;
        }
        // map CHEF properties
        else if (key.startsWith("CHEF:")) {
          // one-to-one namespace mapping
          node.setProperty(key, value);
          continue;
        }
        // map SAKAI properties
        else if (key.startsWith("SAKAI:")) {
          // map to sakai: namespace (i.e. lowercase)
          node.setProperty(key.replace("SAKAI:", "sakai:"), value);
          continue;
        }
        // DAV properties
        else if (key.startsWith("DAV:")) {
          // all remaining DAV properties will be ignored to avoid conflicts
          // with WebDAV server
          continue;
        }
        LOG.error("Unknown metadata not imported: {},{}", new String[] { key,
            value });
      }
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (Exception e) {
      throw new Error(e);
    }
  }

  /**
   * Generate a cluster unique String id.
   * 
   * @return A String which is unique within the cluster.
   */
  private String uniqueId() {
    LOG.debug("uniqueId()");
    // copied from FilesUploadServlet.java
    String id = clusterTrackingService.getClusterUniqueId();
    if (id.endsWith("==")) {
      id = id.substring(0, id.length() - 2);
    }
    id = id.replace('/', '_').replace('=', '-');
    // end copied from FilesUploadServlet.java
    return id;
  }

  /**
   * Simple data object to collect the data being parsed from content.xml
   */
  private static class Resource {
    private Map<String, String> attributes = new HashMap<String, String>();
    private Map<String, String> properties = new HashMap<String, String>();

    public String getId() {
      return attributes.get("id");
    }

    public String getRelativeId() {
      return attributes.get("rel-id");
    }

    public String getType() {
      return attributes.get("resource-type");
    }

    @Override
    public int hashCode() {
      return this.getId().hashCode();
    }

    @Override
    public String toString() {
      return getRelativeId();
    }
  }
}
