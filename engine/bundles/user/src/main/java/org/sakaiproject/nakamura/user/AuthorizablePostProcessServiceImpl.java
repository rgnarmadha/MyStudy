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
package org.sakaiproject.nakamura.user;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.osgi.AbstractOrderedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Session;

@Component(immediate=true)
@Service(value=AuthorizablePostProcessService.class)
@References({
    /**
     * Below is the list of required Authorizable post-processors.
     * Expect redundant bind/unbind calls, since the same post-processors
     * will be added via the dynamic multiple service reference defined below.
     * TODO Configure the post-processor dependencies via a service property?
     */
    @Reference(name="Personal",
       target="(&(service.pid=org.sakaiproject.nakamura.personal.PersonalAuthorizablePostProcessor))",
       referenceInterface=AuthorizablePostProcessor.class,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor"),
    @Reference(name="Calendar",
        target="(&(service.pid=org.sakaiproject.nakamura.calendar.CalendarAuthorizablePostProcessor))",
        referenceInterface=AuthorizablePostProcessor.class,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor"),
    @Reference(name="Connections",
        target="(&(service.pid=org.sakaiproject.nakamura.connections.ConnectionsUserPostProcessor))",
        referenceInterface=AuthorizablePostProcessor.class,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor"),
    @Reference(name="Messages",
        target="(&(service.pid=org.sakaiproject.nakamura.message.MessageAuthorizablePostProcessor))",
        referenceInterface=AuthorizablePostProcessor.class,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor"),
    @Reference(name="Pages",
        target="(&(service.pid=org.sakaiproject.nakamura.pages.PagesAuthorizablePostProcessor))",
        referenceInterface=AuthorizablePostProcessor.class,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor"),
    @Reference(name="PostProcessors",cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy=ReferencePolicy.DYNAMIC,
        referenceInterface=AuthorizablePostProcessor.class,
        strategy=ReferenceStrategy.EVENT,
        bind="bindAuthorizablePostProcessor",
        unbind="unbindAuthorizablePostProcessor")})
public class AuthorizablePostProcessServiceImpl extends AbstractOrderedService<AuthorizablePostProcessor> implements AuthorizablePostProcessService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizablePostProcessServiceImpl.class);

  @Reference
  protected SlingRepository repository;

  AuthorizablePostProcessor sakaiUserProcessor;
  AuthorizablePostProcessor sakaiGroupProcessor;
  private AuthorizablePostProcessor[] orderedServices = new AuthorizablePostProcessor[0];

  public AuthorizablePostProcessServiceImpl() {
    this.sakaiUserProcessor = new SakaiUserProcessor();
    this.sakaiGroupProcessor = new SakaiGroupProcessor();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification)
   */
  public void process(Authorizable authorizable, Session session, ModificationType change) throws Exception {
    process(authorizable, session, change, new HashMap<String, Object[]>());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session,
      ModificationType change, Map<String, Object[]> parameters) throws Exception {
    // Set up the Modification argument.
    final String pathPrefix = authorizable.isGroup() ?
        UserConstants.SYSTEM_USER_MANAGER_GROUP_PREFIX :
        UserConstants.SYSTEM_USER_MANAGER_USER_PREFIX;
    Modification modification = new Modification(change, pathPrefix + authorizable.getID(), null);

    if (change != ModificationType.DELETE) {
      doInternalProcessing(authorizable, session, modification, parameters);
    }
    for ( AuthorizablePostProcessor processor : orderedServices ) {
      processor.process(authorizable, session, modification, parameters);
      // Allowing a dirty session to pass between post-processor components
      // can trigger InvalidItemStateException after a Workspace.copy.
      // TODO Check to see if this is still a problem after we upgrade to
      // Jackrabbit 2.1.1
      if (session.hasPendingChanges()) {
        session.save();
      }
    }
    if (change == ModificationType.DELETE) {
      doInternalProcessing(authorizable, session, modification, parameters);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, org.apache.sling.api.SlingHttpServletRequest)
   */
  public void process(Authorizable authorizable, Session session,
      ModificationType change, SlingHttpServletRequest request) throws Exception {
    Map<String, Object[]> parameters = new HashMap<String, Object[]>();
    if (request != null) {
      RequestParameterMap originalParameters = request.getRequestParameterMap();
      for (String originalParameterName : originalParameters.keySet()) {
        if (originalParameterName.startsWith(SlingPostConstants.RP_PREFIX)) {
          RequestParameter[] values = originalParameters.getValues(originalParameterName);
          String[] stringValues = new String[values.length];
          for (int i = 0; i < values.length; i++) {
            stringValues[i] = values[i].getString();
          }
          parameters.put(originalParameterName, stringValues);
        }
      }
    }
    process(authorizable, session, change, parameters);
  }

  /**
   * @return
   */
  @Override
  protected Comparator<AuthorizablePostProcessor> getComparator(final Map<AuthorizablePostProcessor, Map<String, Object>> propertiesMap) {
    return new Comparator<AuthorizablePostProcessor>() {
      public int compare(AuthorizablePostProcessor o1, AuthorizablePostProcessor o2) {
        Map<String, Object> props1 = propertiesMap.get(o1);
        Map<String, Object> props2 = propertiesMap.get(o2);

        return OsgiUtil.getComparableForServiceRanking(props1).compareTo(props2);
      }
    };
  }

  protected void bindAuthorizablePostProcessor(AuthorizablePostProcessor service, Map<String, Object> properties) {
    LOGGER.debug("About to add service " + service);
    addService(service, properties);
  }

  protected void unbindAuthorizablePostProcessor(AuthorizablePostProcessor service, Map<String, Object> properties) {
    removeService(service, properties);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.util.osgi.AbstractOrderedService#saveArray(java.util.List)
   */
  @Override
  protected void saveArray(List<AuthorizablePostProcessor> serviceList) {
    orderedServices = serviceList.toArray(new AuthorizablePostProcessor[serviceList.size()]);
  }

  @Activate
  protected void activate(ComponentContext componentContext) {
    LOGGER.debug("activate called");
    this.sakaiUserProcessor = new SakaiUserProcessor();
    this.sakaiGroupProcessor = new SakaiGroupProcessor();
  }

  @Deactivate
  protected void deactivate(ComponentContext componentContext) {
    this.sakaiUserProcessor = null;
    this.sakaiGroupProcessor = null;
  }

  private void doInternalProcessing(Authorizable authorizable, Session session,
      Modification change, Map<String, Object[]> parameters) throws Exception {
    if (authorizable.isGroup()) {
      sakaiGroupProcessor.process(authorizable, session, change, parameters);
    } else {
      sakaiUserProcessor.process(authorizable, session, change, parameters);
    }
  }
}
