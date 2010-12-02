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
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

/**
 *
 */
@ServiceDocumentation(name = "Info Servlet", 
    description = "Gets info from one of the configured info providers registerd with the system." +
    		" An info provider produces a map of information from a source, that source may consider " +
    		"the context of the request. By default a SessionInfoProvider binds to the path /system/sling/info.sessionInfo",
    shortDescription="Gets info from one of the configured info providers registered with the system ",
    bindings = @ServiceBinding(type = BindingType.PATH, 
        bindings = "/system/sling/info",
        selectors = @ServiceSelector(name="*", 
            description="The selector selects the type of " +
            		"SessionInfoProvider that is queries. eg sessionInfo is the default"),
        extensions = {
           @ServiceExtension(name="json", 
               description={"If the extension is json, the output of the info provider is rendered as json",
               "<pre>" +
               "{\"userID\":\"anonymous\",\"workspace\":\"default\"}</pre>"}),
           @ServiceExtension(name="txt", 
               description={"If the extension is txt, the output of the info provider is rendered as text",
               "<pre>" +
               "userID: anonymous\n" +
               "workspace: default\n" +
               "</pre>"
               }),
           @ServiceExtension(name="html", 
               description={"If the extension is html, or anything else, then the result of the " +
               		"info provider is rendered as html",
               		"<pre>" +
               		"&lt;!DOCTYPE HTML PUBLIC &quot;-//W3C//DTD HTML 4.01//EN&quot; &quot;http://www.w3.org/TR/html4/strict.dtd&quot;&gt; \n" +
               		"&lt;html&gt;&lt;head&gt;&lt;title&gt;Sling Info&lt;/title&gt;&lt;/head&gt; \n" +
               		"&lt;body&gt;&lt;h1&gt;Sling Info&lt;/h1&gt; \n" +
               		"&lt;table&gt; \n" +
               		"&lt;tr&gt;&lt;td&gt;userID&lt;/td&gt;&lt;td&gt;anonymous&lt;/td&gt;&lt;/tr&gt; \n" +
               		"&lt;tr&gt;&lt;td&gt;workspace&lt;/td&gt;&lt;td&gt;default&lt;/td&gt;&lt;/tr&gt; \n" +
               		"&lt;/table&gt; \n" +
               		"&lt;/body&gt; \n" +
               		"</pre>"
               		
           }) 
        }  
    ), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {
                 "Invokes the selected SessionInfoProvider and emits the appropriately formatted output."
             },
        response = {
           @ServiceResponse(code=304,description="A redirect to the target."),
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_servlets_get_impl_impl_info_SlingInfoServlet {

}
