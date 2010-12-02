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
package org.sakaiproject.nakamura.version.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.servlet.ServletException;

/**
 *
 */
public class ListVersionsServletTest extends AbstractEasyMockTest {

  private ListVersionsServlet listVersionsServlet;

  /**
   * @throws java.lang.Exception
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    listVersionsServlet = new ListVersionsServlet();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testGetInvertedRange() {
    assertArrayEquals(new long[]{5L,10L}, listVersionsServlet.getInvertedRange(10, 0, 5));
    assertArrayEquals(new long[]{6L,11L}, listVersionsServlet.getInvertedRange(11, 0, 5));
    assertArrayEquals(new long[]{5L,10L}, listVersionsServlet.getInvertedRange(11, 1, 5));
  }
  @Test
  public void testGetInvertedRangeOffset() {

    assertArrayEquals(new long[]{1L,6L}, listVersionsServlet.getInvertedRange(10, 4, 5));
    assertArrayEquals(new long[]{0L,6L}, listVersionsServlet.getInvertedRange(10, 4, 6));
    assertArrayEquals(new long[]{0L,6L}, listVersionsServlet.getInvertedRange(10, 4, 7));
    assertArrayEquals(new long[]{0L,6L}, listVersionsServlet.getInvertedRange(10, 4, 10));
    assertArrayEquals(new long[]{0L,6L}, listVersionsServlet.getInvertedRange(10, 4, 100));
    assertArrayEquals(new long[]{0L,1L}, listVersionsServlet.getInvertedRange(10, 9, 5));
    assertArrayEquals(new long[]{0L,0L}, listVersionsServlet.getInvertedRange(10, 10, 5));
  }
  @Test
  public void testGetInvertedRangeLogic() {
    for ( int i = -10; i < 20; i++) {
      for ( int j = -10; j < 20; j++) {
        for ( int k = -10; k < 20; k++) {
          long[] range = listVersionsServlet.getInvertedRange(i,j,k);
          int x = ( i < 0 )?0:i; // no negative lengths
          int[] test = new int[x];
          int y = (k<0)?0:k; // no negative output lengths
          int len = (int) (range[1] - range[0]);
          assertTrue((len) <= y);
          assertTrue("Length was "+len+" i:"+i+" j:"+j+" k:"+k+" r0:"+range[0]+" r1:"+range[1]+" y:"+y,(len) >= 0);
          y = (len<y)?len:y;
          int[] testout = new int[y];
          int m=0;
          for ( int l = (int) range[0]; l < range[1]; l++ ) {
            testout[m++] = test[l];
          }
        }
      }
    }
  }
  @Test
  public void testGetInvertedRangeLength() {
    assertArrayEquals(new long[]{1L,10L}, listVersionsServlet.getInvertedRange(10, 0, 9));
    assertArrayEquals(new long[]{0L,10L}, listVersionsServlet.getInvertedRange(10, 0, 10));
    assertArrayEquals(new long[]{0L,10L}, listVersionsServlet.getInvertedRange(10, 0, 11));
    assertArrayEquals(new long[]{0L,10L}, listVersionsServlet.getInvertedRange(10, 0, 12));
    assertArrayEquals(new long[]{0L,10L}, listVersionsServlet.getInvertedRange(10, 0, 12));
  }


  @Test
  public void testGet() throws VersionException, UnsupportedRepositoryOperationException,
      InvalidItemStateException, LockException, RepositoryException, IOException, ServletException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createNiceMock(SlingHttpServletResponse.class);
    Resource resource = createNiceMock(Resource.class);
    Node node = createNiceMock(Node.class);
    Version version = createNiceMock(Version.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    VersionHistory versionHistory = createNiceMock(VersionHistory.class);
    VersionIterator versionIterator = createNiceMock(VersionIterator.class);
    Node versionNode = createNiceMock(Node.class);
    Session session = createNiceMock(Session.class);
    Workspace workspace = createNiceMock(Workspace.class);
    VersionManager versionManager = createNiceMock(VersionManager.class);
    RequestPathInfo pathInfo = createNiceMock(RequestPathInfo.class);

    EasyMock.expect(pathInfo.getSelectors()).andReturn(new String[0]);

    EasyMock.expect(request.getRequestPathInfo()).andReturn(pathInfo);
    EasyMock.expect(request.getResource()).andReturn(resource).anyTimes();
    EasyMock.expect(resource.adaptTo(Node.class)).andReturn(node).anyTimes();
    PrintWriter pw = new PrintWriter(new ByteArrayOutputStream());
    EasyMock.expect(response.getWriter()).andReturn(pw).anyTimes();

    EasyMock.expect(node.getSession()).andReturn(session);
    EasyMock.expect(session.getWorkspace()).andReturn(workspace);
    EasyMock.expect(workspace.getVersionManager()).andReturn(versionManager);
    EasyMock.expect(versionManager.getVersionHistory("/foo")).andReturn(versionHistory);
    EasyMock.expect(node.getPath()).andReturn("/foo").anyTimes();
    EasyMock.expect(versionHistory.getAllVersions()).andReturn(versionIterator);
    EasyMock.expect(versionIterator.getSize()).andReturn(2L);
    EasyMock.expect(versionIterator.hasNext()).andReturn(true);
    EasyMock.expect(versionIterator.nextVersion()).andReturn(version);
    EasyMock.expect(versionIterator.hasNext()).andReturn(true);
    EasyMock.expect(versionIterator.nextVersion()).andReturn(version);

    EasyMock.expect(version.getName()).andReturn("NameVersioNode").anyTimes();
    EasyMock.expect(version.getNode(JcrConstants.JCR_FROZENNODE)).andReturn(versionNode).anyTimes();
    EasyMock.expect(version.getProperties()).andReturn(propertyIterator).anyTimes();
    EasyMock.expect(propertyIterator.hasNext()).andReturn(false).anyTimes();

    replay();

    listVersionsServlet.doGet(request, response);
    verify();

  }
}
