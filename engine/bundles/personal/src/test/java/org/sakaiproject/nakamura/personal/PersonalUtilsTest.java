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
package org.sakaiproject.nakamura.personal;

import static org.easymock.EasyMock.expect;

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.personal.PersonalConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class PersonalUtilsTest extends AbstractEasyMockTest {

  private Authorizable adminUser;
  private Authorizable myGroup;
  private String groupName = "g-mygroup";
  private String userName = "admin";
  private String groupPublicPath = "/_group/g/g-/g-mygroup/public";
  private String userPublicPath = "/_user/a/ad/admin/public";
  private String groupPrivatePath = "/_group/g/g-/g-mygroup/private";
  private String userPrivatePath = "/_user/a/ad/admin/private";

  @Before
  public void setUp() throws Exception {
    super.setUp();

    adminUser = createAuthorizable(userName, false, true);
    myGroup = createAuthorizable(groupName, true, true);
  }

  @Test
  public void testPublicPath() {
    // Group
    String result = PersonalUtils.getPublicPath(myGroup);
    Assert.assertEquals(groupPublicPath, result);

    // User
    result = PersonalUtils.getPublicPath(adminUser);
    Assert.assertEquals(userPublicPath, result);
  }

  @Test
  public void testProfilePath() {
    String result = PersonalUtils.getProfilePath(adminUser);
    Assert.assertEquals(userPublicPath + "/authprofile", result);
  }

  @Test
  public void testPrivatePath() {
    // Group
    String result = PersonalUtils.getPrivatePath(myGroup);
    Assert.assertEquals(groupPrivatePath, result);

    // User
    result = PersonalUtils.getPrivatePath(adminUser);
    Assert.assertEquals(userPrivatePath, result);
  }

  @Test
  public void testGetPrefferedMailTransport() throws Exception {
    String pref = "internal";
    Node node = createMock(Node.class);
    Property prop = createMock(Property.class);
    expect(node.hasProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)).andReturn(
        true);
    expect(node.getProperty(PersonalConstants.PREFERRED_MESSAGE_TRANSPORT)).andReturn(
        prop);
    expect(prop.getString()).andReturn(pref);
    replay();

    String result = PersonalUtils.getPreferredMessageTransport(node);
    Assert.assertEquals(pref, result);
  }

  @Test
  public void testGetEmailAddresses() throws RepositoryException {
    String[] mails = { "foo@bar.com", "test@test.com" };
    Node node = createMock(Node.class);
    Property prop = createMock(Property.class);
    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    Value[] vals = new Value[2];
    vals[0] = createMock(Value.class);
    vals[1] = createMock(Value.class);
    expect(vals[0].getString()).andReturn(mails[0]);
    expect(vals[1].getString()).andReturn(mails[1]);

    expect(propDef.isMultiple()).andReturn(true);

    expect(node.hasProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(true).times(2);
    expect(node.getProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(prop);
    expect(prop.getDefinition()).andReturn(propDef);
    expect(prop.getValues()).andReturn(vals);

    replay();

    String[] result = PersonalUtils.getEmailAddresses(node);
    for (int i = 0; i < mails.length; i++) {
      Assert.assertEquals(mails[i], result[i]);
    }
  }

  @Test
  public void testPrimaryEmailAddress() throws RepositoryException {
    String[] mails = { "foo@bar.com", "test@test.com" };
    Node node = createMock(Node.class);
    Property prop = createMock(Property.class);
    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    Value[] vals = new Value[2];
    vals[0] = createMock(Value.class);
    vals[1] = createMock(Value.class);
    expect(vals[0].getString()).andReturn(mails[0]);
    expect(vals[1].getString()).andReturn(mails[1]);

    expect(propDef.isMultiple()).andReturn(true);

    expect(node.hasProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(true).times(2);
    expect(node.getProperty(PersonalConstants.EMAIL_ADDRESS)).andReturn(prop);
    expect(prop.getDefinition()).andReturn(propDef);
    expect(prop.getValues()).andReturn(vals);

    replay();

    String result = PersonalUtils.getPrimaryEmailAddress(node);
    Assert.assertEquals(mails[0], result);
  }

}
