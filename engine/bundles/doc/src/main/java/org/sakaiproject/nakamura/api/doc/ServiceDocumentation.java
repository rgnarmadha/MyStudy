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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The root documtation annotation that is intended to be applied to the class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceDocumentation {
  /**
   * @return the name of the service being documented.
   */
  String name() default "no name supplied";

  /**
   * @return an array of documentation paragraphs. Each element in the array is placed in
   *         its own paragraph.
   */
  String[] description() default {};

  /**
   * @return an array of bindings, each binding defines either a binding to path, or a
   *         binding to a resource type.
   */
  ServiceBinding[] bindings() default {};

  /**
   * @return an array of methods one for each HTTP method that is implemented by the
   *         service.
   */
  ServiceMethod[] methods() default {};

  /**
   * @return a short description used in listings of the service.
   */
  String shortDescription() default "no description supplied";

  /**
   * @return If this servlet provides a set of documentation, you can place the URL to the
   *         servlet here.
   */
  String url() default "";

  /**
   * @return is this set of description a proxy for a real servlet.
   */
  String proxy() default "";

  /**
   * @return if true, ignore this class.
   */
  boolean ignore() default false;
}
