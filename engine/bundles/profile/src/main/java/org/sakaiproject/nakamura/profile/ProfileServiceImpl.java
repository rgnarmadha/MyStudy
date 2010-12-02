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
package org.sakaiproject.nakamura.profile;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_DESCRIPTION_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_IDENTIFIER_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_JCR_PATH_PREFIX;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_PROFILE_RT;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.GROUP_TITLE_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_BASIC;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_IDENTIFIER_PROPERTY;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_JCR_PATH_PREFIX;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_PICTURE;
import static org.sakaiproject.nakamura.api.profile.ProfileConstants.USER_PROFILE_RT;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrPropertyMap;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
@Component(immediate = true, specVersion = "1.1")
@Service(value = ProfileService.class)
@References(value = { @Reference(name = "ProfileProviders", referenceInterface = ProfileProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, strategy = ReferenceStrategy.EVENT, bind = "bindProfileProvider", unbind = "unbindProfileProvider") })
public class ProfileServiceImpl implements ProfileService {

  private Map<String, ProfileProvider> providers = new ConcurrentHashMap<String, ProfileProvider>();
  private ProviderSettingsFactory providerSettingsFactory = new ProviderSettingsFactory();
  public static final Logger LOG = LoggerFactory.getLogger(ProfileServiceImpl.class);
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getHomePath(org.apache.jackrabbit.api.security.user.Authorizable)
   */
  public String getHomePath(Authorizable authorizable) {
    String folder = PathUtils.getSubPath(authorizable);
    if (authorizable != null && authorizable.isGroup()) {
      folder = GROUP_JCR_PATH_PREFIX + folder;
    } else {
      folder = USER_JCR_PATH_PREFIX + folder;
    }
    return PathUtils.normalizePath(folder);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getPrivatePath(org.apache.jackrabbit.api.security.user.Authorizable)
   */
  public String getPrivatePath(Authorizable authorizable) {
    return getHomePath(authorizable) + "/private";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getPublicPath(org.apache.jackrabbit.api.security.user.Authorizable)
   */
  public String getPublicPath(Authorizable authorizable) {
    return getHomePath(authorizable) + "/public";
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getProfilePath(org.apache.jackrabbit.api.security.user.Authorizable)
   */
  public String getProfilePath(Authorizable authorizable) {
    return getPublicPath(authorizable) + "/authprofile";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getProfileMap(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session)
   */
  public ValueMap getProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException {
    String profilePath = getProfilePath(authorizable);
    String relativePath = profilePath.substring(1);
    ValueMap profileMap;
    if (session.getRootNode().hasNode(relativePath)) {
      Node profileNode = session.getNode(profilePath);
      profileMap = getProfileMap(profileNode);
    } else {
      profileMap = null;
    }
    return profileMap;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getProfileMap(javax.jcr.Node)
   */
  public ValueMap getProfileMap(Node profileNode) throws RepositoryException {
    // Get the data from our external providers.
    Map<String, List<ProviderSettings>> providersMap = scanForProviders(profileNode);
    Map<Node, Future<Map<String, Object>>> providedNodeData = new HashMap<Node, Future<Map<String, Object>>>();
    for (Entry<String, List<ProviderSettings>> e : providersMap.entrySet()) {
      ProfileProvider pp = providers.get(e.getKey());
      if (pp != null) {
        providedNodeData.putAll(pp.getProvidedMap(e.getValue()));
      }
    }
    try {
      // Return it as a ValueMap.
      ValueMap map = new ValueMapDecorator(new HashMap<String, Object>());
      handleNode(profileNode, providedNodeData, map);
      return map;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fills the provided map with the JCR info and the external information.
   *
   * @param node
   *          The node that should be merged with the external info. The entire nodetree
   *          will be checked.
   * @param baseMap
   *          The map that contains the external information.
   * @param map
   *          The map that should be filled.
   * @throws RepositoryException
   * @throws InterruptedException
   * @throws ExecutionException
   */
  protected void handleNode(Node node, Map<Node, Future<Map<String, Object>>> baseMap,
      Map<String, Object> map) throws RepositoryException, InterruptedException,
      ExecutionException {
    // If our map contains this node, that means one of the provides had some information
    // for it.
    // We will use the provider.
    if (baseMap.containsKey(node)) {
      map.putAll(baseMap.get(node).get());
    } else {

      // The node wasn't found in the baseMap.
      // We just dump the JCR properties.
      map.putAll(new JcrPropertyMap(node));
      map.put("jcr:path", PathUtils.translateAuthorizablePath(node.getPath()));
      map.put("jcr:name", node.getName());

      // We loop over the child nodes, but each node get checked against the baseMap
      // again.
      NodeIterator ni = node.getNodes();
      for (; ni.hasNext();) {
        Node childNode = ni.nextNode();
        ValueMap childMap = new ValueMapDecorator(new HashMap<String, Object>());
        handleNode(childNode, baseMap, childMap);
        map.put(childNode.getName(), childMap);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getCompactProfileMap(org.apache.jackrabbit.api.security.user.Authorizable,
   *      javax.jcr.Session)
   */
  public ValueMap getCompactProfileMap(Authorizable authorizable, Session session)
      throws RepositoryException {
    // The map were we will stick the compact information in.
    ValueMap compactProfile;

    // Get the entire profile.
    ValueMap profile = getProfileMap(authorizable, session);
    if (profile == null) {
      compactProfile = null;
    } else {
      compactProfile = new ValueMapDecorator(new HashMap<String, Object>());

      if (authorizable.isGroup()) {
        // For a group we just dump it's title and description.
        compactProfile.put("groupid", authorizable.getID());
        compactProfile.put(GROUP_TITLE_PROPERTY, profile.get(GROUP_TITLE_PROPERTY));
        compactProfile.put(GROUP_DESCRIPTION_PROPERTY, profile
            .get(GROUP_DESCRIPTION_PROPERTY));
      } else {
        compactProfile.put(USER_PICTURE, profile.get(USER_PICTURE));

        try{
          ValueMap basicMap =(ValueMap) profile.get(USER_BASIC);
          if ( basicMap != null ) {
            compactProfile.put(USER_BASIC, basicMap);
          } else {
            LOG.warn("User {} has no basic profile (firstName, lastName and email not avaiable) ",authorizable.getID());
          }
        }catch(Exception e){
          LOG.warn("Can't get authprofile basic information. ", e);
        }
        // Backward compatible reasons.
        compactProfile.put("userid", authorizable.getID());
        compactProfile.put("hash", getUserHashedPath(authorizable));
      }
    }
    return compactProfile;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileService#getCompactProfileMap(javax.jcr.Node)
   */
  public ValueMap getCompactProfileMap(Node profileNode) throws RepositoryException {
    // Get the authorizable from the profile node.
    Session session = profileNode.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable authorizable = null;
    if (profileNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      if (USER_PROFILE_RT.equals(profileNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)
          .getString())) {
        String user = profileNode.getProperty(USER_IDENTIFIER_PROPERTY).getString();
        authorizable = um.getAuthorizable(user);
      }
      if (GROUP_PROFILE_RT.equals(profileNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)
          .getString())) {
        String group = profileNode.getProperty(GROUP_IDENTIFIER_PROPERTY).getString();
        authorizable = um.getAuthorizable(group);
      }
    }
    if (authorizable == null) {
      throw new RepositoryException("The provided node is not a profile node.");
    }

    return getCompactProfileMap(authorizable, session);
  }

  /**
   * Loops over an entire profile and checks if some of the nodes are marked as external.
   * If there is a node found that has it's sakai:source property set to external, we'll
   * look for a ProfileProvider that matches that path.
   *
   * @param baseNode
   *          The top node of a profile.
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(Node baseNode)
      throws RepositoryException {
    Map<String, List<ProviderSettings>> providerMap = new HashMap<String, List<ProviderSettings>>();
    return scanForProviders("", baseNode, providerMap);
  }

  /**
   * @param path
   * @param node
   * @param providerMap
   * @return
   * @throws RepositoryException
   */
  private Map<String, List<ProviderSettings>> scanForProviders(String path, Node node,
      Map<String, List<ProviderSettings>> providerMap) throws RepositoryException {
    ProviderSettings settings = providerSettingsFactory.newProviderSettings(path, node);
    if (settings == null) {
      for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
        Node newNode = ni.nextNode();
        scanForProviders(appendPath(path, newNode.getName()), newNode, providerMap);
      }
    } else {

      List<ProviderSettings> l = providerMap.get(settings.getProvider());

      if (l == null) {
        l = new ArrayList<ProviderSettings>();
        providerMap.put(settings.getProvider(), l);
      }
      l.add(settings);
    }
    return providerMap;
  }

  /**
   * @param string
   * @param name
   * @return
   */
  private String appendPath(String path, String name) {
    if (path.endsWith("/")) {
      return path + name;
    }
    return path + "/" + name;
  }

  /**
   * @param au
   *          The authorizable to get the hashed path for.
   * @return The hashed path (ex: a/ad/adm/admi/admin/)
   * @throws RepositoryException
   */
  private static String getUserHashedPath(Authorizable au) throws RepositoryException {
    String hash = null;
    if (au.hasProperty("path")) {
      hash = au.getProperty("path")[0].getString();
    } else {
      ItemBasedPrincipal principal = (ItemBasedPrincipal) au.getPrincipal();
      hash = principal.getPath();
    }
    return hash;
  }

  protected void bindProfileProvider(ProfileProvider provider,
      Map<String, Object> properties) {
    String name = (String) properties.get(ProfileProvider.PROVIDER_NAME);
    System.err.println("Bound reference with name: " + name);
    if (name != null) {
      providers.put(name, provider);
    }
  }

  protected void unbindProfileProvider(ProfileProvider provider,
      Map<String, Object> properties) {
    String name = (String) properties.get(ProfileProvider.PROVIDER_NAME);
    System.err.println("Unbound reference with name: " + name);
    if (name != null) {
      providers.remove(name);
    }
  }
}
