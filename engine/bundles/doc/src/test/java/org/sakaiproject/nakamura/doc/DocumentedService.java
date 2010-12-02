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
package org.sakaiproject.nakamura.doc;

import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

/**
 * Testing the documentation annotations.
 */
@ServiceDocumentation(
  name = "DocmentedService", 
  shortDescription = "shortDescription ..",
  description = {
    "This service has been documented using annotations, this is really just a test ",
    "of what is possible. I am using annotations to ensure that this information is ",
    "available at runtime so that we can extract it and processes it automatically",
    "Perhapse this inst the right way of doing this, and OSGi Properties are going to be"
        + "a better way of exposing this information" }, 
  bindings = {
    @ServiceBinding(
        type = BindingType.TYPE, 
        bindings = { "sakai/connectionStore" }, 
        selectors = @ServiceSelector(name = "invite", description = {"invite"}),
        extensions = @ServiceExtension(name = "json", description = {"JSON"})
     ),
     @ServiceBinding(type = BindingType.PATH, bindings = { "/system/documentationTest" })
  }, 
  methods = {
    @ServiceMethod(
        name = "GET", 
        description = "Get Responds with a page of documentation, depending on the "
        + "type of extension there will be different serializations of the output of the resource,"
        + "a .json extention will take the node properties and serialize them into a json tree. ", 
        parameters = { 
            @ServiceParameter(name = "any parameter not starting with a :", description = { "There are no parameters to the get" }) 
        }
    ),
    @ServiceMethod(
        name = "POST", 
        description = "Does nothing") 
  },
  url = "some value that makes this class a 'document servlet'."
)
public class DocumentedService extends SlingSafeMethodsServlet {

  // Class to test our annotations.
  
  private static final long serialVersionUID = -8484041094583306737L;
}
