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
package org.sakaiproject.nakamura.auth.trusted;

import org.sakaiproject.nakamura.api.memory.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public class LocalCache<T>  implements Cache<Object> {

  private HashMap<String, T> m = new HashMap<String, T>();
  /**
   * 
   */
  private static final long serialVersionUID = -7727054985595135527L;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#containsKey(java.lang.String)
   */
  public boolean containsKey(String key) {
    return m.containsKey(key);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#get(java.lang.String)
   */
  public Object get(String key) {
    return m.get(key);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#list()
   */
  public List<Object> list() {
    return new ArrayList<Object>(m.values());
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#remove(java.lang.String)
   */
  public void remove(String key) {
    m.remove(key);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#removeChildren(java.lang.String)
   */
  public void removeChildren(String key) {
    m.remove(key);
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#clear()
   */
  public void clear() {
    m.clear();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.memory.Cache#put(java.lang.String, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public Object put(String key, Object payload) {
    return m.put(key, (T) payload);
  }


}
