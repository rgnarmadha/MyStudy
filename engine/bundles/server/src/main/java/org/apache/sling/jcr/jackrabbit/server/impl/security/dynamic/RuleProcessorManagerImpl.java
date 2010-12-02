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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class RuleProcessorManagerImpl extends ServiceTracker implements RuleProcessorManager {

  private BundleContext bundleContext;
  private Map<String,RuleProcessor> processors = new ConcurrentHashMap<String, RuleProcessor>();

  /**
   * @param bundleContext
   */
  public RuleProcessorManagerImpl(BundleContext bundleContext) {
    super(bundleContext, RuleProcessor.class.getName(),null);
    this.bundleContext = bundleContext;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    RuleProcessor processor = (RuleProcessor) bundleContext.getService(reference);
    String name = (String) reference.getProperty(RuleProcessor.SERVICE_NAME);
    processors.put(name,processor);
    return super.addingService(reference);
  }
  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
   */
  @Override
  public void removedService(ServiceReference reference, Object service) {
    String name = (String) reference.getProperty(RuleProcessor.SERVICE_NAME);
    RuleProcessor processor = processors.get(name);
    if ( service.equals(processor)) {
      processors.remove(name);
    }
    super.removedService(reference, service);
  }
  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
   */
  @Override
  public void modifiedService(ServiceReference reference, Object service) {
    String name = (String) reference.getProperty(RuleProcessor.SERVICE_NAME);
    processors.put(name,(RuleProcessor) service);
    super.modifiedService(reference, service);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.RuleProcessorManager#getRuleProcessor(java.lang.String)
   */
  public RuleProcessor getRuleProcessor(String ruleProcessor) {
    return processors.get(ruleProcessor);
  }

}
