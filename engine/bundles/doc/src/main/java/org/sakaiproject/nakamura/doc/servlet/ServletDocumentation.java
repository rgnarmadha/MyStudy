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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.servlets.post.SlingPostOperation;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.doc.sling.DocumentationProxyPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;

/**
 * Hold documentation for the servlet and provides an HTML serialization.
 */
public class ServletDocumentation implements Comparable<ServletDocumentation> {

  private static final String PACKAGE = DocumentationProxyPackage.class.getPackage().getName()+".Doc_";
  private static final Logger LOGGER = LoggerFactory
      .getLogger(ServletDocumentation.class);
  private ServiceDocumentation serviceDocumetation;
  private String serviceName;
  private Object service;
  private ServiceReference reference;

  /**
   * @param reference
   * @param service
   */
  public ServletDocumentation(ServiceReference reference, Object service) {
    this.service = service;
    this.reference = reference;
    serviceDocumetation = (ServiceDocumentation) service.getClass()
        .getAnnotation(ServiceDocumentation.class);
    if (serviceDocumetation == null) {
      // try and load an info class.
      String name = service.getClass().getName();
      name = PACKAGE + name.replace('.', '_');
      try {
        Class<?> c = this.getClass().getClassLoader().loadClass(name);
        serviceDocumetation = (ServiceDocumentation) c
            .getAnnotation(ServiceDocumentation.class);
      } catch (ClassNotFoundException ex) {
        LOGGER.warn("No documentation proxy {} ", name);
        // no doc class present.
      }
    }
    serviceName = getServiceName(reference, service, serviceDocumetation);

  }

  private static String getBundleName(ServiceReference reference) {
    Bundle bundle = reference.getBundle();
    String bundleName = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
    if (bundleName == null) {
      bundleName = bundle.getSymbolicName();
      if (bundleName == null) {
        bundleName = bundle.getLocation();
      }
    }
    return bundleName;
  }

  /**
   * Generate a service name for the supplied service and reference.
   * 
   * @param reference
   *          OSGi reference for the service.
   * @param service
   *          the Servlet.
   * @param serviceDocumetation
   *          the documentation annotation, if present, null if not.
   * @return the service name.
   */
  private static String getServiceName(ServiceReference reference,
      Object service, ServiceDocumentation serviceDocumetation) {
    String name = service.getClass().getName();
    int i = name.lastIndexOf('.');
    name = i > 0 ? name.substring(i + 1) : name;
    String bundleName = getBundleName(reference);
    if (serviceDocumetation == null) {
      return bundleName + ":" + name + ":"
          + String.valueOf(reference.getProperty(Constants.SERVICE_ID));
    } else {
      return bundleName + ":" + name + ":" + serviceDocumetation.name();
    }
  }

  /**
   * Documentation of the service.
   * 
   * @param request
   *          the current request.
   * @param response
   *          the current response.
   * @throws IOException
   *           of there were issues writing to the response.
   */
  public void send(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws IOException {
    PrintWriter writer = response.getWriter();

    if (serviceDocumetation == null) {
      writer
          .append("<p>No Service documentation has been provided by the developer, tut tut!</p>");
    } else {
      writer.append("<h3>Description</h3><p>");
      sendDoc(writer, serviceDocumetation.description());

      writer.append("</p><h3>Bindings</h3><ul>");

      for (ServiceBinding sb : serviceDocumetation.bindings()) {
        writer.append("<li><p>Type:");
        writer.append(sb.type().toString());
        sendList(writer, sb.bindings());
        writer.append("</p><p>Selectors:");
        if (sb.selectors().length == 0) {
          writer.append("None");
        } else {
          writer.append("<ul>");
          for (ServiceSelector ss : sb.selectors()) {
            writer.append("<li><p>");
            writer.append(ss.name());
            writer.append("</p>");
            sendDoc(writer, ss.description());
            writer.append("</li>");
          }
          writer.append("</ul>");
        }
        writer.append("</p><p>Extensions:");
        if (sb.extensions().length == 0) {
          writer.append("None");
        } else {
          writer.append("<ul>");
          for (ServiceExtension se : sb.extensions()) {
            writer.append("<li><p>");
            writer.append(se.name());
            writer.append("</p>");
            sendDoc(writer, se.description());
            writer.append("</li>");
          }
          writer.append("</ul>");
        }
        writer.append("</li>");

      }
      writer.append("</ul><h3>Methods</h3><ul>");
      for (ServiceMethod sm : serviceDocumetation.methods()) {
        writer.append("<li><h4>Method:");
        writer.append(sm.name());
        writer.append("</h4>");
        sendDoc(writer, sm.description());
        writer.append("<h4>Parameters</h4><ul>");
        for (ServiceParameter sp : sm.parameters()) {
          writer.append("<li><p>");
          writer.append(sp.name());
          writer.append("</p>");
          sendDoc(writer, sp.description());
          writer.append("</li>");
        }
        writer.append("</ul><h4>Status Codes</h4><ul>");
        for (ServiceResponse sp : sm.response()) {
          writer.append("<li><p>");
          writer.append(String.valueOf(sp.code()));
          writer.append("</p>");
          sendDoc(writer, sp.description());
          writer.append("</li>");
        }
        writer.append("</ul></li>");
      }
      writer.append("</ul>");
    }

    writer.append("<h2>Service Properties</h2><ul>");
    for (String k : reference.getPropertyKeys()) {
      writer.append("<li>");
      writer.append(k);
      writer.append(": ");
      writer.append(String.valueOf(reference.getProperty(k)));
      writer.append("</li>");

    }
    writer.append("</ul><h2>Other Services</h2><ul>");
    for (ServiceReference k : reference.getBundle().getRegisteredServices()) {
      Object service = k.getBundle().getBundleContext().getService(k);
      if (service instanceof Servlet || service instanceof SlingPostOperation) {
        ServletDocumentation sd = new ServletDocumentation(k, service);
        String key = sd.getKey();
        if (key != null) {
          writer.append("<li><a href=\"");
          writer.append("?p=");
          writer.append(sd.getKey());
          writer.append("\">");
          writer.append(sd.getName());
          writer.append("</a>");
          writer.append("</li>");
        } else {
          writer.append("<li>");
          writer.append(service.getClass().getName());
          writer.append("</li>");
        }
      } else {
        writer.append("<li>");
        writer.append(service.getClass().getName());
        writer.append("</li>");
      }
    }
    writer.append("</ul>");

  }

  /**
   * @param writer
   * @param bindings
   */
  private void sendList(PrintWriter writer, String[] list) {
    writer.append("<ul>");
    for (String binding : list) {
      writer.append("<li>");
      writer.append(binding);
      writer.append("</li>");
    }
    writer.append("</ul>");
  }

  /**
   * @param writer
   * @param description
   */
  private void sendDoc(PrintWriter writer, String[] description) {
    for (String desc : description) {
      writer.append("<p>");
      writer.append(desc);
      writer.append("</p>");
    }
  }

  /**
   * @return the name of the servlet.
   */
  public String getName() {
    return serviceName;
  }

  /**
   * @return Returns the name of this servlet as specified in the serviceDocumentation
   */
  public String getServiceDocumentationName() {
    if (serviceDocumetation == null) {
      return "No name available";
    } else {
      return serviceDocumetation.name();
    }
  }

  /**
   * @return a short description of the Servlet.
   */
  public String getShortDescription() {
    if (serviceDocumetation == null) {
      return "<p>No documentation available</p>";
    } else {
      return serviceDocumetation.shortDescription();
    }
  }

  /**
   * @return The url of the documentation-servlet (if any)
   */
  public String getUrl() {
    if (serviceDocumetation == null) {
      return "";
    } else {
      return serviceDocumetation.url();
    }
  }

  public boolean isDocumentationServlet() {
    if (serviceDocumetation == null) {
      return false;
    } else {
      if (!serviceDocumetation.url().equals("")) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServletDocumentation) {
      return getName().equals(((ServletDocumentation) obj).getName());
    }
    return super.equals(obj);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(ServletDocumentation o) {
    return getName().compareTo(o.getName());
  }

  /**
   * @return the Key used for the ServletDocumention in the map of registered
   *         documentation objects.
   */
  public String getKey() {
    if (serviceDocumetation != null && serviceDocumetation.ignore()) {
      return null;
    }
    return String.valueOf(service.getClass().getName());
  }

}
