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

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleACLModifier;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@ServiceDocumentation(
    description={"Gets a json feed of ACLS with rule information, and time range properties"},
    name="Get Rule Acl Servlet",
    shortDescription="Gets a json feed of the ACL on a node including Rule information",
    bindings={
       @ServiceBinding(bindings={"/sling/servlet/default"},type=BindingType.TYPE,
           selectors={@ServiceSelector(
               name="ruleacl",
               description={"Outputs the ACL for resource"})})},
    methods={
        @ServiceMethod(name="GET",
            description={
            "gets the ACL for the resource in the following form, rule based ACLs " +
            "have principal IDs of the form sakai-rules:&lt;principalid&gt;.uid, alowing multiple" +
            "rule ace's to be present in a ACL for the same principal. The response will also include" +
            "non rule based ACE's in the ACL ",
            "<pre>\n" +
            "{\n" +
            "  sakai-rules:ieb.2344242423 : {\n" +
            "     order : \"0\" \n" +
            "     granted : [ \"jcr:read\" ],\n" +
            "     denied : [ \"jcr:write\" ],\n" +
            "     active : [  \n" +
            "       \"2010-04-05T01:00:00Z/2010-04-05T02:00:00Z\",\n" +
            "       \"2010-03-04/2010-04-04\"\n" +
            "              ], \n" +
            "     inactive : [  \n" +
            "       \"2010-05-05T01:00:00Z/2010-05-05T02:00:00Z\",\n" +
            "       \"2010-06-04/2010-06-04\"\n" +
            "              ] \n" +
            "  },\n" +
            "  sakai-rules:ieb.1221213 : {\n" +
            "     order : \"1\" \n" +
            "     granted : [ \"jcr:write\" ],\n" +
            "     active : [  \n" +
            "       \"2010-02-05T01:00:00Z/2010-02-05T02:00:00Z\",\n" +
            "              ] \n" +
            "  },\n" +
            "  everyone : {\n" +
            "     order : \"2\" \n" +
            "     granted : [ \"jcr:read\" ]\n" +
            "  }\n" +
            "}" +
            "</pre"            
            }, 
            response={
              @ServiceResponse(code=200,description="On success"),
              @ServiceResponse(code=404,description="If the resource is not found"),
              @ServiceResponse(code=402, description="The current user does not have permission to view the ACL ")
              })
        }
       
    )
    
@SlingServlet(resourceTypes = { "sling/servlet/default" }, methods = { "GET" }, selectors = { "ruleacl" }, extensions = { "json" })
public class GetRuleAclServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = 3158361305196675882L;
  /**
   * default log
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.
   * SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      if (session == null) {
        throw new RepositoryException("JCR Session not found");
      }

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

      AccessControlEntry[] declaredAccessControlEntries = getDeclaredAccessControlEntries(
          session, resourcePath);
      Map<String, Map<String, Object>> aclMap = new LinkedHashMap<String, Map<String, Object>>();
      int sequence = 0;
      for (AccessControlEntry ace : declaredAccessControlEntries) {
        Principal principal = ace.getPrincipal();
        Map<String, Object> map = aclMap.get(principal.getName());
        if (map == null) {
          map = new LinkedHashMap<String, Object>();
          aclMap.put(principal.getName(), map);
          map.put("order", sequence++);
        }

        RuleACLModifier ruleACLModifier = new RuleACLModifier();
        Map<String, Property> pmap = ruleACLModifier.getProperties(resourcePath, session,
            principal);
        if (pmap == null) {
          Map<String, Object> properties = (Map<String, Object>) map.get("properties");
          if (properties == null) {
            properties = new HashMap<String, Object>();
            map.put("properties", properties);
          }
        } else {
          Map<String, Object> properties = (Map<String, Object>) map.get("properties");
          if (properties == null) {
            properties = new HashMap<String, Object>(pmap.size());
            map.put("properties", properties);
          }
          for (Entry<String, Property> ep : pmap.entrySet()) {
            String name = ep.getKey();
            Property prop = ep.getValue();
            if (prop.isMultiple()) {
              Value[] values = prop.getValues();
              String[] s = new String[values.length];
              for (int i = 0; i < s.length; i++) {
                s[i] = values[i].getString();
              }
              log.info("Got Property {} {} ", name, Arrays.toString(s));
              properties.put(name, s);
            } else {
              log.info("Got Property {} {} ", name, prop.getString());
              properties.put(name, prop.getString());
            }
          }
        }

        boolean allow = AccessControlUtil.isAllow(ace);
        if (allow) {
          Set<String> grantedSet = (Set<String>) map.get("granted");
          if (grantedSet == null) {
            grantedSet = new LinkedHashSet<String>();
            map.put("granted", grantedSet);
          }
          Privilege[] privileges = ace.getPrivileges();
          for (Privilege privilege : privileges) {
            grantedSet.add(privilege.getName());
          }
        } else {
          Set<String> deniedSet = (Set<String>) map.get("denied");
          if (deniedSet == null) {
            deniedSet = new LinkedHashSet<String>();
            map.put("denied", deniedSet);
          }
          Privilege[] privileges = ace.getPrivileges();
          for (Privilege privilege : privileges) {
            deniedSet.add(privilege.getName());
          }
        }

      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      List<JSONObject> aclList = new ArrayList<JSONObject>();
      Set<Entry<String, Map<String, Object>>> entrySet = aclMap.entrySet();
      for (Entry<String, Map<String, Object>> entry : entrySet) {
        String principalName = entry.getKey();
        Map<String, Object> value = entry.getValue();

        JSONObject aceObject = new JSONObject();
        
        Map<String, Object> properties = (Map<String, Object>) value.get("properties");
        if ( properties.containsKey(RulesBasedAce.P_RULEPROCESSOR) ) {
          aceObject.put(RulesBasedAce.P_RULEPROCESSOR, properties.get(RulesBasedAce.P_RULEPROCESSOR));
        }
        String[] activeRanges = convertToList(properties,RulesBasedAce.P_ACTIVE_RANGE);
        if ( activeRanges != null ) {
          JSONArray active = new JSONArray();
          for ( String r : activeRanges ) {
            active.put(r);
          }
          aceObject.put("active", active);
        }
        String[] inactiveRanges = convertToList(properties,RulesBasedAce.P_INACTIVE_RANGE);
        if ( inactiveRanges != null ) {
          JSONArray inactive = new JSONArray();
          for ( String r : inactiveRanges ) {
            inactive.put(r);
          }
          aceObject.put("inactive", inactive);
        }
        
        aceObject.put("principal", principalName);

        Set<String> grantedSet = (Set<String>) value.get("granted");
        if (grantedSet != null) {
          aceObject.put("granted", grantedSet);
        }

        Set<String> deniedSet = (Set<String>) value.get("denied");
        if (deniedSet != null) {
          aceObject.put("denied", deniedSet);
        }
        aceObject.put("order", value.get("order"));
        
        aclList.add(aceObject);
      }
      JSONObject jsonAclMap = new JSONObject(aclMap);
      for (JSONObject jsonObj : aclList) {
        jsonAclMap.put(jsonObj.getString("principal"), jsonObj);
      }
      jsonAclMap.write(response.getWriter());
      // do the dump
    } catch (AccessDeniedException ade) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (ResourceNotFoundException rnfe) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND, rnfe.getMessage());
    } catch (Throwable throwable) {
      log.debug("Exception while handling GET " + request.getResource().getPath()
          + " with " + getClass().getName(), throwable);
      throw new ServletException(throwable);
    }
  }

  /**
   * @param properties
   * @param pActiveRange
   * @return
   */
  private String[] convertToList(Map<String, Object> properties, String name) {
    List<String> active = new ArrayList<String>();
    if ( properties.containsKey(name) ) {
      addObject(active, properties.get(name));
    }
    for ( int i = 0; i < 100; i++ ) {
      if ( properties.containsKey(name+i) ) {
        addObject(active, properties.get(name+i));
      } else {
        break;
      }
    }
    if (active.size() == 0) {
      return null;
    } 
    return active.toArray(new String[active.size()]);
  }

  /**
   * @param active
   * @param object
   */
  private void addObject(List<String> active, Object o) {    
    if ( o instanceof Object[] ) {
      for ( Object ov : (Object[])o ) {
        active.add(String.valueOf(ov));
      }
    } else {
      active.add(String.valueOf(o));
    }
  }

  private AccessControlEntry[] getDeclaredAccessControlEntries(Session session,
      String absPath) throws RepositoryException {
    AccessControlManager accessControlManager = AccessControlUtil
        .getAccessControlManager(session);
    AccessControlPolicy[] policies = accessControlManager.getPolicies(absPath);
    for (AccessControlPolicy accessControlPolicy : policies) {
      if (accessControlPolicy instanceof AccessControlList) {
        AccessControlEntry[] accessControlEntries = ((AccessControlList) accessControlPolicy)
            .getAccessControlEntries();
        return accessControlEntries;
      }
    }
    return new AccessControlEntry[0];
  }

}
