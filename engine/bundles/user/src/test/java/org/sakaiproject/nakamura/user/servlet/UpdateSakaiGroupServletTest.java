package org.sakaiproject.nakamura.user.servlet;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.jcr.Session;

public class UpdateSakaiGroupServletTest extends AbstractEasyMockTest {

  @Test
  public void testHandleOperation() throws Exception {
    UpdateSakaiGroupServlet usgs = new UpdateSakaiGroupServlet();

    ArrayList<Modification> changes = new ArrayList<Modification>();

    Group authorizable = createMock(Group.class);
    SlingRepository slingRepository = createMock(SlingRepository.class);
    usgs.bindRepository(slingRepository);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    AuthorizablePostProcessService authorizablePostProcessService = createMock(AuthorizablePostProcessService.class);

    expect(authorizable.isGroup()).andReturn(true).times(2);
    expect(authorizable.getID()).andReturn("g-foo").anyTimes();
    expect(authorizable.hasProperty(UserConstants.PROP_GROUP_MANAGERS)).andReturn(false);
    expect(authorizable.hasProperty(UserConstants.PROP_MANAGERS_GROUP)).andReturn(false);
    expect(authorizable.hasProperty(UserConstants.PROP_MANAGED_GROUP)).andReturn(false);
    expect(authorizable.hasProperty(UserConstants.PROP_GROUP_VIEWERS)).andReturn(false);
    expect(authorizable.hasProperty(UserConstants.PROP_JOINABLE_GROUP)).andReturn(false);

    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Authorizable.class)).andReturn(authorizable);

    UserManager userManager = createMock(UserManager.class);


    expect(session.getUserManager()).andReturn(userManager).anyTimes();




    expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    expectLastCall();

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session).anyTimes();


    Vector<String> params = new Vector<String>();
    HashMap<String, RequestParameter[]> rpm = new HashMap<String, RequestParameter[]>();

    RequestParameterMap requestParameterMap = createMock(RequestParameterMap.class);
    expect(requestParameterMap.entrySet()).andReturn(rpm.entrySet());

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource).times(2);
    expect(request.getResourceResolver()).andReturn(rr).times(2);
    expect(request.getParameterNames()).andReturn(params.elements());
    expect(request.getRequestParameterMap()).andReturn(requestParameterMap);
    expect(request.getParameterValues(":member@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":member")).andReturn(new String[] {});
    expect(request.getParameterValues(":manager@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":manager")).andReturn(new String[] {});
    expect(request.getParameterValues(":viewer@Delete")).andReturn(new String[] {});
    expect(request.getParameterValues(":viewer")).andReturn(new String[] {});

    authorizablePostProcessService.process((Authorizable)EasyMock.anyObject(),(Session)EasyMock.anyObject(),
        (ModificationType)EasyMock.anyObject(), (SlingHttpServletRequest)EasyMock.anyObject());
    expectLastCall();

    HtmlResponse response = new HtmlResponse();

    replay();

    usgs.postProcessorService = authorizablePostProcessService;
    usgs.handleOperation(request, response, changes);

    verify();
  }
}
