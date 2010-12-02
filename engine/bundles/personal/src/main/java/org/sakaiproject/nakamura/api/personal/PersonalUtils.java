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
package org.sakaiproject.nakamura.api.personal;

import static org.sakaiproject.nakamura.api.personal.PersonalConstants.AUTH_PROFILE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PRIVATE;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PUBLIC;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants._GROUP;
import static org.sakaiproject.nakamura.api.personal.PersonalConstants._USER;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
public class PersonalUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(PersonalUtils.class);

  /**
   * @param au
   *          The authorizable to get the hashed path for.
   * @return The hashed path (ex: a/ad/adm/admi/admin/)
   * @throws RepositoryException
   */
  public static String getUserHashedPath(Authorizable au) throws RepositoryException {
    String hash = null;
    if (au.hasProperty("path")) {
      hash = au.getProperty("path")[0].getString();
    } else {
      LOGGER
          .debug(
              "Authorizable {} has no path property set on it, grabbing hash from ItemBasedPrincipal!",
              au);
      ItemBasedPrincipal principal = (ItemBasedPrincipal) au.getPrincipal();
      hash = principal.getPath();
    }
    return hash;
  }

  public static String getPrimaryEmailAddress(Node profileNode)
      throws RepositoryException {
    String addr = null;
    if (profileNode.hasProperty(PersonalConstants.EMAIL_ADDRESS)) {
      Value[] addrs = JcrUtils.getValues(profileNode, PersonalConstants.EMAIL_ADDRESS);
      if (addrs.length > 0) {
        addr = addrs[0].getString();
      }
    }
    return addr;
  }

  public static String[] getEmailAddresses(Node profileNode) throws RepositoryException {
    String[] addrs = null;
    if (profileNode.hasProperty(PersonalConstants.EMAIL_ADDRESS)) {
      Value[] vaddrs = JcrUtils.getValues(profileNode, PersonalConstants.EMAIL_ADDRESS);
      addrs = new String[vaddrs.length];
      for (int i = 0; i < addrs.length; i++) {
        addrs[i] = vaddrs[i].getString();
      }
    }
    return addrs;
  }

  public static String getPreferredMessageTransport(Node profileNode)
      throws RepositoryException {
    String transport = null;
    if (profileNode.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)) {
      transport = profileNode.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)
          .getString();
    }
    return transport;
  }

  /**
   * @param au
   *          The authorizable to get the authprofile path for.
   * @return The absolute path in JCR to the authprofile node that contains all the
   *         profile information.
   * @deprecated Use the ProfileService.getProfilePath() method.
   */
  @Deprecated
  public static String getProfilePath(Authorizable au) {
    StringBuilder sb = new StringBuilder();
    sb.append(getPublicPath(au)).append("/").append(AUTH_PROFILE);
    return sb.toString();
  }

  /**
   * @param au
   *          The authorizable to get the private path for.
   * @return The absolute path in JCR to the private folder in the user his home folder.
   */
  public static String getPrivatePath(Authorizable au) {
    return getHomeFolder(au) + "/" + PRIVATE;
  }

  /**
   * @param au
   *          The authorizable to get the public path for.
   * @return The absolute path in JCR to the public folder in the user his home folder.
   */
  public static String getPublicPath(Authorizable au) {
    return getHomeFolder(au) + "/" + PUBLIC;
  }

  /**
   * Get the home folder for an authorizable. If the authorizable is a user, this might
   * return: /_user/t/te/tes/test/testuser
   *
   * @param au
   *          The authorizable to get the home folder for.
   * @return The absolute path in JCR to the home folder for an authorizable.
   */
  public static String getHomeFolder(Authorizable au) {
    String folder = PathUtils.getSubPath(au);
    if (au != null && au.isGroup()) {
      folder = _GROUP + folder;
    } else {
      // Assume this is a user.
      folder = _USER + folder;
    }
    return PathUtils.normalizePath(folder);
  }

  /**
   * @param session
   *          The Jackrabbit session.
   * @param id
   *          The id of an authorizable.
   * @return An authorizable that represents a person.
   * @throws RepositoryException
   */
  public static Authorizable getAuthorizable(Session session, String id)
      throws RepositoryException {
    UserManager um = AccessControlUtil.getUserManager(session);
    return um.getAuthorizable(id);
  }

}
