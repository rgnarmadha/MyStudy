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
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

/**
 * 
 */
@ServiceDocumentation(name = "Delete ACE Servlet", 
    description = "Deletes a set of ACE's on a node. This servlet takes a set of principals and " +
    		"deletes all matching ACE's for those principals on the node.",
    shortDescription="Deletes a set of ACE's on a node.",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default"
    ), 
    methods = { 
         @ServiceMethod(name = "POST", 
             description = {
                 "Processes the list of princiapls for the resource."
             },
             parameters={
             @ServiceParameter(name=":applyTo", description="A list of principal ID's to be deleted from the ACL on the node.")
         },
        response = {
           @ServiceResponse(code=304,description="A redirect to the reource to list the access"),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_jcr_jackrabbit_accessmanager_post_DeleteAcesServlet {

}
