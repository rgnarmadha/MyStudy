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
package org.sakaiproject.nakamura.activity.routing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityRouter;
import org.sakaiproject.nakamura.api.activity.ActivityRouterManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;

@Component(immediate = true, enabled = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "servie.description", value = "The manager who will pass the activity to all the routers") })
@Service(value = ActivityRouterManager.class)
@Reference(name = "activityRouters", policy=ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ActivityRouter.class, bind = "addActivityRouter", unbind = "removeActivityRouter")
public class ActivityRouterManagerImpl implements ActivityRouterManager {

  private List<ActivityRouter> routers = new ArrayList<ActivityRouter>();
  private Set<ActivityRouter> activityRouters = new HashSet<ActivityRouter>();

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.activity.ActivityRouterManager#getActivityRoutes(javax.jcr.Node)
   */
  public List<ActivityRoute> getActivityRoutes(Node activity) {
    List<ActivityRoute> routes = new ArrayList<ActivityRoute>();
    for (ActivityRouter router : routers) {
      router.route(activity, routes);
    }
    return routes;

  }

  protected void addActivityRouter(ActivityRouter router) {
    activityRouters.add(router);
    routers = getSortedList();

  }

  protected void removeActivityRouter(ActivityRouter router) {
    activityRouters.remove(router);
    routers = getSortedList();
  }

  /**
   * @return Sorts the bound routers based on their priority.
   */
  protected List<ActivityRouter> getSortedList() {
    List<ActivityRouter> sortedRouters = new ArrayList<ActivityRouter>(
        activityRouters);
    Collections.sort(sortedRouters, new Comparator<ActivityRouter>() {

      public int compare(ActivityRouter o1, ActivityRouter o2) {
        return o2.getPriority() - o1.getPriority();
      }
    });
    return sortedRouters;
  }
}
