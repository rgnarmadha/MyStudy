package org.sakaiproject.nakamura.batch;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import junit.framework.Assert;

import static org.mockito.Mockito.verify;

import org.apache.sling.api.SlingConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.batch.WidgetService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

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

/**
 *
 */
public class WidgetEventHandlerTest {

  private WidgetEventHandler handler;

  @Mock
  private WidgetService widgetService;

  @Before
  public void setUp() throws IOException {
    widgetService = mock(WidgetService.class);
    List<String> folders = Arrays.asList(new String[] { "/widgets" });
    when(widgetService.getWidgetFolders()).thenReturn(folders);

    handler = new WidgetEventHandler();
    handler.widgetService = widgetService;
  }

  @Test
  public void testHandleWidgetEvent() {
    Dictionary<String, Object> dict = new Hashtable<String, Object>();
    String path = "/widgets/twitter/foo";
    dict.put(SlingConstants.PROPERTY_PATH, path);
    Event event = new Event(SlingConstants.TOPIC_RESOURCE_ADDED, dict);
    handler.handleEvent(event);
    verify(widgetService).updateWidget(path);
  }

  @Test
  public void testHandleRandomEvent() {
    Dictionary<String, Object> dict = new Hashtable<String, Object>();
    String path = "/some/path/not/in/the/widgets/space";
    dict.put(SlingConstants.PROPERTY_PATH, path);
    Event event = new Event(SlingConstants.TOPIC_RESOURCE_ADDED, dict);
    handler.handleEvent(event);
    try {
      verify(widgetService).updateWidget(path);
      Assert.fail("This should not have triggered a widget update.");
    } catch (Throwable e) {

    }
  }

}
