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
package org.sakaiproject.nakamura.doc.sling;

import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

/**
 *
 */
@ServiceDocumentation(name = "Login Servlet", 
    description = "" +
        "The Login servlet provides and end point for performing login operations ",
    shortDescription="The Sling Default Post servlet implements the Sling Client Protocol",
    bindings = @ServiceBinding(type = BindingType.PATH, 
        bindings = "/system/sling/login"
    ), 
    methods = { 
      @ServiceMethod(name="GET",
          description={
          "Performs a login agains the configured athenticator, responding with a 403 if the user failed to login."
      },
      response = {
              @ServiceResponse(code=200,description="The login operation was sucessful"),
              @ServiceResponse(code=403,description="The login was denied.")          
      }),
         @ServiceMethod(name = "POST", 
             description = {
               "Simple emits a redirect to the GET method on the same url."
         })
        })
public class Doc_org_apache_sling_engine_impl_auth_LoginServlet {

}
