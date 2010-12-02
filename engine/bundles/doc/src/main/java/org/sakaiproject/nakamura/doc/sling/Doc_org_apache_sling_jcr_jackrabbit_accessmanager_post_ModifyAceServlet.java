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
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

/**
 * 
 */
@ServiceDocumentation(name = "Modify ACE Servlet", 
    description = "Gets the ACL on a resource in the form of a JSON tree. The tree is a map, keyed by principal ID. " +
    		"Each element of the map containing a map keyed on granted and denied. Granted only being present if " +
    		"privilages have explicitly been granted, and denied present only if privilages have been explicitly granted. Each " +
    		"of these elements contains an array of the privileges granted or denied.",
    shortDescription="Modify ACE on a resource.",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default",
        selectors = @ServiceSelector(name="modifyAce", description=" requires a selector of resource.modifyAce.html to modify the ACE")
    ), 
    methods = { 
         @ServiceMethod(name = "POST", 
             description = {
                 "Modifies an ACE in the ACL on the Reource identified by the URL. The post will replace the ACE on the user, so " +
                 "the caller must ensure they have a current copy of the ACE, edit it locally and then submit the edited copy to this " +
                 "servlet ",
                 "<pre>" +
                 "curl -FprincipalId=ieb -Fprivilege@jcr:read=granted -Fprivilege@jcr:write=denied http://admin:admin@localhost:8080/_user/private.modifyAce.html\n" +
                 "Location: http://admin:admin@localhost:8080/_user/private.acl.html\n" +
                 "</pre>"
         },
         parameters = {
         @ServiceParameter(name="principalId",description={
            "The principal ID of the set of entries to modify on this resource." 
         }),
         @ServiceParameter(name="privilege@&lt;privilege-name&gt;", description={
             "Privieges to grant or deny are identified by parameters prefixed with <em>privilege@</em>, the remainder of the " +
             "parameter name identifies the privilege. The value of the parameter is either granted, meaning the privilege is " +
             "granted or denied mening the privilege is denied. "
         })
         },
        response = {
             @ServiceResponse(code=304,description="On sucess a redirect to get the ACL is sent."),
             @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
             @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_jcr_jackrabbit_accessmanager_post_ModifyAceServlet {

}
