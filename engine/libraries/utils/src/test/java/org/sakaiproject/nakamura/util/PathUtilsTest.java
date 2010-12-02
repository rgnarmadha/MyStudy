package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.easymock.EasyMock;
import org.junit.Test;

import java.security.Principal;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class PathUtilsTest {

  
  @Test
  public void toInternalHash() {
   assertEquals("/testing/d0/33/e2/2a/admin/.extra", PathUtils.toInternalShardPath("/testing", "admin", ".extra"));
   assertEquals("/testing/0a/92/fa/b3/anonymous/.extra", PathUtils.toInternalShardPath("/testing", "anonymous", ".extra"));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetUserPrefix() {
    assertEquals("61/51/anon/", PathUtils.getUserPrefix("",2));
    assertNull(PathUtils.getUserPrefix(null,2));
    assertEquals("22/c6/Lorem/", PathUtils.getUserPrefix("Lorem",2));
    assertEquals("da/3b/ipsum/", PathUtils.getUserPrefix("ipsum",2));
    assertEquals("90/8b/amet_/", PathUtils.getUserPrefix("amet.",2));
  }

  @SuppressWarnings("deprecation")
  @Test
  public void testGetMessagePrefix() {
    Pattern prefixFormat = Pattern.compile("^/\\d{4}/\\d{1,2}/$");
    assertTrue(prefixFormat.matcher(PathUtils.getMessagePath()).matches());
  }

  @Test
  public void testGetParentReference() {
    assertEquals("/Lorem/ipsum/dolor", PathUtils
        .getParentReference("/Lorem/ipsum/dolor/sit"));
    assertEquals("/Lorem/ipsum", PathUtils
        .getParentReference("/Lorem/ipsum/dolor/"));
    assertEquals("/Lorem/ipsum", PathUtils
        .getParentReference("/Lorem/ipsum/dolor"));
    assertEquals("/", PathUtils.getParentReference("/Lorem/"));
    assertEquals("/", PathUtils.getParentReference("/"));
    assertEquals("/", PathUtils.getParentReference(""));
  }

  @Test
  public void testGetDatePrefix() {
    Pattern prefixFormat = Pattern
        .compile("^/\\d{4}/\\d{1,2}/\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    @SuppressWarnings("deprecation")
    String path = PathUtils.getDatePath("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    assertTrue(path.endsWith("/22/c6/Lorem/"));
  }
  @Test
  public void testGetHashPrefix() {
    Pattern prefixFormat = Pattern
        .compile("^/\\p{XDigit}{2}/\\p{XDigit}{2}/\\w+/$");
    String path = PathUtils.getShardPath("Lorem",2);
    assertTrue(path,prefixFormat.matcher(path).matches());
    
    assertEquals("/22/c6/Lorem/",path);
  }

  @Test
  public void testNormalizePath() {
    assertEquals("/Lorem/ipsum/dolor/sit", PathUtils
        .normalizePath("/Lorem//ipsum/dolor///sit"));
    assertEquals("/Lorem/ipsum", PathUtils.normalizePath("//Lorem/ipsum/"));
    assertEquals("/Lorem/ipsum", PathUtils.normalizePath("/Lorem/ipsum//"));
    assertEquals("/", PathUtils.normalizePath("/"));
    assertEquals("/Lorem", PathUtils.normalizePath("Lorem"));
    assertEquals("/", PathUtils.normalizePath(""));
  }
  
  
  @Test
  public void testRemoveFistElement() {
    assertEquals("/a/b/c", PathUtils.removeFirstElement("/x/a/b/c"));
    assertEquals("/a/b/c", PathUtils.removeFirstElement("x/a/b/c"));
    assertEquals("/a/b/c/", PathUtils.removeFirstElement("//x/a/b/c/"));
    assertEquals(null, PathUtils.removeFirstElement(null));
    assertEquals("/", PathUtils.removeFirstElement("/x/"));
    assertEquals("/", PathUtils.removeFirstElement("/"));
    assertEquals("", PathUtils.removeFirstElement(""));
  }
  
  @Test
  public void testRemoveLastElement() {
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x/"));
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x"));
    assertEquals("/a/b/c", PathUtils.removeLastElement("/a/b/c/x///"));
    assertEquals(null, PathUtils.removeLastElement(null));
    assertEquals("/", PathUtils.removeLastElement("/x/"));
    assertEquals("/", PathUtils.removeLastElement("/"));
    assertEquals("", PathUtils.removeLastElement(""));
  }
  
  
  @Test
  public void testNodePatParts() {
    assertArrayEquals(new String[]{"/a/b/c/x/",""},PathUtils.getNodePathParts("/a/b/c/x/"));
    assertArrayEquals(new String[]{"/a/b/c/x",""},PathUtils.getNodePathParts("/a/b/c/x"));
    assertArrayEquals(new String[]{"/a.a/b/c/x/",""},PathUtils.getNodePathParts("/a.a/b/c/x/"));
    assertArrayEquals(new String[]{"/aaa.a/b/c/x",""},PathUtils.getNodePathParts("/aaa.a/b/c/x"));
    assertArrayEquals(new String[]{"/aaa.aa.aa/b/c/x",""},PathUtils.getNodePathParts("/aaa.aa.aa/b/c/x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/x",""},PathUtils.getNodePathParts("aaa.aa.aa/b/c/x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx",".x"},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx",".x.a"},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x.a"));
    assertArrayEquals(new String[]{"aaa.aa.aa/b/c/xxxx.x.a/",""},PathUtils.getNodePathParts("aaa.aa.aa/b/c/xxxx.x.a/"));
    assertArrayEquals(new String[]{"",""},PathUtils.getNodePathParts(""));
    assertArrayEquals(new String[]{"/",""},PathUtils.getNodePathParts("/"));
    assertArrayEquals(new String[]{"/",".aaa"},PathUtils.getNodePathParts("/.aaa"));
    assertArrayEquals(new String[]{"/",".a.aa"},PathUtils.getNodePathParts("/.a.aa"));
    assertArrayEquals(new String[]{"",".aaa"},PathUtils.getNodePathParts(".aaa"));
  }

  @Test
  public void testLastElement() { 
    assertEquals("",PathUtils.lastElement("/a/b/c/x/"));
    assertEquals("x",PathUtils.lastElement("/a/b/c/x"));
    assertEquals("",PathUtils.lastElement("/a.a/b/c/x/"));
    assertEquals("x",PathUtils.lastElement("/aaa.a/b/c/x"));
    assertEquals("x",PathUtils.lastElement("/aaa.aa.aa/b/c/x"));
    assertEquals("x",PathUtils.lastElement("aaa.aa.aa/b/c/x"));
    assertEquals("xxxx",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x"));
    assertEquals("xxxx",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x.a"));
    assertEquals("",PathUtils.lastElement("aaa.aa.aa/b/c/xxxx.x.a/"));
    assertEquals("",PathUtils.lastElement(""));
    assertEquals("",PathUtils.lastElement("/"));
    assertEquals("",PathUtils.lastElement("/.aaa"));
    assertEquals("",PathUtils.lastElement("/.a.aa"));
    assertEquals("",PathUtils.lastElement(".aaa"));
  }

  @Test
  public void testGetSubPath() throws RepositoryException {
    Authorizable au = EasyMock.createMock(Authorizable.class);
    ItemBasedPrincipal principal = EasyMock.createMock(ItemBasedPrincipal.class);
    EasyMock.expect(au.getPrincipal()).andReturn(principal);
    EasyMock.expect(au.hasProperty("path")).andReturn(false);
    EasyMock.expect(principal.getPath()).andReturn("/rep:system/re:authsdfsdfds/rep:user/f/fo/foo");
    EasyMock.replay(au,principal);
    PathUtils.getSubPath(au);
    EasyMock.verify(au,principal);
  }
  @Test
  public void testGetSubPathNoPrincipal() throws RepositoryException {
    Authorizable au = EasyMock.createMock(Authorizable.class);
    Principal principal = EasyMock.createMock(Principal.class);
    EasyMock.expect(au.getPrincipal()).andReturn(principal);
    EasyMock.expect(au.hasProperty("path")).andReturn(false);
    EasyMock.expect(au.getID()).andReturn("theid");
    EasyMock.replay(au,principal);
    Assert.assertEquals("/theid", PathUtils.getSubPath(au));
    EasyMock.verify(au,principal);
  }
  @Test
  public void testGetSubPathHasPath() throws RepositoryException {
    Authorizable au = EasyMock.createMock(Authorizable.class);
    Principal principal = EasyMock.createMock(Principal.class);
    Value value = EasyMock.createMock(Value.class);
    EasyMock.expect(au.getPrincipal()).andReturn(principal);
    EasyMock.expect(au.hasProperty("path")).andReturn(true);
    EasyMock.expect(au.getProperty("path")).andReturn(new Value[]{value});
    EasyMock.expect(value.getString()).andReturn("/f/fo/foo");
    EasyMock.replay(au,principal,value);
    Assert.assertEquals("/f/fo/foo", PathUtils.getSubPath(au));
    EasyMock.verify(au,principal,value);
  }
  

  @Test
  public void testTranslate() {
    assertEquals("/~ieb/testing", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb/testing"));
    assertEquals("/~ieb/", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb/"));
    assertEquals("/~ieb", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb"));
    assertEquals("/~ieb", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb/ieb"));
    assertEquals("/~ieb/ieb", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb/ieb/ieb"));
    assertEquals("/~ieb236/testing/a/b/c", PathUtils.translateAuthorizablePath("/_user/i/ie/ieb/ieb236/testing/a/b/c"));
    assertEquals("/~ieb/testing", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb/testing"));
    assertEquals("/~ieb/", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb/"));
    assertEquals("/~ieb", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb"));
    assertEquals("/~ieb", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb/ieb"));
    assertEquals("/~ieb/ieb", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb/ieb/ieb"));
    assertEquals("/~ieb/testing/a/b/c", PathUtils.translateAuthorizablePath("/_group/i/ieb/ieb/testing/a/b/c"));
    assertEquals("/~ieb236/testing/a/b/c", PathUtils.translateAuthorizablePath("/_group/i/ie/ieb/ieb236/testing/a/b/c"));
    assertEquals("/_group", PathUtils.translateAuthorizablePath("/_group"));
    assertEquals("/_group/", PathUtils.translateAuthorizablePath("/_group/"));
    assertEquals("/_user", PathUtils.translateAuthorizablePath("/_user"));
    assertEquals("/_user/", PathUtils.translateAuthorizablePath("/_user/"));
    assertEquals("/anything/", PathUtils.translateAuthorizablePath("/anything/"));
  }
}
