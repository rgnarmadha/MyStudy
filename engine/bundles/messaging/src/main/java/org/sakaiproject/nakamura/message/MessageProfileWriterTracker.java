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
package org.sakaiproject.nakamura.message;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.sakaiproject.nakamura.api.message.MessageProfileWriter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all the services who are able to write out profile information for a recipient
 * bound to their type.
 */
public class MessageProfileWriterTracker extends ServiceTracker {

  private Map<String, MessageProfileWriter> writers = new ConcurrentHashMap<String, MessageProfileWriter>();

  /**
   * @param context
   */
  public MessageProfileWriterTracker(BundleContext context) {
    super(context, MessageProfileWriter.class.getName(), null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    Object service = super.addingService(reference);
    if (service instanceof MessageProfileWriter) {
      MessageProfileWriter writer = (MessageProfileWriter) context.getService(reference);
      putWriter(writer);

    }
    return service;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
   *      java.lang.Object)
   */
  @Override
  public void removedService(ServiceReference reference, Object service) {
    if (service instanceof MessageProfileWriter) {
      MessageProfileWriter writer = (MessageProfileWriter) service;
      removeWriter(writer.getType());
    }
    super.removedService(reference, service);
  }

  protected void putWriter(MessageProfileWriter writer) {
    writers.put(writer.getType(), writer);
  }

  protected void removeWriter(String type) {
    writers.remove(type);
  }

  /**
   * @return all the search result processors.
   */
  public Map<String, MessageProfileWriter> getMessageProfileWriters() {
    return writers;
  }

  /**
   * Get a search result processor by the sling:resourceType.
   * 
   * @param type
   *          The sling:resourceType that a processor associates itself with.
   * @return The search result processor.
   */
  public MessageProfileWriter getMessageProfileWriterByType(String type) {
    return writers.get(type);
  }

}
