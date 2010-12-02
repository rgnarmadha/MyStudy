package org.sakaiproject.nakamura.user.servlet;

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.Modification;
import org.junit.Test;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;

public class DeleteSakaiAuthorizableServletTest extends AbstractEasyMockTest {

  @Test
  public void testHandleOperation() throws Exception {
    DeleteSakaiAuthorizableServlet dsas = new DeleteSakaiAuthorizableServlet();

    JackrabbitSession session = createMock(JackrabbitSession.class);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.adaptTo(Session.class)).andReturn(session);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getParameterValues(":applyTo")).andReturn(new String[] {}).times(2);
    expect(request.getResourceResolver()).andReturn(rr).times(3);
    expect(request.getResource()).andReturn(null).times(2);

    List<Modification> changes = new ArrayList<Modification>();

    AuthorizablePostProcessService authorizablePostProcessService = createMock(AuthorizablePostProcessService.class);

    HtmlResponse response = new HtmlResponse();


    replay();
    dsas.postProcessorService = authorizablePostProcessService;
    dsas.handleOperation(request, response, changes);
    verify();
  }
}
