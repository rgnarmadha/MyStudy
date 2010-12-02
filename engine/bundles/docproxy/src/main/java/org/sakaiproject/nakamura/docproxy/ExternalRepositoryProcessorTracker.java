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
package org.sakaiproject.nakamura.docproxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class ExternalRepositoryProcessorTracker extends ServiceTracker {

  private Map<String, ExternalRepositoryProcessor> processors = new ConcurrentHashMap<String, ExternalRepositoryProcessor>();

  /**
   * @param bundleContext
   * @param className
   * @param customizer
   */
  public ExternalRepositoryProcessorTracker(BundleContext bundleContext,
      String className, ServiceTrackerCustomizer customizer) {
    super(bundleContext, className, customizer);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    Object service = super.addingService(reference);
    if (service instanceof ExternalRepositoryProcessor) {
      ExternalRepositoryProcessor proc = (ExternalRepositoryProcessor) service;
      String type = proc.getType();
      if (type != null) {
        putProcessor(proc, type);
      }
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
    if (service instanceof ExternalRepositoryProcessor) {
      ExternalRepositoryProcessor proc = (ExternalRepositoryProcessor) service;
      String type = proc.getType();
      if (type != null) {
        removeProcessor(type);
      }
    }
  }

  /**
   * Gets a processor by it's type name.
   * 
   * @param type
   * @return
   */
  public ExternalRepositoryProcessor getProcessorByType(String type) {
    return processors.get(type);
  }

  protected void putProcessor(ExternalRepositoryProcessor processor, String type) {
    processors.put(type, processor);
  }

  protected void removeProcessor(String type) {
    processors.remove(type);
  }

}
