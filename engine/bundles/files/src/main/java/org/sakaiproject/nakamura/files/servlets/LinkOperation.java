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
package org.sakaiproject.nakamura.files.servlets;

import static org.sakaiproject.nakamura.api.files.FilesConstants.TOPIC_FILES_LINK;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 * Create an interal jcr link to a file.
 */
@Component(immediate = true)
@Service(value = SlingPostOperation.class)
@Properties(value = {
    @Property(name = "sling.post.operation", value = "link"),
    @Property(name = "service.description", value = "Creates an internal link to a file."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
    
@ServiceDocumentation(name = "LinkOperation", shortDescription = "Link a node to a file", description = { "Link a node to another node in the repository." }, methods = { @ServiceMethod(name = "POST", description = { "This operation has to be performed on the file NOT the target. In the current implementation we use file link nodes that connect the original uploaded file to another location. This practice will continue except we will obviously link to wherever the file was uploaded." }, parameters = {
    @ServiceParameter(name = ":operation", description = "The value HAS TO BE <i>link</i>."),
    @ServiceParameter(name = "link", description = {
        "The absolute path in JCR where the link should be put.",
        "This can be multivalued. It has to be the same size as the site parameter though." }),
    @ServiceParameter(name = "site", description = {
        "Required: absolute path to a site that should be associated with this file.",
        "This can be multivalued. It has to be the same size as the site parameter though." }) }, response = {
    @ServiceResponse(code = 201, description = {
        "The link was created.",
        "The body will also contain a JSON response that lists all the links and if they were sucesfully created or not." }),
    @ServiceResponse(code = 400, description = "Filedata parameter was not provided."),
    @ServiceResponse(code = 500, description = "Failure with HTML explanation.") }) }, bindings = { @ServiceBinding(type = BindingType.OPERATION, bindings = { "link" }) })
public class LinkOperation extends AbstractSlingPostOperation {

  public static final Logger log = LoggerFactory.getLogger(LinkOperation.class);
  private static final long serialVersionUID = -6206802633585722105L;
  private static final String LINK_PARAM = "link";
  private static final String SITE_PARAM = "site";

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected transient SiteService siteService;

  @Reference
  protected transient EventAdmin eventAdmin;
  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.servlets.post.AbstractSlingPostOperation#doRun(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {

    if (UserConstants.ANON_USERID.equals(request.getRemoteUser())) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN,
          "Anonymous users can't link things.");
      return;
    }

    String link = request.getParameter(LINK_PARAM);
    String site = request.getParameter(SITE_PARAM);
    Resource resource = request.getResource();
    if ( resource == null ) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
      "A link operation must be performed on an actual resource");
      return;      
    }
    Node node = resource.adaptTo(Node.class);
    if (node == null || ResourceUtil.isNonExistingResource(resource)) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A link operation must be performed on an actual resource");
      return;
    }
    if (link == null) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
          "A link parameter has to be provided.");
      return;
    }

    if (site != null) {
      try {
        Session session = request.getResourceResolver().adaptTo(Session.class);
        Node siteNode = (Node) session.getNodeByIdentifier(site);
        if (siteNode == null || !siteService.isSite(siteNode)) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
              "The site parameter doesn't point to a valid site.");
          return;
        }
        site = siteNode.getPath();
      } catch (RepositoryException e) {
        // We assume it went wrong because of a bad parameter.
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST,
            "The site parameter doesn't point to a valid site.");
        return;
      }
    }

    try {
      FileUtils.createLink(node, link, site, slingRepository);
    } catch (RepositoryException e) {
      log.warn("Failed to create a link.", e);
    }
    
    // Send an OSGi event.
    try {
      Dictionary<String, String> properties = new Hashtable<String, String>();
      properties.put(UserConstants.EVENT_PROP_USERID, request.getRemoteUser());
      EventUtils.sendOsgiEvent(request.getResource(), properties, TOPIC_FILES_LINK,
          eventAdmin);
    } catch (Exception e) {
      // We do NOT interrupt the normal workflow if sending an event fails.
      // We just log it to the error log.
      log.error("Could not send an OSGi event for tagging a file", e);
    }
  }
}
