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

import static org.junit.Assert.assertEquals;

import static org.easymock.EasyMock.expect;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.files.LinkHandler;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;

/**
 *
 */
public class LinkHandlerTrackerTest extends AbstractEasyMockTest {

  private LinkHandlerTracker tracker;
  protected BundleContext bundleContext;
  protected ComponentContext componentContext;

  @Before
  public void setUp() {
    tracker = new LinkHandlerTracker();
  }

  @Test
  public void testAdd() throws InvalidSyntaxException {

    bundleContext = expectServiceTrackerCalls(LinkHandler.class.getName());

    componentContext = EasyMock.createMock(ComponentContext.class);

    tracker.setComponentContext(componentContext);

    ServiceReference referenceA = addProcesor(componentContext, "FooHandlerA");
    ServiceReference referenceB = addProcesor(componentContext, "FooHandlerB");

    EasyMock.replay(componentContext);

    // Add a couple of handlers.
    tracker.bindLinkHandler(referenceA);
    tracker.bindLinkHandler(referenceB);

    assertEquals(getProcessor(componentContext, referenceA), tracker
        .getProcessorByName("FooHandlerA"));
    assertEquals(getProcessor(componentContext, referenceB), tracker
        .getProcessorByName("FooHandlerB"));

    // Remove a handler
    tracker.unbindLinkHandler(referenceA);

    Iterator<LinkHandler> handlers = tracker.getProcessors().iterator();
    int count = 0;
    while (handlers.hasNext()) {
      handlers.next();
      count++;
    }
    assertEquals(1, count);

  }

  /**
   * @param componentContext2
   * @param string
   * @return
   */
  private Object getProcessor(ComponentContext componentContext,
      ServiceReference reference) {
    return componentContext.locateService(FilesConstants.LINK_HANDLER, reference);
  }

  /**
   * @param componentContext2
   * @param string
   * @return
   */
  private ServiceReference addProcesor(ComponentContext context, String handlerName) {
    ServiceReference reference = EasyMock.createMock(ServiceReference.class);
    expect(reference.getProperty(FilesConstants.REG_PROCESSOR_NAMES)).andReturn(
        handlerName).anyTimes();
    ;
    LinkHandler handler = new LinkHandler() {

      public void handleFile(SlingHttpServletRequest request,
          SlingHttpServletResponse response, String to) throws ServletException,
          IOException {
      }
    };

    EasyMock.expect(
        componentContext.locateService(FilesConstants.LINK_HANDLER, reference))
        .andReturn(handler).anyTimes();
    EasyMock.replay(reference);
    return reference;
  }

}
