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
 * Documentation for a parameters used by a method. This annotation is nested inside a
 * {@link ServiceMethod} annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceParameter {
  /**
   * @return the name of the parameter.
   */
  String name() default "Please ask the developer to describe the parameters of this service";

  /**
   * @return description of the parameter, each element will be placed in its own
   *         paragraph.
   */
  String[] description() default "Please ask the developer to document this parameter";
}
