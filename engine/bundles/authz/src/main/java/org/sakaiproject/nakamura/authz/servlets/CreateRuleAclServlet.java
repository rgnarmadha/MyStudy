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
package org.sakaiproject.nakamura.authz.servlets;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.ISO8601Date;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleACLModifier;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;

import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 *
 */
@ServiceDocumentation(name = "Modify Rule ACE Servlet", 
    description = "Modifies and Creates rule based ACLE associated with a node, using simular semantics to the standard *.acl.html selectors.",
    shortDescription="Modify or Create a Rule ACL ACE on a resource.",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sling/servlet/default",
        selectors = @ServiceSelector(name="modifyRuleAce", description=" requires a selector of resource.modifyRuleAce.html to modify the ACE")
    ), 
    methods = { 
         @ServiceMethod(name = "POST", 
             description = {
                 "Modifies an ACE in the ACL on the Reource identified by the URL. The post will replace the ACE on the user, so " +
                 "the caller must ensure they have a current copy of the ACE, edit it locally and then submit the edited copy to this " +
                 "servlet. In addition to the standard ACE modification provided by the standard Sling Access manager this version allows the caller " +
                 "to set multiple active and inactive periods (see kernel documentation under KERN-629) or specify a rule to be used from the " +
                 "rules engine",
                 "<pre>" +
                 "#Make this ACL active for ieb if the Rule in rules system named WhenHeHasEatenHisBreakfast evaluated to true " +
                 "curl -FprincipalId=ieb -Fprivilege@jcr:read=granted -Frule=WhenHeHasEatenHisBreakfast -Fprivilege@jcr:write=denied http://admin:admin@localhost:8080/_user/private.modifyRuleAce.html\n" +
                 "Location: http://admin:admin@localhost:8080/_user/private.acl.html\n" +
                 "</pre>",
                 "<pre>" +
                 "#Make this ACL active for ieb between 12 May 2010 and 24 May 2010" +
                 "curl -FprincipalId=ieb -Fprivilege@jcr:read=granted -Factive=20100512/20100524 -Fprivilege@jcr:write=denied http://admin:admin@localhost:8080/_user/private.modifyRuleAce.html\n" +
                 "Location: http://admin:admin@localhost:8080/_user/private.acl.html\n" +
                 "</pre>"

         },
         parameters = {
         @ServiceParameter(name="principalId",description={
            "The principal ID of the set of entries to modify on this resource. If this is a " +
            "principal of an Autorizable a new RulePrincipal will be created and a new rule based " +
            "ACE will be generated. If this is alreadt a RulePrincipal ID, then that ACE will be " +
            "located and modified." 
         }),
         @ServiceParameter(name="active",description={
             "An array of time periods when the ACL is active. This parameter is optional, when used it takes precidence over the rule parameter. " +
             "This parameter takes the format FromDateTime/ToDateTime, where FromDateTime and ToDateTime are ISO8601 Formatted date strings " +
             "in one of the following forms. 19970714T170000Z, 19970714T170000+01, 1997-07-14T17:00:00Z, 19970714T170000+0100, " +
             "1997-07-14T17:00:00+01, 1997-07-14T17:00:00+01:00 and for date only ranges 19970714 and 1997-07-14 " +
             "eg 1997-07-14T17:00:00+01:00/1997-07-24T09:00:00+01:00"
          }),
          @ServiceParameter(name="inactive",description={
              "An array of time periods when the ACL is inactive. This parameter is optional, when used, it takes precidence over the rule parameter. The format of this parameter is the same as for active." 
           }),
           @ServiceParameter(name="rule",description={
               "The uri of a rule to test to see if the ACL is active. The time active and time inactive take precidence over this parameter.  " 
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
@SlingServlet(methods={"POST"},selectors={"modifyRuleAce"},extensions={"html"},resourceTypes={"sling/servlet/default"})
public class CreateRuleAclServlet extends AbstractRuleAccessPostServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 1149917790920274046L;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.authz.servlets.AbstractRuleAccessPostServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void handleOperation(SlingHttpServletRequest request,
      HtmlResponse htmlResponse, List<Modification> changes)
      throws RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    if (session == null) {
      throw new RepositoryException("JCR Session not found");
    }

    String principalId = request.getParameter("principalId");
    if (principalId == null) {
      throw new RepositoryException("principalId was not submitted.");
    }
    // the principal at this point is a rules principal, do all the normal things with it.
    
    String resourcePath = null;
    Resource resource = request.getResource();
    if (resource == null) {
      throw new ResourceNotFoundException("Resource not found.");
    } else {
      Item item = resource.adaptTo(Item.class);
      if (item != null) {
        resourcePath = item.getPath();
      } else {
        throw new ResourceNotFoundException("Resource is not a JCR Node");
      }
    }
    
    PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
    Principal principal = principalManager.getPrincipal(principalId);
    if ( principal == null ) {
      throw new RepositoryException("Principal "+principalId+" cound not be found ");
    }
    if ( !RulesBasedAce.isRulesBasedPrincipal(principal) ) {
      principal = RulesBasedAce.createPrincipal(principal.getName());
      changes.add(Modification.onCreated(resourcePath));
    }
    
    // Collect the modified privileges from the request.
    Set<String> grantedPrivilegeNames = new HashSet<String>();
    Set<String> deniedPrivilegeNames = new HashSet<String>();
    Set<String> removedPrivilegeNames = new HashSet<String>();
    Enumeration<?> parameterNames = request.getParameterNames();
    while (parameterNames.hasMoreElements()) {
      Object nextElement = parameterNames.nextElement();
      if (nextElement instanceof String) {
        String paramName = (String)nextElement;
        if (paramName.startsWith("privilege@")) {
          String privilegeName = paramName.substring(10);
          String parameterValue = request.getParameter(paramName);
          if (parameterValue != null && parameterValue.length() > 0) {
            if ("granted".equals(parameterValue)) {
              grantedPrivilegeNames.add(privilegeName);
            } else if ("denied".equals(parameterValue)) {
              deniedPrivilegeNames.add(privilegeName);
            } else if ("none".equals(parameterValue)){
              removedPrivilegeNames.add(privilegeName);
            }
          }
        }
      }
    }

    String order = request.getParameter("order");
    
    String[] active = request.getParameterValues("active");
    String[] inactive = request.getParameterValues("inactive");
    String rule = request.getParameter("rule");
    ValueFactory valueFactory = session.getValueFactory();
    Map<String,Object> ruleProperties = new HashMap<String, Object>();
    if ( active != null ) {
      Value[] ranges = createValueRanges(active, valueFactory);
      if ( ranges.length == 1) {
        ruleProperties.put(RulesBasedAce.P_ACTIVE_RANGE, ranges[0]);
      } else if ( ranges.length > 1 ) {
        ruleProperties.put(RulesBasedAce.P_ACTIVE_RANGE, ranges);
      }
    }
    if ( inactive != null ) {
      Value[] ranges = createValueRanges(inactive, valueFactory);
      if ( ranges.length == 1) {
        ruleProperties.put(RulesBasedAce.P_INACTIVE_RANGE, ranges[0]);
      } else if ( ranges.length > 1 ) {
        ruleProperties.put(RulesBasedAce.P_INACTIVE_RANGE, ranges);
      }
    }
    if ( ruleProperties.size() == 0  && rule != null ) {
      ruleProperties.put(RulesBasedAce.P_RULEPROCESSOR, valueFactory.createValue(rule));
    }
    
    // Make the actual changes.
    try {
      
      AccessControlUtil.replaceAccessControlEntry(session, resourcePath, principal,
          grantedPrivilegeNames.toArray(new String[grantedPrivilegeNames.size()]),
          deniedPrivilegeNames.toArray(new String[deniedPrivilegeNames.size()]),
          removedPrivilegeNames.toArray(new String[removedPrivilegeNames.size()]),
          order);
      changes.add(Modification.onModified(resourcePath));
      
      if ( ruleProperties.size() > 0  ) {
        RuleACLModifier ruleACLModifier = new RuleACLModifier();
        ruleACLModifier.setProperties(resourcePath, session, principal, ruleProperties);
      }
      
     
      
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException re) {
      throw new RepositoryException("Failed to create ace.", re);
    }
  }

  /**
   * @param active
   * @param valueFactory
   * @return
   */
  private Value[] createValueRanges(String[] active, ValueFactory valueFactory) {
    int i = 0;
    Value[] ranges = new Value[active.length];
    for ( String a : active) {
      String[] range = StringUtils.split(a, '/');
      ISO8601Date[] d = new ISO8601Date[]{new ISO8601Date(range[0]), new ISO8601Date(range[1])};
      ranges[i++] = valueFactory.createValue(d[0].toString()+"/"+d[1].toString());
    }
    return ranges;
  }
}
