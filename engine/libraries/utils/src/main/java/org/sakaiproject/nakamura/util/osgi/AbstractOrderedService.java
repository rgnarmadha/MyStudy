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
package org.sakaiproject.nakamura.util.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
/**
 * <p>
 * Provides a mechanism where services can bind and unbind such that they are available as
 * an ordered list. This class takes care of the ordering of the list and the
 * synchronization issues surrounding service registration. In
 * addition concrete classes must provide a sorting mechanism in getComparitor.
 * </p><p>
 * 
 * The class also provides a default implementation of the BoundService, notifying listeners of changes to the list.
 * </p>
 */
public abstract class AbstractOrderedService<T> implements BoundService {


  /**
   * The set of services, to be ordered.
   */
  private Map<T, Map<String, Object>> serviceSet = new HashMap<T, Map<String, Object>>();

  /**
   * A list of listeners that have actions that need to be performed when processors are
   * registered or de-registered.
   */
  private List<BindingListener> listeners = new ArrayList<BindingListener>();


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.util.osgi.BoundService#addListener(org.sakaiproject.nakamura.util.osgi.BindingListener)
   */
  public void addListener(BindingListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
      listeners.add(listener);
    }
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.util.osgi.BoundService#removeListener(org.sakaiproject.nakamura.util.osgi.BindingListener)
   */
  public void removeListener(BindingListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);
    }
  }

  /**
   * Notifies registered listeners of a change to the list so that they can invoke any
   * additional actions they deem necessary.
   */
  private void notifyNewList() {
    synchronized (listeners) {
      for (BindingListener listener : listeners) {
        listener.notifyBinding();
      }
    }

  }

  /**
   * SCR Integration, bind a service.
   * 
   * @param service
   */
  protected void addService(T service, Map<String, Object> properties) {
    synchronized (serviceSet) {
      serviceSet.put(service, properties);
      createNewSortedList();
      notifyNewList();
    }
  }

  /**
   * SCR integration, unbind a service.
   * 
   * @param service
   */
  protected void removeService(T service, Map<String, Object> properties) {
    
    synchronized (serviceSet) {
      serviceSet.remove(service);
      createNewSortedList();
      notifyNewList();
    }
  }

  /**
   * Generate a new sorted list.
   */
  private void createNewSortedList() {
    List<T> serviceList = new ArrayList<T>(serviceSet.keySet());
    Collections.sort(serviceList, getComparator(serviceSet)); 
    saveArray(serviceList);
  }

  /**
   * @param serviceList
   */
  protected abstract void saveArray(List<T> serviceList);

  /**
   * @return a compartator suitable for sorting the list of services.
   */
  protected abstract Comparator<? super T> getComparator(final Map<T, Map<String, Object>> propertiesMap);
}
