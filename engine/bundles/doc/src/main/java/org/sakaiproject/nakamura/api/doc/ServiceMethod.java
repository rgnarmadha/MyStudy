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
 * Documentation for methods implemented by the service.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceMethod {
  /**
   * @return the name of the method, GET, POST, PUT etc
   */
  String name() default "Please ask the developer to document";

  /**
   * @return description of the service method. Each element of the array will be displayed in its own paragraph.
   */
  String[] description() default "Please as the developer to document";

  /**
   * @return an array of parameters used by the service method.
   */
  ServiceParameter[] parameters() default {};
  
  ServiceResponse[] response() default {};

}
