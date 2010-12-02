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

package org.sakaiproject.nakamura.rules;

import junit.framework.Assert;

import org.drools.KnowledgeBase;
import org.drools.definition.KnowledgePackage;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

public class KnowledgeBaseHolderTest {

  @Mock
  private Node ruleSetNode;
  @Mock
  private NodeIterator nodeIterator;
  @Mock
  private Node packageNode;
  @Mock
  private NodeType packageNodeType;
  @Mock
  private Property property;
  @Mock
  private Node packageFileNode;
  @Mock
  private Property packageFileBody;
  @Mock
  private Binary binary;

  public KnowledgeBaseHolderTest() {
    MockitoAnnotations.initMocks(this);
  }
  
  @Test
  public void test() throws IOException, ClassNotFoundException, RepositoryException, InstantiationException, IllegalAccessException {
    
    Mockito.when(ruleSetNode.getPath()).thenReturn("/test/ruleset");
    Mockito.when(ruleSetNode.getNodes()).thenReturn(nodeIterator);
    Mockito.when(nodeIterator.hasNext()).thenReturn(true, false, true, false);
    Mockito.when(nodeIterator.nextNode()).thenReturn(packageNode, packageNode);
    Mockito.when(packageNode.getPrimaryNodeType()).thenReturn(packageNodeType);
    Mockito.when(packageNodeType.getName()).thenReturn(NodeType.NT_FILE);
        
    Mockito.when(packageNode.getPath()).thenReturn("/test/ruleset/package1");
    
    Mockito.when(packageNode.getNode(Node.JCR_CONTENT)).thenReturn(packageFileNode);
    
    Mockito.when(packageFileNode.getProperty(Property.JCR_LAST_MODIFIED)).thenReturn(property);
    Calendar lastModified = GregorianCalendar.getInstance();
    lastModified.setTimeInMillis(System.currentTimeMillis());
    Mockito.when(property.getDate()).thenReturn(lastModified);
    
    Mockito.when(packageFileNode.getProperty(Property.JCR_DATA)).thenReturn(packageFileBody);
    
    Mockito.when(packageFileBody.getBinary()).thenReturn(binary);
    
    
    
    Mockito.when(binary.getStream()).thenAnswer(new Answer<InputStream>() {

      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return  this.getClass().getResourceAsStream("/SLING-INF/content/var/rules/org.sakaiproject.nakamura.rules/org.sakaiproject.nakamura.rules.example/0.10-SNAPSHOT/org.sakaiproject.nakamura.rules.example-0.7-SNAPSHOT.pkg");
      }
    });
    
    
    KnowledgeBaseHolder kbh = new KnowledgeBaseHolder(ruleSetNode,null);
    KnowledgeBase kb = kbh.getKnowledgeBase();
    Assert.assertNotNull(kb);
    Collection<KnowledgePackage> packages = kb.getKnowledgePackages();
    Assert.assertEquals(1, packages.size());
  }
}
