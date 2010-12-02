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
package org.sakaiproject.nakamura.eventexplorer;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;

import java.util.Map;

@Component(immediate = true, enabled = true, metatype = true)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Handles incoming JMS messages.") })
public class JmsRouteBuilder extends RouteBuilder {

  @Property(description = "The name of the queue", value = {
      "org/apache/sling/api/resource/Resource/ADDED, org/apache/sling/api/resource/Resource/REMOVED",
      "org/apache/sling/api/resource/Resource/CHANGED",
      "org/apache/sling/api/resource/ResourceProvider/ADDED",
      "org/apache/sling/api/resource/ResourceProvider/REMOVED" })
  static final String JMS_TOPIC_NAMES = "jms.topic.names";

  private String[] topics;

  @Activate
  protected void activate(Map<?, ?> properties) {
    topics = (String[]) properties.get(JMS_TOPIC_NAMES);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.camel.builder.RouteBuilder#configure()
   */
  @Override
  public void configure() throws Exception {
    // Our SakaiMessageProcessor will handle all incoming messages.
    Processor processor = new SakaiMessageProcessor();

    // We handle all the topics that are defined in the system console.
    for (String topic : topics) {
      from(topic).process(processor);
    }
  }
}
