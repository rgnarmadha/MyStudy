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
package org.sakaiproject.nakamura.api.doc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Documents a service binding for a Service, this annotation should be nested inside
 * {@link ServiceDocumentation} annotation. If used on its own it will not have any
 * effect.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceBinding {
  /**
   * @return The type of the binding, defined in {@link BindingType}, PATH type bindings
   *         define fixed paths. TYPE binding are bound to resource types.
   */
  BindingType type() default BindingType.PATH;

  /**
   * @return an array of binding locations, all of the binding type.
   */
  String[] bindings() default "";
  
  /**
   * @return a list of selectors that this servlet binds to
   */
  ServiceSelector[] selectors() default {};
  
  /**
   * @return a list of extensions that this servlet allows
   */
  ServiceExtension[] extensions() default {};
}
