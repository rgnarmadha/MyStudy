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
package org.sakaiproject.nakamura.doc.servlet;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;

/**
 * track servlets with service documentation in a single place.
 */
@Component(immediate=true)
@Service(value=ServletDocumentationRegistry.class)
@References(
    value = { 
        @Reference(name = "servlet", referenceInterface=Servlet.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC),
        @Reference(name = "operation", referenceInterface=SlingPostOperation.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    }
  )
public class ServletDocumentationRegistryImpl implements ServletDocumentationRegistry {

  private ComponentContext context;
  private List<ServiceReference> pendingServlets = new ArrayList<ServiceReference>();
  private List<ServiceReference> pendingOperations = new ArrayList<ServiceReference>();
  /**
   * A map of servlet Documetnation objects.
   */
  private Map<String, ServletDocumentation> servletDocumentation = new ConcurrentHashMap<String, ServletDocumentation>();

  protected void activate(ComponentContext context) { 
    synchronized (pendingServlets) {
      this.context = context;
      for (ServiceReference ref : pendingServlets) {
        addServlet(ref);
      }
    }
  }

  protected void deactivate(ComponentContext context) {
    synchronized (pendingServlets) {
      pendingServlets.clear();
      servletDocumentation.clear();
      this.context = null;
    }
  }

  protected void bindServlet(ServiceReference reference) {
    synchronized (pendingServlets) {
      if (context == null) {
        pendingServlets.add(reference);
      } else {
        addServlet(reference);
      }
    }
  }

  protected void unbindServlet(ServiceReference reference) {
    synchronized (pendingServlets) {
      pendingServlets.remove(reference);
      if (context != null) {
        removeServlet(reference);
      }
    }
  }

  /**
   * @param reference
   * @param service
   */
  public void removeServlet(ServiceReference reference) {
    Servlet servlet = (Servlet) context.getBundleContext().getService(reference);
    ServletDocumentation doc = new ServletDocumentation(reference, servlet);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.remove(key);
    }
  }

  /**
   * @param reference
   * @param service
   */
  public void addServlet(ServiceReference reference) {
    Servlet servlet = (Servlet) context.getBundleContext().getService(reference);
    ServletDocumentation doc = new ServletDocumentation(reference, servlet);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.put(key, doc);
    }
  }
  
  /*
   * Operations
   */
  
  protected void bindOperation(ServiceReference reference) {
    synchronized (pendingOperations) {
      if (context == null) {
        pendingOperations.add(reference);
      } else {
        addOperation(reference);
      }
    }
  }

  protected void unbindOperation(ServiceReference reference) {
    synchronized (pendingOperations) {
      pendingOperations.remove(reference);
      if (context != null) {
        removeOperation(reference);
      }
    }
  }

  
  
  /**
   * @param reference
   */
  protected void addOperation(ServiceReference reference) {
    SlingPostOperation operation = (SlingPostOperation) context.getBundleContext().getService(reference);
    ServletDocumentation doc = new ServletDocumentation(reference, operation);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.put(key, doc);
    }
  }
  
  /**
   * @param reference
   */
  protected void removeOperation(ServiceReference reference) {
    SlingPostOperation operation = (SlingPostOperation) context.getBundleContext().getService(reference);
    ServletDocumentation doc = new ServletDocumentation(reference, operation);
    String key = doc.getKey();
    if (key != null) {
      servletDocumentation.remove(key);
    }
  }
  
  
  
  /**
   * @return the map of servlet documents, this map is the internal map and should not be
   *         modified.
   */
  public Map<String, ServletDocumentation> getServletDocumentation() {
    return servletDocumentation;
  }


}
