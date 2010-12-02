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

/**
 *
 */
@ServiceDocumentation(name = "Default Post Servlet", 
    description = "" +
        "The Sling Default Post servlet implements the Sling Client Protocol ",
    shortDescription="The Sling Default Post servlet implements the Sling Client Protocol",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default",
        extensions = {
           @ServiceExtension(name="html", description="Post responses are sent in a standard form html encoded.") 
        }  
    ), 
    methods = { 
         @ServiceMethod(name = "POST", 
             description = {
                 "Detailed documentation of this servlet can be found at the <a href=\"http://sling.apache.org/site/manipulating-content-the-slingpostservlet-servletspost.html\">Sling Tutorial</a>, " +
                 "what follows is a brief description.",
                 "The POST servlet accepts posts to Resources identified by theur URL. If the Resource does not exist it is created. " +
                 "The request parameters of the post become the properties of the resource, except for some special properties. " +
                 "Any name that does not start with a ':' becomes a property of the node."
             },
        parameters={
           @ServiceParameter(name="*,!:*", description="all parameters except those starting with : become properties of the node."),
           @ServiceParameter(name=":*", description="all parameters starting with : are treated as command parameters."),
           @ServiceParameter(name=":*", description="all parameters starting with : are treated as command parameters."),
           @ServiceParameter(name=":operation", description={
               "not set, the resource is created or updated ",
               "delete, the resource is deleted Example:",
               "<pre>$ curl -F\":operation=delete\" http://host/content/sample</pre>",
               "move, the resource is moved",
               "copy, the resource is moved"
            }),
          @ServiceParameter(name=":applyTo", description={
              "<h2>Deleting Multiple Items</h2>",
              "By using the :applyTo request parameter it is possible to remove multiple items in one single request. " +
              "Deleting items in this way leaves you with less control, though. In addition, if a single item removal " +
              "fails, no item at all is removed. ",

              "When specifying the item(s) to be removed with the :applyTo parameter, the request resource is left " +
              "untouched (unless of course if listed in the :applyTo parameter) and only used to resolve any relative " +
              "paths in the :applyTo parameter.",

              "To for example remove the /content/page1 and /content/page2 nodes, you might use the following command line:",

              "<pre>$ curl -F\":operation=delete\" -F\":applyTo=/content/page1\" -F\":applyTo=/content/page2\" " +
              "http://host/content/sample</pre>",

              "If any resource listed in the :applyTo parameter does not exist, it is silently ignored.",
              
              "<h2>Copying Multiple Items</h2>",

              "By using the :applyTo request parameter it is possible to copy multiple items in one single request. " +
              "Copying items in this way leaves you with less control, though. In addition, if a single item copy fails, " +
              "no item at all is copied.",

              "When specifying the item(s) to be copied with the :applyTo parameter, the request resource is left " +
              "untouched (unless of course if listed in the :applyTo parameter) and only used to resolve any relative " +
              "paths in the :applyTo parameter. ",

              "To for example copy the /content/page1 and /content/page2 nodes to /content/target, you might use the " +
              " following command line:",

              "<pre>$ curl -F\":operation=copy\" -F\":applyTo=/content/page1\" -F\":applyTo=/content/page2\" " +
              "-F\":dest=/content/target/\" http://host/content/sample</pre>",

              "Please note the trailing slash character (/) in the value of the :dest parameter. This is required " +
              "for mult-item copy operations using the :applyTo parameter. The copied items are created below the " +
              "node indicated by the :dest. ",

              "If any resource listed in the :applyTo parameter does not exist, it is silently ignored. Any item " +
              "already existing at the copy destination whose name is the same as the name of an item to be copied " +
              "is silently overwritten with the source item. ",
              
              "<h2>Moving Multiple Items</h2>",

              "By using the :applyTo request parameter it is possible to move multiple items in one single request. " +
              "Moving items in this way leaves you with less control, though. In addition, if a single item move " +
              "fails, no item at all is moved. ",

              "When specifying the item(s) to be moved with the :applyTo parameter, the request resource is left " +
              "untouched (unless of course if listed in the :applyTo parameter) and only used to resolve any " +
              "relative paths in the :applyTo parameter.",

              "To for example move the /content/page1 and /content/page2 nodes to /content/target, you might use " +
              "the following command line:",

              "<pre>$ curl -F\":operation=move\" -F\":applyTo=/content/page1\" -F\":applyTo=/content/page2\" " +
              "-F\":dest=/content/target/\" http://host/content/sample</pre>",

              "Please note the trailing slash character (/) in the value of the :dest parameter. This is required " +
              "for mult-item move operations using the :applyTo parameter. The moved items are created below the " +
              "node indicated by the :dest. ",

              "If any resource listed in the :applyTo parameter does not exist, it is silently ignored. Any item " +
              "already existing at the move destination whose name is the same as the name of an item to be moved " +
              "is silently overwritten with the source item."
          }),
          @ServiceParameter(name=":nopstatus", description={
              "<h2>Null Operation</h2>",

              "Sometimes it is useful to explicitly request that nothing is to be done. The SlingPostServlet now provides " +
              "such an operation under the name nop. Apart from doing nothing, the nop operations sets the response " +
              "status to either the default 200/OK or to any status requested by the :nopstatus request parameter. ",

              "The :nopstatus request parameter must be an integral number in the range [ 100 .. 999 ]. If the parameter value " +
              "cannot be parsed to an integer or the value is outside of this range, the default status 200/OK is still set."
          }),
          @ServiceParameter(name=":dest", description={
              "To copy existing content to a new location, the copy operation is specified. This operation copies the " +
              "item addressed by the request URL to a new location indicated by the :dest parameter. The :dest parameter " +
              "is the absolute or relative path to which the resource is copied. If the path is relative it is " +
              "assumed to be below the same parent as the request resource. If it is terminated with a / character " +
              "the request resource is copied to an item of the same name under the destination path. ",

              "To illustrate the :dest parameter handling, lets look at a few examples. All examples are based " +
              "on addressing the /content/sample item:",
              "<table><tr><th>:dest Parameter</th><th>Destination Absolute Path</th></tr>"+
              "<tr><td>/content/newSample</td><td>/content/newSample</td></tr>"+
              "<tr><td>different/newSample</td><td>/content/different/newSample</td></tr>"+
              "<tr><td>/content/different/</td><td>/content/different/sample</td></tr>"+
              "<tr><td>different/</td><td>/content/different/sample</td></tr></table>",

              "If an item already exists at the location derived from the :dest parameter, the copy operation fails " +
              "unless the :replace parameter is set to true (case is ignored when checking the parameter value)."
          }),
          @ServiceParameter(name=":replace", description={"If :replace is set to true, move and copy operations will replace the content"}),
          @ServiceParameter(name=":order", description={
              "Child nodes may be ordered if the primary node type of their common parent node is defined as having orderable " +
              "child nodes. To employ such ordering, the content creation/modification, move and copy operations support the " +
              ":order parameter which apply child node ordering amongst its sibblings of the target node.",

              "The :order parameter may have the following values:",
              "<table><tr><th>Value</th><th>Description</th></tr>" +
              "<tr><td>first</td><td>Place the target node as the first amongst its sibblings</td></tr>" +
              "<tr><td>last</td><td>Place the target node as the last amongst its sibblings</td></tr>" +
              "<tr><td>before xyz</td><td>Place the target node immediately before the sibbling whose name is xyz</td></tr>" +
              "<tr><td>after xyz</td><td>Place the target node immediately after the sibbling whose name is xyz</td></tr>" +
              "<tr><td>numeric</td><td>Place the target node at the indicated numeric place amongst its sibblings where 0 is equivalent to first and 1 means the second place</td></tr></table>" ,

              "Note that simple content reordering can be requested without applying any other operations. This is easiest done " +
              "by placing a request to the resource to be reordered and just setting the :order parameter. For example to " +
              "order the /content/sample/page5 resource above its sibbling resource /content/sample/other a simple request ",

              "<pre>$ curl -F\":order=before other\" http://host/content/sample/page5</pre>",

              "does the trick. To be redirected after the reodering, the :redirect parameter may optionally also be specified."
          }),
          @ServiceParameter(name=":redirect", description={
              "nstructs the SlingPostServlet to redirect the client to the indicated location if the operation succeeds. " +
              "That is the reponse " +
              "status is set to 302/FOUND and the Location header is set to the value of the :redirect parameter."
          }),
          @ServiceParameter(name=":status", description={
              "By default the SlingPostServlet sets response status according to the status of the operation executed. " +
              "In some cases, it may be desirable to not have the real status codes (e.g. 404 or 505) but a normal 200/OK " +
              "to trick the client browser into displaying the response content generated by the SlingPostServlet. ",

              "To not send the actual response status back to the client, the :status request parameter should be set to " +
              "browser. If this parameter is not set, is empty, is set to standard or to any other value, the actual status " +
              "code is sent back to the client. "
          }),
          @ServiceParameter(name="*@*", description="Parameters with @ embeded add metadata to a property, the suffix after @ specifies the type of the property."),
          @ServiceParameter(name="*@TypeHint", description={
              "Parameters with the @TypeHint suffix may be used to force storing the named parameter in a property with the " +
              "given type. The value of the @TypeHint parameter, if applied to a parameter for a property, is the JCR " +
              "property type name. If the @TypeHint parameter is applied to a field upload parameter, the value is used " +
              "to indicate the JCR primary node type for the node into which the uploaded file is stored.",
              "If the @TypeHint ends with \"[]\", it indicates a multi-value property. A multi-value property is usually " +
              "auto-detected if there are mutliple values for the property (ie. request parameter). But if only a single value " +
              "is present in the request, the desired property type needs to be explicitly defined as multi-value by stating " +
              "@TypeHint=&lt;type&gt;[].",
              "Example: The following form sets the numeric width, the boolean checked, and the multi-valued hobbys (with 3 " +
              "values to enter) properties:",
              "<pre>" +
              "  &lt;form method=&quote;POST&quote; action=&quote;/content/page/first&quote; enctype=&quote;multipart/form-data&quote;&gt; \n"+
              "      &lt;input type=&quote;text&quote; name=&quote;width&quote; /&gt; \n"+
              "      &lt;input type=&quote;hidden&quote; name=&quote;width@TypeHint&quote; value=&quote;Long&quote; /&gt; \n"+
              "      &lt;input type=&quote;checkbox&quote; name=&quote;checked&quote; /&gt; \n"+
              "      &lt;input type=&quote;hidden&quote; name=&quote;checked@TypeHint&quote; value=&quote;Boolean&quote; /&gt; \n"+
              "      &lt;input type=&quote;text&quote; name=&quote;hobbys&quote;/&gt; \n"+
              "      &lt;input type=&quote;text&quote; name=&quote;hobbys&quote;/&gt; \n"+
              "      &lt;input type=&quote;text&quote; name=&quote;hobbys&quote;/&gt; \n"+
              "      &lt;input type=&quote;hidden&quote; name=&quote;hobbys@TypeHint&quote; value=&quote;String[]&quote; /&gt; \n"+
              "      &lt;input type=&quote;Submit&quote; /&gt; \n"+
              "  &lt;/form&gt; \n"+
              "</pre>"
              }),
          @ServiceParameter(name="*@DefaultValue", description={
              "The @DefaultValue suffixed parameter may be provided to set a property to a default value should no " +
              "value be provided in the actual parameters. Same as for normal paramaters, the @DefaultValue parameter " +
              "may have multiple values to create multi-valued properties.",

              "Example: Set the text property to a default value if the user does not provide one:",

              "<pre>" +
              "&lt;form method=&quot;POST&quot; action=&quot;/content/page/first&quot; enctype=&quot;multipart/form-data&quot;&gt; \n"+
              "    &lt;input type=&quot;text&quot; name=&quot;text&quot; /&gt; \n"+
              "    &lt;input type=&quot;hidden&quot; name=&quot;text@DefaultValue&quot; value=&quot;--- Default Value ---&quot; /&gt; \n"+
              "    &lt;input type=&quot;Submit&quot; /&gt; \n"+
              "&lt;/form&gt; \n"+
              "</pre>"
          }),
          @ServiceParameter(name="*@ValueFrom", description={
              "In some situations, an HTML form with parameters may be reused to update content. But one or more " +
              "form parameters may not comply with the names expected to be used for properties. In this case a " +
              "parameter suffixed with @ValueFrom may be set containing the name of the parameter providing the " +
              "actual data to be used. ",

              "Example: To set the property text from a form element supplied_text, you might use the following form:",

              "<pre>" +
              "&lt;form method=&quot;POST&quot; action=&quot;/content/page/first&quot; enctype=&quot;multipart/form-data&quot;&gt; \n"+
              "    &lt;input type=&quot;text&quot; name=&quot;supplied_text&quot; /&gt; \n"+
              "    &lt;input type=&quot;hidden&quot; name=&quot;./text@ValueFrom&quot; value=&quot;supplied_text&quot; /&gt; \n"+
              "    &lt;input type=&quot;Submit&quot; /&gt; \n"+
              "&lt;/form&gt; \n" +
              "</pre>",

              "To prevent storing the additional paramaters in the repository you might want to use the prefixing mechanism as " +
              "shown in the example above, where the @ValueFrom parameter is prefixed and thus the supplied_text " +
              "parameter is not used for property setting.",

              "The @ValueFrom suffixed parameter is assumed to be single-valued. If the parameter has multiple values " +
              "it is ignored completely. ",

              "The @ValueFrom suffixed parameter is also special in that there must not be a correlated parameter " +
              "without a suffix. Thus have parameters text and text@ValueFrom may have unexpected results."
          }),
          @ServiceParameter(name="*@Delete", description={
              "Sometimes it may be required to not set a property to a specific value but to just remove it while processing " +
              "the content update request. One such situation is a property filled from one or more checkboxes in an " +
              "HTML form. If none of the checkboxes are checked, no parameter is actually submitted for these checkboxes. " +
              "Hence the SlingPostServlet will not touch this property and effectively leave it untouched, while the natural " +
              "reaction would have been to remove the property.",

              "Here comes the @Delete suffixed parameter. This simply causes the indicated property be removed if it exists. " +
              "If the property does not exist, nothing more happens. The actual value of the @Delete suffixed parameter does " +
              "not care as long as the parameter is submitted. ",

              "Example: To ensure the color property is actually removed if no color has been selected, you might use the " +
              "following form: ",

              "<pre>" +
              "&lt;form method=&quot;POST&quot; action=&quot;/content/page/first&quot; enctype=&quot;multipart/form-data&quot;&gt; \n" +
              "    &lt;input type=&quot;checkbox&quot; name=&quot;color&quot; value=&quot;red&quot; /&gt; \n" +
              "    &lt;input type=&quot;checkbox&quot; name=&quot;color&quot; value=&quot;green&quot; /&gt; \n" +
              "    &lt;input type=&quot;checkbox&quot; name=&quot;color&quot; value=&quot;blue&quot; /&gt; \n" +
              "    &lt;input type=&quot;hidden&quot; name=&quot;color@Delete&quot; value=&quot;delete text&quot; /&gt;&lt;!-- actual value is ignored --&gt; \n" +
              "    &lt;input type=&quot;Submit&quot; /&gt; \n" +
              "&lt;/form&gt; \n" +
              "</pre>",
              "The @Delete suffixed parameter is also special in that there need not be a correlated parameter without " +
              "a suffix. If both – a parameters text and text@Delete are set, the text property is first deleted and then " +
              "filled with the new content. ",

              "The @Delete suffixed parameter in fact calls for a sub-operation, which is executed after the node " +
              "addressed by the request URL is created (if needed) but before any other tasks of content creattion " +
              "and modification are done. Any item – this may be a property or a node, actually – addressed by the " +
              "@Delete suffixed parameter is just removed if it exists. If the item does not exist, nothing happens. "
          }),
          @ServiceParameter(name="*@MoveFrom", description={
              "Now, that your bright and shiny content management application has great Flash-based file upload feature " +
              "you will want to be able to use the pre-uploaded files for your content with the same request as when " +
              "you upload other content. For example you might have a node storing some text and an illustration you " +
              "uploaded as an image file. ",

              "To support this kind of functionality, the @MoveFrom suffixed parameter may be set to the repository " +
              "path of the node to where you uploaded the image file. ",

              "Example: Your Flash-based file upload stored the file on the server at /tmp/upload/123. You now want " +
              "to store this file along with a title and a text in a newly created node. The following form will" +
              " be your friend: ",
             
              "<pre>" +
              "&lt;!-- trailing slash generates a name for the new node --&gt; \n" +
              "&lt;form method=&quot;POST&quot; action=&quot;/content/page/&quot; enctype=&quot;multipart/form-data&quot;&gt; \n" +
              "    &lt;input type=&quot;hidden&quot; name=&quot;image@MoveFrom&quot; value=&quot;/tmp/upload/123&quot; /&gt; \n" +
              "    &lt;input type=&quot;text&quot; name=&quot;title&quot; /&gt; \n" +
              "    &lt;input type=&quot;text&quot; name=&quot;text&quot; /&gt; \n" +
              "    &lt;input type=&quot;Submit&quot; /&gt; \n" +
              "&lt;/form&gt; \n" +
              "</pre>",

              "If there exists no repository item at the indicated path, nothing is done. If the item indicated by the " +
              "@MoveFrom suffixed parameter already exists, it is replaced by the item addressed by the parameter ",

              "The @MoveFrom suffixed parameter is assumed to be single-valued. If the parameter has multiple values it is " +
              "ignored completely. ",

              "The @MoveFrom suffixed parameter is also special in that there must not be a correlated parameter without a suffix. " +
              "Thus have parameters text and text@MoveFrom may have unexpected results. ",

              "The @MoveFrom suffixed parameter in fact calls for a sub-operation, which is executed after the @Delete sub " +
              "operation but before any other tasks of content creattion and modification are done."
          }),
          @ServiceParameter(name="*@CopyFrom", description={
              "except that the item addressed by the parameter value is not moved but just copied. ",

              "Example: Your Flash-based file upload stored the file on the server at /tmp/upload/123. You now want to store " +
              "this file along with a title and a text in a newly created node. The following form may be your friend: ",

              "<pre>" +
              "&lt;!-- trailing slash generates a name for the new node --&gt; \n" +
              "&lt;form method=&quot;POST&quot; action=&quot;/content/page/&quot; enctype=&quot;multipart/form-data&quot;&gt; \n" +
              "    &lt;input type=&quot;hidden&quot; name=&quot;image@CopyFrom&quot; value=&quot;/tmp/upload/123&quot; /&gt; \n" +
              "    &lt;input type=&quot;text&quot; name=&quot;title&quot; /&gt; \n" +
              "    &lt;input type=&quot;text&quot; name=&quot;text&quot; /&gt; \n" +
              "    &lt;input type=&quot;Submit&quot; /&gt; \n" +
              "&lt;/form&gt; \n" +
              "</pre>",

              "If there exists no repository item at the indicated path, nothing is done. If the item indicated by the " +
              "@CopyFrom suffixed parameter already exists, it is replaced by the item addressed by the parameter value – " +
              "unless of course there is no item at the named location. ",

              "The @CopyFrom suffixed parameter is assumed to be single-valued. If the parameter has multiple values it " +
              "is ignored completely.",

              "The @CopyFrom suffixed parameter is also special in that there must not be a correlated parameter without " +
              "a suffix. Thus have parameters text and text@CopyFrom may have unexpected results.",

              "The @CopyFrom suffixed parameter in fact calls for a sub-operation, which is executed after the @MoveFrom " +
              "sub operation but before any other tasks of content creattion and modification are done. "
          }),
          @ServiceParameter(name=",")

                     
           
         },
        response = {
           @ServiceResponse(code=201,description="The resource was created."),
           @ServiceResponse(code=200,description="The resource was updated or deleted"),
           @ServiceResponse(code=412,description="An item already exists at the destination and the :replace parameter is not set to true"),
           
           @ServiceResponse(code=404,description="The resource does not exist, or the target is not found"),
           @ServiceResponse(code=403,description="The user does not have permission to access the resource"),
           @ServiceResponse(code=401,description="The user is not logged in and the resource is protected"),
           @ServiceResponse(code=0,description="Any other status codes emmitted with have the meaning prescribed in the RFC")
         })
        })
public class Doc_org_apache_sling_servlets_post_impl_SlingPostServlet {

}
