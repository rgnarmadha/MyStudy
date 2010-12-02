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
package org.sakaiproject.nakamura.antixss.servlet;

import org.junit.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.owasp.validator.html.PolicyException;
import org.sakaiproject.nakamura.antixss.servlet.AntiXssServlet;
import org.sakaiproject.nakamura.api.antixss.AntiXssService;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.ServletException;

/**
 *
 */
public class AntiXssServletTest {

  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private Resource resource;
  @Mock
  private Node node;
  @Mock
  private AntiXssService antiXssService;
  @Mock
  private PropertyIterator propertyIterator;
  @Mock
  private Property propertyValue;
  @Mock
  private PropertyDefinition propertyDefinition;
  @Mock
  private Value value;

  public AntiXssServletTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testGet() throws IOException, PolicyException, ServletException,
      RepositoryException {

    AntiXssServlet antiXssServlet = new AntiXssServlet();
    antiXssServlet.antiXssService = antiXssService;
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.adaptTo(Node.class)).thenReturn(node);
    Mockito.when(antiXssService.cleanHtml(Mockito.anyString())).thenReturn("[cleanhtml]");
    Mockito.when(node.getProperties()).thenReturn(propertyIterator);
    Mockito.when(propertyIterator.hasNext()).thenReturn(true, false);
    Mockito.when(propertyIterator.nextProperty()).thenReturn(propertyValue);
    Mockito.when(propertyValue.getDefinition()).thenReturn(propertyDefinition);
    Mockito.when(propertyValue.getValue()).thenReturn(value);
    Mockito.when(value.getType()).thenReturn(PropertyType.STRING);
    Mockito.when(propertyValue.getName()).thenReturn("propname");
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    Mockito.when(response.getWriter()).thenReturn(pw);
    antiXssServlet.doGet(request, response);

    Assert
        .assertEquals(
            "{\"jcr:path\":\"[cleanhtml]\",\"jcr:name\":\"[cleanhtml]\",\"propname\":\"[cleanhtml]\"}",
            sw.toString());

  }
  
  @Test
  public void testGetNoResource() throws IOException, PolicyException, ServletException,
      RepositoryException {

    AntiXssServlet antiXssServlet = new AntiXssServlet();
    antiXssServlet.antiXssService = antiXssService;
    Mockito.when(request.getResource()).thenReturn(null);
    
    try {
      antiXssServlet.doGet(request, response);
      Assert.fail();
    } catch ( ResourceNotFoundException e) {
      
    }


  }

}
