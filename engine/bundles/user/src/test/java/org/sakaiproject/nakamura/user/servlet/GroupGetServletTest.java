package org.sakaiproject.nakamura.user.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class GroupGetServletTest extends AbstractEasyMockTest {

  @Test
  public void testNullAuthorizable() throws Exception {
    badAuthorizable(null);
  }

  @Test
  public void testNonGroupAuthorizable() throws Exception {
    Authorizable authorizable = createMock(Authorizable.class);
    expect(authorizable.isGroup()).andReturn(false);

    badAuthorizable(authorizable);
  }

  private void badAuthorizable(Authorizable authorizable) throws IOException,
      ServletException {
    GroupGetServlet ggs = new GroupGetServlet();

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Couldn't find group");
    EasyMock.expectLastCall();

    replay();
    ggs.doGet(request, response);
    verify();
  }

  @Test
  public void testNoSession() throws Exception {
    GroupGetServlet ggs = new GroupGetServlet();

    Authorizable authorizable = createMock(Authorizable.class);
    expect(authorizable.isGroup()).andReturn(true);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(null);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(rr);

    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "Unable to get repository session");
    EasyMock.expectLastCall();

    replay();
    ggs.doGet(request, response);
    verify();
  }

  @Test
  public void testGoodRequest() throws Exception {
    GroupGetServlet ggs = new GroupGetServlet();

    Principal principal = createMock(Principal.class);

    ArrayList<Authorizable> al = new ArrayList<Authorizable>();

    Iterator<Authorizable> members = al.iterator();

    Group authorizable = createMock(Group.class);
    expect(authorizable.isGroup()).andReturn(true);
    expect(authorizable.getPrincipal()).andReturn(principal);
    expect(authorizable.getMembers()).andReturn(members);

    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("foo", "bar");

    ValueMap groupProps = createMock(ValueMap.class);
    expect(groupProps.entrySet()).andReturn(map.entrySet());

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);
    expect(resource.adaptTo(ValueMap.class)).andReturn(groupProps);

    expect(authorizable.hasProperty("path")).andReturn(true).anyTimes();
    Value pathValue = createMock(Value.class);

    expect(authorizable.getProperty("path")).andReturn(new Value[] {pathValue}).anyTimes();
    expect(pathValue.getString()).andReturn("/g/g-/g-foo").anyTimes();

    Session session = createMock(Session.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(rr);

    PrintWriter write = new PrintWriter(System.out);

    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    response.setContentType("application/json");
    EasyMock.expectLastCall();
    response.setCharacterEncoding("UTF-8");
    EasyMock.expectLastCall();
    expect(response.getWriter()).andReturn(write);

    replay();
    ggs.doGet(request, response);
    verify();
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    GroupGetServlet ggs = new GroupGetServlet();

    Principal principal = createMock(Principal.class);

    Group authorizable = createMock(Group.class);
    expect(authorizable.isGroup()).andReturn(true);
    expect(authorizable.getPrincipal()).andReturn(principal);
    expect(authorizable.getMembers()).andThrow(new RepositoryException());

    expect(authorizable.hasProperty("path")).andReturn(true).anyTimes();
    Value pathValue = createMock(Value.class);

    expect(authorizable.getProperty("path")).andReturn(new Value[] {pathValue}).anyTimes();
    expect(pathValue.getString()).andReturn("/g/g-/g-foo").anyTimes();


    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put("foo", "bar");

    ValueMap groupProps = createMock(ValueMap.class);
    expect(groupProps.entrySet()).andReturn(map.entrySet());

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);
    expect(resource.adaptTo(ValueMap.class)).andReturn(groupProps);

    Session session = createMock(Session.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getResourceResolver()).andReturn(rr);

    PrintWriter write = new PrintWriter(System.out);

    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    response.setContentType("application/json");
    EasyMock.expectLastCall();
    response.setCharacterEncoding("UTF-8");
    EasyMock.expectLastCall();
    expect(response.getWriter()).andReturn(write);
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "Error reading from repository");
    EasyMock.expectLastCall();

    replay();
    ggs.doGet(request, response);
    verify();
  }
}
