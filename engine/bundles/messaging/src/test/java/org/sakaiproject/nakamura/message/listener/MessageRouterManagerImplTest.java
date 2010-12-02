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
package org.sakaiproject.nakamura.message.listener;

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageRouter;

import java.util.List;

/**
 *
 */
public class MessageRouterManagerImplTest {

  private MessageRouterManagerImpl manager;
  private MessageRouter routerA;
  private MessageRouter routerB;

  @Before
  public void setUp() {
    manager = new MessageRouterManagerImpl();
    routerA = mock(MessageRouter.class);
    routerB = mock(MessageRouter.class);
    when(routerA.getPriority()).thenReturn(100);
    when(routerB.getPriority()).thenReturn(50);

    manager.addMessageRouter(routerA);
    manager.addMessageRouter(routerB);
    manager.removeMessageRouter(routerB);
    manager.addMessageRouter(routerB);
  }

  @Test
  public void testOrder() {
    List<MessageRouter> sortedRouters = manager.getSortedRouterList();

    assertEquals(100, sortedRouters.get(0).getPriority());
    assertEquals(50, sortedRouters.get(1).getPriority());

  }

}
