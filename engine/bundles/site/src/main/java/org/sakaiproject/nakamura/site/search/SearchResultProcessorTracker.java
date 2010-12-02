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
package org.sakaiproject.nakamura.site.search;

import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SearchResultProcessorTracker extends ServiceTracker {

  private static final String SAKAI_SEACH_RESOURCETYPE = "sakai.seach.resourcetype";
  private Map<String, SearchResultProcessor> processors = new ConcurrentHashMap<String, SearchResultProcessor>();

  /**
   * @param context
   * @param customizer
   */
  public SearchResultProcessorTracker(BundleContext context) {
    super(context, SearchResultProcessor.class.getName(), null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    Object service = super.addingService(reference);
    if (service instanceof SearchResultProcessor) {
      SearchResultProcessor processor = (SearchResultProcessor) context
          .getService(reference);

      String[] processorNames = OsgiUtil.toStringArray(reference
          .getProperty(SAKAI_SEACH_RESOURCETYPE));

      putProcessor(processor, processorNames);

    }
    return service;
  }

  protected void putProcessor(SearchResultProcessor processor,
      String[] processorNames) {
    if (processorNames != null) {
      for (String processorName : processorNames) {
        processors.put(processorName, processor);
      }
    }
  }
  
  protected void removeProcessor(String[] processorNames) {
    if (processorNames != null) {
      for (String processorName : processorNames) {
        processors.remove(processorName);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
   *      java.lang.Object)
   */
  @Override
  public void removedService(ServiceReference reference, Object service) {
    if (service instanceof SearchResultProcessor) {
      String[] processorNames = OsgiUtil.toStringArray(reference
          .getProperty(SAKAI_SEACH_RESOURCETYPE));

      removeProcessor(processorNames);
    }
    super.removedService(reference, service);
  }

  /**
   * @return all the search result processors.
   */
  public Map<String, SearchResultProcessor> getSearchResultProcessors() {
    return processors;
  }

  /**
   * Get a search result processor by the sling:resourceType.
   * 
   * @param type
   *          The sling:resourceType that a processor associates itself with.
   * @return The search result processor.
   */
  public SearchResultProcessor getSearchResultProcessorByType(String type) {
    return processors.get(type);
  }

}
