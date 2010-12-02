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
package org.sakaiproject.nakamura.api.registry;

/**
 * Some components have a lifecycle beyond what is supported by OSGi. Anything inside a
 * bundle can implement this interface. To register its lifecycle with the system it must
 * add itself to the lifecycle registry using the RegistryUtils.addComponent() method.
 * When the lifecycle component starts it will perform all the lifecycle operations on
 * components that have been registered for lifecycle events. Lifecycles are registered
 * with a priority that is used during processing, higher priority services are started
 * first and stopped last.
 */
public interface ComponentLifecycle extends Provider<String> {

  /**
   * initialize the component
   */
  void init();

  /**
   * destroy the component.
   */
  void destroy();

}
