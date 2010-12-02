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

package org.sakaiproject.nakamura.api.rules;

import java.io.InputStream;

/**
 * Things that implement RulePackageClassloaders allow bundles to export a Package Stream
 * and a classloader keeping all internal classes internal. The name of the class is
 * registered as a source of the package and then an instance of the class will be used to
 * provide an input stream to the compiled package of rules, and the classloader to be
 * used to load the rules. This enables rules bundles to contain both compiled rules and
 * supporting model classes.
 * 
 * Anything that implements this class must have default constructor.
 * 
 */
public interface RulePackageLoader {

  /**
   * @return The input stream that represents this the Rules pacakge associated with this class.
   */
  InputStream getPackageInputStream();

  /**
   * @return the bundle classloader that can see all the classes related to the pacakge.
   */
  ClassLoader getPackageClassLoader();

}
