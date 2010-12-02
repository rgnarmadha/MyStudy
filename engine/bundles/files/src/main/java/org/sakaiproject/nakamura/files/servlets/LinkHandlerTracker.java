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

import static org.sakaiproject.nakamura.api.files.FilesConstants.LINK_HANDLER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.REG_PROCESSOR_NAMES;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.files.LinkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkHandlerTracker {
  private Map<String, LinkHandler> processors = new ConcurrentHashMap<String, LinkHandler>();
  private ComponentContext osgiComponentContext;
  private List<ServiceReference> delayedReferences = new ArrayList<ServiceReference>();

  protected void bindLinkHandler(ServiceReference serviceReference) {

    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.add(serviceReference);
      } else {
        addProcessor(serviceReference);
      }
    }

  }

  protected void unbindLinkHandler(ServiceReference serviceReference) {
    synchronized (delayedReferences) {
      if (osgiComponentContext == null) {
        delayedReferences.remove(serviceReference);
      } else {
        removeProcessor(serviceReference);
      }
    }

  }

  /**
   * @param serviceReference
   */
  private void removeProcessor(ServiceReference serviceReference) {
    String[] processorNames = OsgiUtil.toStringArray(serviceReference
        .getProperty(REG_PROCESSOR_NAMES));

    for (String processorName : processorNames) {
      processors.remove(processorName);
    }
  }

  /**
   * @param serviceReference
   */
  private void addProcessor(ServiceReference serviceReference) {
    LinkHandler processor = (LinkHandler) osgiComponentContext.locateService(
        LINK_HANDLER, serviceReference);
    String[] processorNames = OsgiUtil.toStringArray(serviceReference
        .getProperty(REG_PROCESSOR_NAMES));

    for (String processorName : processorNames) {
      processors.put(processorName, processor);
    }

  }

  /**
   * @param componentContext
   */
  public void setComponentContext(ComponentContext componentContext) {
    synchronized (delayedReferences) {
      osgiComponentContext = componentContext;
      for (ServiceReference ref : delayedReferences) {
        addProcessor(ref);
      }
      delayedReferences.clear();
    }
  }

  /**
   * Gets a processor by name
   * 
   * @param name
   * @return The processor or null if none is found.
   */
  public LinkHandler getProcessorByName(String name) {
    return processors.get(name);
  }

  /**
   * @return
   */
  public Iterable<LinkHandler> getProcessors() {
    return processors.values();
  }

}
