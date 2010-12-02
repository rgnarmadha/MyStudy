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
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

/**
 *
 */
@ServiceDocumentation(name = "Json Query Servlet", 
    description = "A Safe (no modification) service that renders output in json format. ",
    shortDescription="A servlet that renders search output as json. ",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default",
        selectors={
          @ServiceSelector(name="query", description="Binds to the query get mechanims")
        },
        extensions={
        @ServiceExtension(name="json",
            description={"Serialization of the node properties in json form"})
    }), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {
                 "Processes a Query on a URL itentified by the resource in terms of the resource. " +
                 "The query is passed to the resource for execution in its context. By default, with " +
                 "JCR this translates into performing a sub path JCR based query on the resource."},
             parameters={
             @ServiceParameter(name="statement", description="a query string in JCR XPath or JCR SQL format"),
             @ServiceParameter(name="queryType", description="defines the format of the statement either sql or xpath, defaults to xpath"),
             @ServiceParameter(name="offset", description="The offset into the result set."),
             @ServiceParameter(name="rows", description="The number of rows to return."),
             @ServiceParameter(name="property", description="A multi value parameter defining the properties to be returned. If the column name is rep:excerpt() then the excerptPath parameter is consulted to return rep:excerpt(excerptPath), see the <a href=\"http://wiki.apache.org/jackrabbit/ExcerptProvider\" >Jackrabbit Documentation</a> for details of this function.  "),
             @ServiceParameter(name="excerptPath", description="The name of the column to excerpt (highlight with hits) ")
             
         },
        response= {
           @ServiceResponse(code=200,description="The Query response formatted as JSON."),
           @ServiceResponse(code=404,description="The resource does not exist"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_servlets_get_impl_JsonQueryServlet {

}
