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

package org.sakaiproject.nakamura.workflow.persistence;

import org.drools.KnowledgeBase;
import org.drools.marshalling.Marshaller;
import org.drools.marshalling.MarshallerFactory;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.sakaiproject.nakamura.api.workflow.WorkflowConstants;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class SessionInfo extends AbstractIdBasedObject {

  private Date startDate;
  private Date lastModificationDate;

  private byte[] rulesByteArray;

  private byte[] rulesByteArrayShadow;

  private transient StatefulKnowledgeSession statefulKnowledgeSession;
  private boolean dirty;
  private KnowledgeBase kbase;
  private KnowledgeSessionConfiguration conf;
  private Environment env;
  private Marshaller marshaller;

  public SessionInfo(Session session, StatefulKnowledgeSession ksession,
      KnowledgeSessionConfiguration conf) throws PathNotFoundException,
      RepositoryException, IOException {
    super(session, ksession.getId());
    this.statefulKnowledgeSession = ksession;
    this.kbase = ksession.getKnowledgeBase();
    this.conf = conf;
    this.env = ksession.getEnvironment();
    ObjectMarshallingStrategy[] strategies = (ObjectMarshallingStrategy[]) this.env
        .get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
    if (strategies != null) {
      // use strategies if provided in the environment
      this.marshaller = MarshallerFactory.newMarshaller(kbase, strategies);
    } else {
      this.marshaller = MarshallerFactory.newMarshaller(kbase);
    }
  }

  public SessionInfo(Session session, KnowledgeBase kbase,
      KnowledgeSessionConfiguration conf, Environment env) throws RepositoryException,
      IOException {
    this(session, -1, kbase, conf, env);
    
  }

  public SessionInfo(Session session, int sessionId, KnowledgeBase kbase,
      KnowledgeSessionConfiguration conf, Environment env) throws RepositoryException,
      IOException {
    super(session, sessionId);
    this.kbase = kbase;
    this.conf = conf;
    this.env = env;
    ObjectMarshallingStrategy[] strategies = (ObjectMarshallingStrategy[]) env
        .get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
    if (strategies != null) {
      // use strategies if provided in the environment
      this.marshaller = MarshallerFactory.newMarshaller(kbase, strategies);
    } else {
      this.marshaller = MarshallerFactory.newMarshaller(kbase);
    }
    this.startDate = new Date();
  }

  public Date getStartDate() {
    return this.startDate;
  }

  public Date getLastModificationDate() {
    return this.lastModificationDate;
  }

  public StatefulKnowledgeSession getStatefulKnowledgeSession() throws IOException, ClassNotFoundException {
    if (dirty || statefulKnowledgeSession == null) {
      ByteArrayInputStream bais = new ByteArrayInputStream(rulesByteArray);
      if (statefulKnowledgeSession == null) {
        statefulKnowledgeSession = marshaller.unmarshall(bais, this.conf, this.env);
      } else {
        marshaller.unmarshall(bais, statefulKnowledgeSession);
      }
      dirty = false;
    }
    return statefulKnowledgeSession;
  }

  /**
   * Update the session info object from the session.
   * 
   * @throws RepositoryException
   * @throws IOException
   */
  public void update() throws RepositoryException, IOException {
    // we always increase the last modification date for each action, so we know there
    // will be an update
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      marshaller.marshall(baos, statefulKnowledgeSession);
    } catch (IOException e) {
      throw new RuntimeException("Unable to get session snapshot", e);
    }

    byte[] newByteArray = baos.toByteArray();
    if (!Arrays.equals(newByteArray, this.rulesByteArray)) {
      lastModificationDate = new Date();
      rulesByteArray = newByteArray;
    }
    rulesByteArrayShadow = this.rulesByteArray;
  }

  /**
   * Save the session info to jcr.
   * 
   * @throws RepositoryException
   * @throws IOException
   * 
   */
  public void save() throws RepositoryException, IOException {
    update();
    if (!Arrays.equals(this.rulesByteArrayShadow, this.rulesByteArray)) {
      setProperty(rulesByteArray);
      if (session.hasPendingChanges()) {
        session.save();
      }
      lastModificationDate = new Date();
      rulesByteArrayShadow = rulesByteArray;
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#load()
   */
  @Override
  public void load() throws RepositoryException, IOException {
    byte[] newByteArray = getByteArray(new byte[0]);
    if (!Arrays.equals(newByteArray, this.rulesByteArray)) {
      lastModificationDate = new Date();
      rulesByteArray = newByteArray;
    }
    rulesByteArrayShadow = this.rulesByteArray;
    dirty = true;
  }

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#createContentNode()
   */
  @Override
  protected Node createContentNode(Node parentNode, String nodeName)
      throws RepositoryException {
    Node newObjectNode = parentNode.addNode(nodeName, "nt:file");
    Node contentNode = newObjectNode.addNode("jcr:content", "nt:resource");
    Binary value = contentNode.getSession().getValueFactory().createBinary(
        new ByteArrayInputStream(new byte[0]));
    contentNode.setProperty("jcr:data", value);
    return newObjectNode;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.workflow.persistence.AbstractIdBasedObject#getStoragePrefix()
   */
  @Override
  protected String getStoragePrefix() {
    return WorkflowConstants.SESSION_STORAGE_PREFIX;
  }

}
