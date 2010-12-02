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
@ServiceDocumentation(name = "Default GET Servlet", 
    description = "Provides the default GET functionality for all requests that are not handled by other " +
    		"servlets in the system. After the URL is resolved into a Resource this servlet is resolved as the " +
    		"Servlet that will handle the request if no other servlets match. If the Resource has no extensions, or selectors and " +
    		"has a body, as in the case of n nt:file node, then the body is sent as a response. If the Resource is a folder" +
    		"then no response is sent. Where the request includes extensions, the properties of the node are sent in response in " +
    		"the serailisation form that matches that extension, these serializations include json, html, xml however other " +
    		"serializations may have been added to the Sling server ",
    shortDescription="Default servlet for handling GET requests ",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default",
        selectors={
          @ServiceSelector
        },
        extensions={
        @ServiceExtension(name="txt", 
            description={"Serialization of the node properties in text form",
            "<pre>** Resource dumped by PlainTextRendererServlet**\n" +
            "Resource path:/_user\n" +
            "Resource metadata: {sling.resolutionPath=/_user, sling.resolutionPathInfo=.txt}\n" +
            "Resource type: sling:Folder\n" +
            "Resource super type: -\n" +
            "\n" +
            "** Resource properties **\n" +
            "jcr:created: java.util.GregorianCalendar[time=1258327123114,areFieldsSet=true,areAllFieldsSet=true,lenient=false,zone=sun.util.calendar.ZoneInfo[id=\"GMT\",offset=0,dstSavings=0,useDaylight=false,transitions=0,lastRule=null],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2009,MONTH=10,WEEK_OF_YEAR=47,WEEK_OF_MONTH=3,DAY_OF_MONTH=15,DAY_OF_YEAR=319,DAY_OF_WEEK=1,DAY_OF_WEEK_IN_MONTH=3,AM_PM=1,HOUR=11,HOUR_OF_DAY=23,MINUTE=18,SECOND=43,MILLISECOND=114,ZONE_OFFSET=0,DST_OFFSET=0]\n" +
            "jcr:primaryType: sling:Folder" +
            "</pre>"}),
        @ServiceExtension(name="json",
            description={"Serialization of the node properties in json form",
            "<pre>{\n" +
            "   \"jcr:created\":\"Sun Nov 15 2009 17:18:43 GMT-0600\",\n" +
            "   \"jcr:primaryType\":\"sling:Folder\"\n" +
            "}</pre>"}),
        @ServiceExtension(name="html",
                description={"Serialization of the node properties in html form",
                "<pre>" +
                "&lt;?xml version=&quote;1.0&quote; encoding=&quote;utf-8&quote;?&gt; \n"+
                "&lt;!DOCTYPE html PUBLIC &quote;-//W3C//DTD XHTML 1.1//EN&quote; \n"+
                "    &quote;http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd&quote;&gt; \n"+
                "&lt;html xmlns=&quote;http://www.w3.org/1999/xhtml&quote; xml:lang=&quote;en&quote;&gt; \n"+
                "&lt;head&gt;&lt;meta http-equiv=&quote;content-type&quote; content=&quote;text/html; charset=utf-8&quote; /&gt;&lt;/head&gt; \n"+
                "&lt;body&gt; \n"+
                "&lt;h1&gt;Resource dumped by HtmlRendererServlet&lt;/h1&gt; \n"+
                "&lt;p&gt;Resource path: &lt;b&gt;/_user&lt;/b&gt;&lt;/p&gt; \n"+
                "&lt;p&gt;Resource metadata: &lt;b&gt;{sling.resolutionPath=/_user, sling.resolutionPathInfo=.html}&lt;/b&gt;&lt;/p&gt; \n"+
                "&lt;p&gt;Resource type: &lt;b&gt;sling:Folder&lt;/b&gt;&lt;/p&gt; \n"+
                "&lt;p&gt;Resource super type: &lt;b&gt;-&lt;/b&gt;&lt;/p&gt; \n"+
                "&lt;h2&gt;Resource properties&lt;/h2&gt; \n"+
                "&lt;p&gt; \n"+
                "jcr:created: &lt;b&gt;java.util.GregorianCalendar[time=1258327123114,areFieldsSet=true,areAllFieldsSet=true,lenient=false,zone=sun.util.calendar.ZoneInfo[id=&quote;GMT&quote;,offset=0,dstSavings=0,useDaylight=false,transitions=0,lastRule=null],firstDayOfWeek=1,minimalDaysInFirstWeek=1,ERA=1,YEAR=2009,MONTH=10,WEEK_OF_YEAR=47,WEEK_OF_MONTH=3,DAY_OF_MONTH=15,DAY_OF_YEAR=319,DAY_OF_WEEK=1,DAY_OF_WEEK_IN_MONTH=3,AM_PM=1,HOUR=11,HOUR_OF_DAY=23,MINUTE=18,SECOND=43,MILLISECOND=114,ZONE_OFFSET=0,DST_OFFSET=0]&lt;/b&gt;&lt;br /&gt; \n"+
                "jcr:primaryType: &lt;b&gt;sling:Folder&lt;/b&gt;&lt;br /&gt; \n"+
                "&lt;/p&gt; \n"+
                "&lt;/body&gt;&lt;/html&gt; \n"+
                "</pre>"
        }),
        @ServiceExtension(name="xml",
            description={"Serialization of the node properties in xml form",
            "<pre>" +
            "&lt;?xml version=&quote1.0&quote encoding=&quoteUTF-8&quote?&gt; \n" +
            "&lt;_user xmlns:mix=&quotehttp://www.jcp.org/jcr/mix/1.0&quote xmlns:nt=&quotehttp://www.jcp.org/jcr/nt/1.0&quote\n" +
            "  xmlns:fn_old=&quotehttp://www.w3.org/2004/10/xpath-functions&quote\n" +
            "  xmlns:sling=&quotehttp://sling.apache.org/jcr/sling/1.0&quote\n" +
            "  xmlns:fn=&quotehttp://www.w3.org/2005/xpath-functions&quote\n" +
            "  xmlns:sakai=&quotehttp://www.sakaiproject.org/nakamura/2.0&quote xmlns:xs=&quotehttp://www.w3.org/2001/XMLSchema&quote\n" +
            "  xmlns:sv=&quotehttp://www.jcp.org/jcr/sv/1.0&quote xmlns:rep=&quoteinternal&quote\n" +
            "  xmlns:jcr=&quotehttp://www.jcp.org/jcr/1.0&quote jcr:primaryType=&quotesling:Folder&quote\n" +
            "  jcr:created=&quote2009-11-15T23:18:43.114Z&quote&gt;\n" +
            "&lt;/_user&gt;\n" +
            "</pre>"
        })
    }), 
    methods = { 
         @ServiceMethod(name = "GET", 
             description = {"Responds in accordance witht he <a href=\"http://www.ietf.org/rfc/rfc2616.txt\">HTTP 1.1 Spec RFC2616</a> in general and specificly section 9.3, reproduced below " +
             		" ", 
             		"9.3 GET",
   "The GET method means retrieve whatever information (in the form of an " +
   "entity) is identified by the Request-URI. If the Request-URI refers " +
   "to a data-producing process, it is the produced data which shall be " +
   "returned as the entity in the response and not the source text of the " +
   "process, unless that text happens to be the output of the process. ",
   "The semantics of the GET method change to a \"conditional GET\" if the " +
   "request message includes an If-Modified-Since, If-Unmodified-Since, " +
   "If-Match, If-None-Match, or If-Range header field. A conditional GET " +
   "method requests that the entity be transferred only under the " +
   "circumstances described by the conditional header field(s). The " +
   "conditional GET method is intended to reduce unnecessary network " +
   "usage by allowing cached entities to be refreshed without requiring " +
   "multiple requests or transferring data already held by the client.",
   
   "The semantics of the GET method change to a \"partial GET\" if the " +
   "request message includes a Range header field. A partial GET requests " +
   "that only part of the entity be transferred, as described in section " +
   "14.35. The partial GET method is intended to reduce unnecessary " +
   "network usage by allowing partially-retrieved entities to be " +
   "completed without transferring data already held by the client.",
   "The response to a GET request is cacheable if and only if it meets " +
   "the requirements for HTTP caching described in section 13.",
   "See section 15.1.3 for security considerations when used for forms."},
              response= {
           @ServiceResponse(code=200,description="Either the body of the resource if the resource has a content child as with nt:file nodes, or a serialization of the node properites."),
           @ServiceResponse(code=404,description="The resource does not exist"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })

public class Doc_org_apache_sling_servlets_get_impl_DefaultGetServlet {

}
