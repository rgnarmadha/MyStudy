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
package org.sakaiproject.nakamura.api.configuration;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


/**
 *
 */
public class NakamuraConstantsTest {

  @Mock private ConfigurationListener listener;
  public NakamuraConstantsTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testNakamuraConstants() throws IOException, ConfigurationException {
    NakamuraConstants nc = new NakamuraConstants();
    Map<String, String> properties = nc.getProperties();
    nc.addListener(listener);
    Dictionary<String, Object> config = new Hashtable<String, Object>();
    nc.updated(config);
    nc.removeListener(listener);
  }
}
