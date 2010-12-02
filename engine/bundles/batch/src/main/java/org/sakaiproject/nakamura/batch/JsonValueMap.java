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
package org.sakaiproject.nakamura.batch;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Utility function to convert JSON data to a map.
 */
public class JsonValueMap implements ValueMap {

  private Map<String, Object> map;

  /**
   * Convert an {@link InputStream} to a {@link ValueMap}.
   * 
   * @param stream
   *          The stream to convert.
   * @throws UnsupportedEncodingException
   *           This stream could not be convert with UTF-8.
   * @throws JSONException
   *           The content of this stream is not a valid JSON string.
   * @throws IOException
   *           Something went wrong trying to read the stream.
   */
  public JsonValueMap(InputStream stream) throws UnsupportedEncodingException,
      JSONException, IOException {
    this(IOUtils.readFully(stream, "UTF-8"));
  }

  /**
   * Convert a {@link String} to a {@link ValueMap}.
   * 
   * @param content
   *          The string to convert to a map.
   * @throws JSONException
   *           The string is not a valid JSON string.
   */
  public JsonValueMap(String content) throws JSONException {
    this(new JSONObject(content));
  }

  /**
   * Convert a {@link JSONObject} to a {@link ValueMap}.
   * 
   * @param json
   *          The object to convert to a map.
   */
  public JsonValueMap(JSONObject json) {
    map = new HashMap<String, Object>();
    Iterator<String> iterator = json.keys();
    while (iterator.hasNext()) {
      String key = iterator.next();
      map.put(key, json.opt(key));
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String name, Class<T> type) {
    return (T) map.get(name);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.resource.ValueMap#get(java.lang.String, java.lang.Object)
   */
  @SuppressWarnings("unchecked")
  public <T> T get(String name, T defaultValue) {
    Object o;
    try {
      o = (T) map.get(name);
      if (o == null) {
        return defaultValue;
      }
    } catch (ClassCastException e) {
      o = defaultValue;
    }
    return (T) o;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#clear()
   */
  public void clear() {
    map.clear();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#containsKey(java.lang.Object)
   */
  public boolean containsKey(Object key) {
    return map.containsKey(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#containsValue(java.lang.Object)
   */
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#entrySet()
   */
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    return map.entrySet();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#get(java.lang.Object)
   */
  public Object get(Object key) {
    return map.get(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#isEmpty()
   */
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#keySet()
   */
  public Set<String> keySet() {
    return map.keySet();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#put(java.lang.Object, java.lang.Object)
   */
  public Object put(String key, Object value) {
    return map.put(key, value);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#putAll(java.util.Map)
   */
  public void putAll(Map<? extends String, ? extends Object> m) {
    map.putAll(m);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#remove(java.lang.Object)
   */
  public Object remove(Object key) {
    return map.remove(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#size()
   */
  public int size() {
    return map.size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Map#values()
   */
  public Collection<Object> values() {
    return map.values();
  }

}
