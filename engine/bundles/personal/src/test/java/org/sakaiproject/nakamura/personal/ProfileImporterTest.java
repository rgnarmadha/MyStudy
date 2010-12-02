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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.jcr.contentloader.ContentImportListener;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.personal.PersonalConstants;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

@RunWith(MockitoJUnitRunner.class)
public class ProfileImporterTest {
  @Mock
  private JackrabbitSession session;
  @Mock
  private Node profileNode;
  @Mock
  ContentImporter contentImporter;
  Map<String, Object[]> parameters;

  @Before
  public void setUp() throws RepositoryException {
    parameters = new HashMap<String, Object[]>();
  }

  @Test
  public void canAcceptObjectArrayOfStrings() throws IOException, RepositoryException {
    Object[] objectArrayWithStrings = new Object[] {"{topprop: topvalue}"};
    parameters.put(PersonalConstants.PROFILE_JSON_IMPORT_PARAMETER, objectArrayWithStrings);
    ProfileImporter.importFromParameters(profileNode, parameters, contentImporter, session, null);
    verify(contentImporter).importContent(eq(profileNode), eq(ProfileImporter.CONTENT_ROOT_NAME),
        any(InputStream.class), any(ImportOptions.class), any(ContentImportListener.class));
  }
}
