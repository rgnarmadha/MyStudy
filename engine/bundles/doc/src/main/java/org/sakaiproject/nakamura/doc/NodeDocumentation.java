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
package org.sakaiproject.nakamura.doc;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.doc.DocumentationConstants;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class NodeDocumentation {

  public static final String TITLE = "sakai:title";
  public static final String DESCRIPTION = "sakai:description";
  public static final String PARAMETERS = "sakai:parameters";
  public static final String RESPONSE = "sakai:response";
  public static final String SHORT_DESCRIPTION = "sakai:shortDescription";
  private boolean documentationNode;
  private String title;
  private String[] description;
  private NodeDocumentationParameter[] parameters;
  private String[] response;
  private String path;
  private String shortDescription;
  private Node node;

  /**
   * Create a SearchDocumentation object from a search node.
   * 
   * @param node
   *          The search node
   * @throws RepositoryException
   */
  public NodeDocumentation(Node node) throws RepositoryException {
    if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
        && node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            .getString().equals("sakai/search")) {
      setDocumentationNode(true);
    }
    if (node.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
        && node.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            .getString().equals("sakai/proxy")) {
      setDocumentationNode(true);
    }
    setPath(node.getPath());
    if (node.hasProperty(TITLE)) {
      setTitle(node.getProperty(TITLE).getString());
    }
    if (node.hasProperty(DESCRIPTION)) {
      Value[] vals = JcrUtils.getValues(node, DESCRIPTION);
      String[] description = new String[vals.length];
      for (int i = 0; i < vals.length; i++) {
        description[i] = vals[i].getString();
      }
      setDescription(description);
    }
    if (node.hasProperty(PARAMETERS)) {
      Value[] vals = JcrUtils.getValues(node, PARAMETERS);
      NodeDocumentationParameter[] parameters = new NodeDocumentationParameter[vals.length];
      for (int i = 0; i < parameters.length; i++) {
        parameters[i] = new NodeDocumentationParameter(vals[i]);
      }
      setParameters(parameters);
    }
    if (node.hasProperty(RESPONSE)) {
      Value[] vals = JcrUtils.getValues(node, RESPONSE);
      String[] response = new String[vals.length];
      for (int i = 0; i < vals.length; i++) {
        response[i] = vals[i].getString();
      }
      setResponse(response);
    }
    if (node.hasProperty(SHORT_DESCRIPTION)) {
      setShortDescription(node.getProperty(SHORT_DESCRIPTION).getString());
    }
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title
   *          the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the description
   */
  public String[] getDescription() {
    String[] r = null;
    if (description != null) {
      r = new String[description.length];
      System.arraycopy(description, 0, r, 0, description.length);
    }
    return r;
  }

  /**
   * @param description
   *          the description to set
   */
  @SuppressWarnings(justification = "Annotations fields are immutable", value = { "EI_EXPOSE_REP2" })
  public void setDescription(String[] description) {
    this.description = description;
  }

  /**
   * @return the parameters
   */
  public NodeDocumentationParameter[] getParameters() {
    NodeDocumentationParameter[] r;
    if (parameters != null) {
      r = new NodeDocumentationParameter[parameters.length];
      System.arraycopy(parameters, 0, r, 0, parameters.length);
    } else {
      r = new NodeDocumentationParameter[0];
    }
    return r;
  }

  /**
   * @param parameters
   *          the parameters to set
   */
  @SuppressWarnings(justification = "Annotations fields are immutable", value = { "EI_EXPOSE_REP2" })
  public void setParameters(NodeDocumentationParameter[] parameters) {
    this.parameters = parameters;
  }

  /**
   * @return the response
   */
  public String[] getResponse() {
    String[] r = null;
    if (response != null) {
      r = new String[response.length];
      System.arraycopy(response, 0, r, 0, response.length);
    }
    return r;
  }

  /**
   * @param response
   *          the response to set
   */
  @SuppressWarnings(justification = "Annotations fields are immutable", value = { "EI_EXPOSE_REP2" })
  public void setResponse(String[] response) {
    this.response = response;
  }

  /**
   * @return the path
   */
  public String getPath() {
    return path;
  }

  /**
   * @param path
   *          the path to set
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * @param shortDescription
   *          the shortDescription to set
   */
  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  /**
   * @return the shortDescription
   */
  public String getShortDescription() {
    return shortDescription;
  }

  /**
   * @param node
   *          the node to set
   */
  public void setNode(Node node) {
    this.node = node;
  }

  /**
   * @return the node
   */
  public Node getNode() {
    return node;
  }

  /**
   * @param searchNode
   *          the searchNode to set
   */
  public void setDocumentationNode(boolean searchNode) {
    this.documentationNode = searchNode;
  }

  /**
   * @return the searchNode
   */
  public boolean isDocumentationNode() {
    return documentationNode;
  }

  /**
   * @param writer
   * @param description
   */
  private void sendDescription(PrintWriter writer, String[] description) {
    if (description == null) {
      writer.append("<p class=\"");
      writer.append(DocumentationConstants.CSS_CLASS_NODOC);
      writer.append("\">No description has been provided by the developer, tut tut!</p>");
    } else {
      for (String desc : description) {
        writer.append("<p>");
        writer.append(desc);
        writer.append("</p>");
      }
    }
  }

  /**
   * @param writer
   * @param parameters
   */
  private void writeParameters(PrintWriter writer, NodeDocumentationParameter[] parameters) {

    writer.append("<ul class=\"").append(DocumentationConstants.CSS_CLASS_PARAMETERS)
        .append("\">");
    if (parameters == null || parameters.length == 0) {
      writer.append("<li>No parameters required</li>");
    } else {
      for (NodeDocumentationParameter par : parameters) {
        writer.append("<li>");
        writer.append("<span class=\"");
        writer.append(DocumentationConstants.CSS_CLASS_PARAMETER_NAME);
        writer.append("\">");
        writer.append(par.getName());
        writer.append("</span>");
        writer.append("<span class=\"");
        writer.append(DocumentationConstants.CSS_CLASS_PARAMETER_DESCRIPTION);
        writer.append("\">");
        writer.append(par.getDescription());
        writer.append("</span>");
        writer.append("</li>");
      }
    }
    writer.append("</ul>");

  }

  /**
   * Print all the information about a search node
   * 
   * @param writer
   */
  public void send(PrintWriter writer) {

    if (!isDocumentationNode()) {
      writer.append("<p>This is not a search node!</p>");
    } else {
      writer.append("<h3>Description</h3><p>");
      sendDescription(writer, getDescription());

      writer.append("<h3>Path</h3>");
      writer.append("<p class=\">").append(DocumentationConstants.CSS_CLASS_PATH).append(
          "\">");
      writer.append(getPath());
      writer.append("</p>");

      writer.append("<h3>Parameters</h3>");
      writeParameters(writer, getParameters());

      writer.append("<h3>Response</h3>");
      sendDescription(writer, getResponse());

    }
  }

}
