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

import static org.sakaiproject.nakamura.api.files.FilesConstants.REQUIRED_MIXIN;
import static org.sakaiproject.nakamura.api.files.FilesConstants.RT_SAKAI_LINK;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_LINK;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAGS;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_NAME;
import static org.sakaiproject.nakamura.api.files.FilesConstants.SAKAI_TAG_UUIDS;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.files.pool.CreateContentPoolServlet;
import org.sakaiproject.nakamura.util.DateUtils;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

/**
 * Some utility function regarding file management.
 */
public class FileUtils {

  public static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  /**
   * Create a link to a file. There is no need to call a session.save, the change is
   * persistent.
   *
   * @param fileNode
   *          The node that represents the file. This node has to be retrieved via the
   *          normal user his {@link Session session}. If the userID equals
   *          {@link UserConstants.ANON_USERID} an AccessDeniedException will be thrown.
   * @param linkPath
   *          The absolute path in JCR where the link should be placed.
   * @param sitePath
   *          An optional absolute path in JCR to a site. If this parameter is null, it
   *          will be ignored.
   * @param slingRepository
   *          The {@link SlingRepository} to use to login as an administrative.
   * @return The newly created node.
   * @throws AccessDeniedException
   *           When the user is anonymous.
   * @throws RepositoryException
   *           Something else went wrong.
   */
  public static Node createLink(Node fileNode, String linkPath, String sitePath,
      SlingRepository slingRepository) throws AccessDeniedException, RepositoryException {
    Session session = fileNode.getSession();
    String userId = session.getUserID();
    if (UserConstants.ANON_USERID.equals(userId)) {
      throw new AccessDeniedException();
    }

    boolean hasMixin = JcrUtils.hasMixin(fileNode, REQUIRED_MIXIN) && fileNode.canAddMixin(REQUIRED_MIXIN);
    // If the fileNode doesn't have the required referenceable mixin, we need to set it.
    // Also, if we want to link this file into a site. We have to be
    if (!hasMixin || sitePath != null) {
      // The required mixin is not on the node.
      // Set it.
      Session adminSession = null;
      try {
        adminSession = slingRepository.loginAdministrative(null);

        // Grab the node via the adminSession
        String path = fileNode.getPath();
        Node adminFileNode = (Node) adminSession.getItem(path);
        if (!hasMixin) {
          adminFileNode.addMixin(REQUIRED_MIXIN);
        }

        // Used in a site.
        if (sitePath != null) {
          Node siteNode = (Node) session.getItem(sitePath);
          String site = siteNode.getIdentifier();
          JcrUtils.addUniqueValue(adminSession, adminFileNode, "sakai:sites", site,
              PropertyType.STRING);
        }

        if (adminSession.hasPendingChanges()) {
          adminSession.save();
        }
      } finally {
        adminSession.logout();
      }
    }

    // Now that the file is referenceable, it has a uuid.
    // Use it for the link.
    // Grab the (updated) node via the user's session id.
    fileNode = (Node) session.getItem(fileNode.getPath());

    // Create the link
    Node linkNode = JcrUtils.deepGetOrCreateNode(session, linkPath);
    if (!"sling:Folder".equals(linkNode.getPrimaryNodeType().getName())) {
      // sling folder allows single and multiple properties, no need for the mixin.
      if ( linkNode.canAddMixin(REQUIRED_MIXIN) ) {
        linkNode.addMixin(REQUIRED_MIXIN);
      }
    }
    linkNode
        .setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY, RT_SAKAI_LINK);
    linkNode.setProperty(SAKAI_LINK, fileNode.getIdentifier());

    // Save link.
    if (session.hasPendingChanges()) {
      session.save();
    }

    return linkNode;
  }

  /**
   * Writes all the properties of a sakai/file node. Also checks what the permissions are
   * for a session and where the links are.<br/>
   * Same as calling {@link #writeFileNode(Node, Session, JSONWriter, SiteService, 0)}
   *
   * @param node
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeFileNode(Node node, Session session, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    writeFileNode(node, session, write, siteService, 0);
  }

  /**
   * Writes all the properties of a sakai/file node. Also checks what the permissions are
   * for a session and where the links are.
   *
   * @param node
   * @param write
   * @param objectInProgress
   *          Whether object creation is in progress. If false, object is started and
   *          ended in this method call.
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeFileNode(Node node, Session session, JSONWriter write,
      SiteService siteService, int maxDepth) throws JSONException, RepositoryException {

    write.object();

    // dump all the properties.
    ExtendedJSONWriter.writeNodeTreeToWriter(write, node, true, maxDepth);
    // The permissions for this session.
    writePermissions(node, session, write);

    if (node.hasNode(JcrConstants.JCR_CONTENT)) {
      Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
      write.key(JcrConstants.JCR_LASTMODIFIED);
      Calendar cal = contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getDate();
      write.value(DateUtils.iso8601(cal));
      write.key(JcrConstants.JCR_MIMETYPE);
      write.value(contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString());

      if (contentNode.hasProperty(JcrConstants.JCR_DATA)) {
        write.key(JcrConstants.JCR_DATA);
        write.value(contentNode.getProperty(JcrConstants.JCR_DATA).getLength());
      }
    }

    // Get all the sites where this file is referenced.
    getSites(node, write, siteService);

    write.endObject();
  }

  /**
   * Writes all the properties for a linked node.
   *
   * @param node
   * @param write
   * @param siteService
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void writeLinkNode(Node node, Session session, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    write.object();
    // Write all the properties.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, node);
    // permissions
    writePermissions(node, session, write);

    // Write the actual file.
    if (node.hasProperty(SAKAI_LINK)) {
      String uuid = node.getProperty(SAKAI_LINK).getString();
      write.key("file");
      try {
        Node fileNode = session.getNodeByIdentifier(uuid);
        writeFileNode(fileNode, session, write, siteService);
      } catch (ItemNotFoundException e) {
        write.value(false);
      }
    }

    write.endObject();
  }

  /**
   * Gives the permissions for this user.
   *
   * @param node
   * @param session
   * @param write
   * @throws RepositoryException
   * @throws JSONException
   */
  private static void writePermissions(Node node, Session session, JSONWriter write)
      throws RepositoryException, JSONException {
    String path = node.getPath();
    write.key("permissions");
    write.object();
    write.key("set_property");
    write.value(hasPermission(session, path, "set_property"));
    write.key("read");
    write.value(hasPermission(session, path, "read"));
    write.key("remove");
    write.value(hasPermission(session, path, "remove"));
    write.endObject();
  }

  /**
   * Checks if the current user has a permission on a path.
   *
   * @param session
   * @param path
   * @param permission
   * @return
   */
  private static boolean hasPermission(Session session, String path, String permission) {
    try {
      session.checkPermission(path, permission);
      return true;
    } catch (AccessControlException e) {
      return false;
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Gets all the sites where this file is used and parses the info for it.
   *
   * @param node
   * @param write
   * @throws RepositoryException
   * @throws JSONException
   */
  @SuppressWarnings(justification = "Need to trap subsystem errors ", value = { "REC_CATCH_EXCEPTION" })
  private static void getSites(Node node, JSONWriter write, SiteService siteService)
      throws RepositoryException, JSONException {

    write.key("usedIn");
    write.object();
    write.key("sites");
    write.array();

    // sakai:sites contains uuid's of sites where the file is being referenced.
    Value[] sites = JcrUtils.getValues(node, "sakai:sites");
    Session session = node.getSession();

    int total = 0;
    try {
      List<String> handledSites = new ArrayList<String>();
      AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
      Privilege read = acm.privilegeFromName(Privilege.JCR_READ);
      Privilege[] privs = new Privilege[] { read };
      for (Value v : sites) {
        String path = v.getString();
        if (!handledSites.contains(path)) {
          handledSites.add(path);
          Node siteNode = session.getNodeByIdentifier(v.getString());

          boolean hasAccess = acm.hasPrivileges(path, privs);
          if (siteService.isSite(siteNode) && hasAccess) {
            writeSiteInfo(siteNode, write, siteService);
            total++;
          }
        }
      }
    } catch (Exception e) {
      // We ignore every exception it has when looking up sites.
      // it is dirty ..
      log.info("Catched exception when looking up used sites for a file. "
          + e.getMessage());
    }
    write.endArray();
    write.key("total");
    write.value(total);
    write.endObject();
  }

  /**
   * Parses the info for a site.
   *
   * @param siteNode
   * @param write
   * @throws JSONException
   * @throws RepositoryException
   */
  protected static void writeSiteInfo(Node siteNode, JSONWriter write,
      SiteService siteService) throws JSONException, RepositoryException {
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(siteNode)));
    ExtendedJSONWriter.writeNodeContentsToWriter(write, siteNode);
    write.endObject();
  }

  /**
   * Check if a node is a proper sakai tag.
   *
   * @param node
   *          The node to check if it is a tag.
   * @return true if the node is a tag, false if it is not.
   * @throws RepositoryException
   */
  public static boolean isTag(Node node) throws RepositoryException {
    if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
        && FilesConstants.RT_SAKAI_TAG.equals(node.getProperty(
            JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
      return true;
    }
    return false;
  }

  /**
   * Add's a tag on a node. If the tag has a name defined in the {@link Property property}
   * sakai:tag-name it will be added in the fileNode as well.
   *
   * @param adminSession
   *          The session that can be used to modify the fileNode.
   * @param fileNode
   *          The node that needs to be tagged.
   * @param tagNode
   *          The node that represents the tag.
   */
  public static void addTag(Session adminSession, Node fileNode, Node tagNode)
      throws RepositoryException {
    if (tagNode == null || fileNode == null) {
      throw new RuntimeException(
          "Cant tag non existant nodes, sorry, both must exist prior to tagging. File:"
              + fileNode + " Node To Tag:" + tagNode);
    }
    // Grab the node via the adminSession
    String path = fileNode.getPath();
    fileNode = (Node) adminSession.getItem(path);

    // Check if the mixin is on the node.
    // This is nescecary for nt:file nodes.
    if (!JcrUtils.hasMixin(fileNode, REQUIRED_MIXIN)) {
      if ( fileNode.canAddMixin(REQUIRED_MIXIN)) {
        fileNode.addMixin(REQUIRED_MIXIN);
      }
    }

    // Add the reference from the tag to the node.
    String tagUuid = tagNode.getIdentifier();
    String tagName = tagNode.getName();
    if (tagNode.hasProperty(SAKAI_TAG_NAME)) {
      tagName = tagNode.getProperty(SAKAI_TAG_NAME).getString();
    }
    JcrUtils.addUniqueValue(adminSession, fileNode, SAKAI_TAG_UUIDS, tagUuid,
        PropertyType.STRING);
    JcrUtils.addUniqueValue(adminSession, fileNode, SAKAI_TAGS, tagName,
        PropertyType.STRING);

  }

  /**
   * Delete a tag from a node.
   *
   * @param adminSession
   * @param fileNode
   * @param tagNode
   * @throws RepositoryException
   */
  public static void deleteTag(Session adminSession, Node fileNode, Node tagNode)
      throws RepositoryException {
    if (tagNode == null || fileNode == null) {
      throw new RuntimeException("Can't delete tag from non existent nodes. File:"
          + fileNode + " Node To Tag:" + tagNode);
    }
    // Grab the node via the adminSession
    String path = fileNode.getPath();
    fileNode = (Node) adminSession.getItem(path);

    // Add the reference from the tag to the node.
    String tagUuid = tagNode.getIdentifier();
    String tagName = tagNode.getName();
    if (tagNode.hasProperty(SAKAI_TAG_NAME)) {
      tagName = tagNode.getProperty(SAKAI_TAG_NAME).getString();
    }

    JcrUtils.deleteValue(adminSession, fileNode, SAKAI_TAG_UUIDS, tagUuid);
    JcrUtils.deleteValue(adminSession, fileNode, SAKAI_TAGS, tagName);
  }

  /**
   * Resolves a Node given one of three possible passed parameters: 1) A fully qualified
   * path to a Node (e.g. "/foo/bar/baz"), 2) a Node's UUID, or 3) the PoolId from a
   * ContentPool.
   *
   * @param pathOrIdentifier
   *          One of three possible parameters: 1) A fully qualified path to a Node (e.g.
   *          "/foo/bar/baz"), 2) a Node's UUID, or 3) the PoolId from a ContentPool.
   * @param session
   * @return If the Node cannot be resolved, <code>null</code> will be returned.
   * @throws IllegalArgumentException
   */
  public static Node resolveNode(final String pathOrIdentifier, final Session session) {
    if (pathOrIdentifier == null || "".equals(pathOrIdentifier)) {
      throw new IllegalArgumentException("Passed argument was null or empty");
    }
    if (session == null) {
      throw new IllegalArgumentException("Session cannot be null");
    }
    Node node = null;
    try {
      if (pathOrIdentifier.startsWith("/")) { // it is a path specification
        node = session.getNode(pathOrIdentifier);
      } else {
        // assume we have a UUID and try to resolve
        try {
          node = session.getNodeByIdentifier(pathOrIdentifier);
        } catch (RepositoryException e) {
          log.debug("Swallowed exception; i.e. normal operation: {}",
              e.getLocalizedMessage(), e);
        }
      }
    } catch (PathNotFoundException e) {
      // Normal execution path - ignore
    } catch (ItemNotFoundException e) {
      // Normal execution path - ignore
    } catch (Throwable e) {
      log.error(e.getLocalizedMessage(), e);
    }
    if (node == null) {
      // must not have been a UUID; resolve via poolId
      try {
        //
        final String poolPath = CreateContentPoolServlet.hash(pathOrIdentifier);
        node = session.getNode(poolPath);
      } catch (PathNotFoundException e) {
        // Normal execution path - ignore
      } catch (Throwable e) {
        log.error(e.getLocalizedMessage(), e);
      }
    }
    return node;
  }

}
