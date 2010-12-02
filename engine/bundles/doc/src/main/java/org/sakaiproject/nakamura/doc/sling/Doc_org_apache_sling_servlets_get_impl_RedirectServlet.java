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
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

/**
 *
 */
@ServiceDocumentation(name = "Redirect Servlet", 
    description = "" +
    		"The <code>RedirectServlet</code> implements support for GET requests to " +
    		"resources of type <code>sling:redirect</code>. This servlet tries to " +
    		"get the redirect target by " +
    		"<ul> <li>first adapting the resource to a {@link ValueMap} and trying " +
    		"to get the property <code>sling:target</code>.</li> " +
    		"<li>The second attempt is to access the resource <code>sling:target</code> " +
    		"below the requested resource and attapt this to a string.</li> " +
    		"<p> " +
    		"If there is no value found for <code>sling:target</code> a 404 (NOT FOUND) " +
    		"status is " +
    		"sent by this servlet. Otherwise a 302 (FOUND, temporary redirect) status is " +
    		"sent where the target is the relative URL from the current resource to the " +
    		"target resource. Selectors, extension, suffix and query string are also " +
    		"appended to the redirect URL.",
    shortDescription="A servlet that redirects requests to sling:redirect nodes. ",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling:redirect",
        extensions = {
           @ServiceExtension(name="json", description="If the extension is json, the properties of the node are sent as a json tree") 
        }  
    ), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {
                 "Processes the GET to a resource of type sling:redirect"
             },
        response = {
           @ServiceResponse(code=304,description="A redirect to the target."),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_servlets_get_impl_RedirectServlet {

}
