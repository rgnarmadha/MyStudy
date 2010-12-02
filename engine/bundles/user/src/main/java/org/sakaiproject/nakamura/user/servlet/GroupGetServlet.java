package org.sakaiproject.nakamura.user.servlet;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.PathUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>GroupGetServlet</code>
 *
 * @scr.component immediate="true"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="service.description" value="Renders groups"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="sling.servlet.resourceTypes" values="sling/group"
 * @scr.property name="sling.servlet.methods" value="GET"
 * @scr.property name="sling.servlet.extensions" value="json"
 */
@ServiceDocumentation(name="Get Group Servlet",
    description="Returns a group in json format, using all the standard Sling semantics, but includes group profile." +
    		" Binds to any resource of type sling/group although these are" +
    		"store under /rep:system/rep:userManager/rep:groups in the repo eg " +
    		"/rep:system/rep:userManager/rep:groups/ae/3f/ed/g-groupname. The URL is exposed as  " +
        "url /system/userManager/group/g-groupname. This servlet responds at " +
        "/system/userManager/group/g-groupname.json",
    shortDescription="Get a group as json",
    bindings=@ServiceBinding(type=BindingType.TYPE,bindings={"sling/group"},
        extensions=@ServiceExtension(name="*", description="All the standard Sling serializations are possible, json, xml, html")),
    methods=@ServiceMethod(name="GET",
        description={"Get the group json.",
            "Example<br>" +
            "<pre>curl http://localhost:8080/system/userManager/group/g-groupname.json</pre>"},
        response={
          @ServiceResponse(code=200,description="Success, the body contains the Group json with profile."),
          @ServiceResponse(code=404,description="Group was not found."),
          @ServiceResponse(code=500,description="Failure with HTML explanation.")}
   ))
public class GroupGetServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 2792407832129918578L;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Authorizable authorizable = null;
    Resource resource = request.getResource();
    if (resource != null) {
        authorizable = resource.adaptTo(Authorizable.class);
    }

    if (authorizable == null || !authorizable.isGroup()) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find group");
      return;
    }

    Session session = request.getResourceResolver().adaptTo(Session.class);
    if (session == null) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to get repository session");
      return;
    }

    try {

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.object();
      ValueMap groupProps = resource.adaptTo(ValueMap.class);
      if (groupProps != null)
      {
        write.key("properties");
        write.valueMap(groupProps);

      }
      write.key("profile");
      write.value("/_group"+PathUtils.getSubPath(authorizable)+"/public/authprofile");
      write.key("members");
      write.array();

      Group group = (Group)authorizable;
      Set<String> memberNames = new HashSet<String>();
      Iterator<Authorizable> members = group.getMembers();
      while (members.hasNext())
      {
        Authorizable member = members.next();
        String name = member.getPrincipal().getName();
        if (!memberNames.contains(name)) {
          write.value(name);
        }
        memberNames.add(name);
      }
      write.endArray();
      write.endObject();
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to render group details");
      return;
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading from repository");
      return;
    }
  }



}
