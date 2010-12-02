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

package org.sakaiproject.nakamura.rules;

import java.lang.ref.WeakReference;
import java.net.URL;

/**
 * This week reference classloader does not hold a classloader open and so is safe to use
 * where the classloader might be garbage collected. It will not protect against the
 * classloader being garbage collected but it will allow that to happen. When the
 * classloader is GCd a runtime exception will be emitted. Consumers should validate that
 * the classloader is current before using it.
 */
public class WeakReferenceClassloader extends ClassLoader {

  private WeakReference<ClassLoader> reference;

  public WeakReferenceClassloader(ClassLoader packageClassLoader) {
    reference = new WeakReference<ClassLoader>(packageClassLoader);
  }

  @Override
  public synchronized void clearAssertionStatus() {
    getClassloader().clearAssertionStatus();
  }

  private ClassLoader getClassloader() {
    ClassLoader cl = reference.get();
    if (cl == null) {
      throw new RuntimeException(
          "The Bundles assocaited with this classloader has been reloaded, classes are no longer available, rules and workflow needs to be reloaded.");
    }
    return cl;
  }

  @Override
  public boolean equals(Object obj) {
    return getClassloader().equals(obj);
  }

  @Override
  public URL getResource(String name) {
    return super.getResource(name);
  }

  public boolean isAvailable() {
    return getClassloader() != null;
  }
}
