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
import org.drools.RuleBase;
import org.drools.SessionConfiguration;
import org.drools.StatefulSession;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.process.command.Command;
import org.drools.process.command.CommandService;
import org.drools.reteoo.ReteooWorkingMemory;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrProcessInstanceManager;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrSignalManager;
import org.sakaiproject.nakamura.workflow.persistence.managers.JcrWorkItemManager;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class JcrSingleSessionCommandService implements CommandService {

  private SessionInfo sessionInfo;
  private StatefulSession session;
  private StatefulKnowledgeSession ksession;
  private Environment env;

  public void checkEnvironment(Environment env) {
    if (env.get(EnvironmentName.ENTITY_MANAGER_FACTORY) == null) {
      throw new IllegalArgumentException("Environment must have an EntityManagerFactory");
    }

    // @TODO log a warning that all transactions will be locally scoped using the
    // EntityTransaction
    // if ( env.get( EnvironmentName.TRANSACTION_MANAGER ) == null ) {
    // throw new IllegalArgumentException( "Environment must have an EntityManagerFactory"
    // );
    // }
  }

  public JcrSingleSessionCommandService(RuleBase ruleBase, SessionConfiguration conf,
      Environment env, Session jcrSession) {
    this(new KnowledgeBaseImpl(ruleBase), (SessionConfiguration) conf, env, jcrSession);
  }

  public JcrSingleSessionCommandService(int sessionId, RuleBase ruleBase,
      SessionConfiguration conf, Environment env, Session jcrSession) {
    this(sessionId, new KnowledgeBaseImpl(ruleBase), (SessionConfiguration) conf, env,
        jcrSession);
  }

  public JcrSingleSessionCommandService(KnowledgeBase kbase,
      KnowledgeSessionConfiguration conf, Environment env, Session jcrSession) {
    if (conf == null) {
      conf = new SessionConfiguration();
    }
    this.env = env;

    this.session = ((KnowledgeBaseImpl) kbase).ruleBase.newStatefulSession(
        (SessionConfiguration) conf, this.env);

    this.ksession = new StatefulKnowledgeSessionImpl((ReteooWorkingMemory) session);

    ((JcrSignalManager) this.session.getSignalManager()).setCommandService(this);
    try {
      this.sessionInfo = new SessionInfo(jcrSession, ksession, conf);
      this.sessionInfo.save();
      this.ksession = sessionInfo.getStatefulKnowledgeSession();
    } catch (RepositoryException e) {
      throw new IllegalArgumentException("Could not save session data ", e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not find session data ", e);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Could not find session data ", e);
    }

    new Thread(new Runnable() {
      public void run() {
        session.fireUntilHalt();
      }
    });
  }

  public JcrSingleSessionCommandService(int sessionId, KnowledgeBase kbase,
      KnowledgeSessionConfiguration conf, Environment env, Session jcrSession) {
    if (conf == null) {
      conf = new SessionConfiguration();
    }

    this.env = env;

    try {
      sessionInfo = new SessionInfo(jcrSession, sessionId, kbase, conf, env);
      this.ksession = sessionInfo.getStatefulKnowledgeSession();
    } catch (RepositoryException e) {
      throw new IllegalArgumentException("Could not find session data for id "
          + sessionId, e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Could not find session data for id "
          + sessionId, e);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Could not find session data for id "
          + sessionId, e);
    }
    this.session = (StatefulSession) ((StatefulKnowledgeSessionImpl) ksession).session;
    ((JcrSignalManager) this.session.getSignalManager()).setCommandService(this);

    new Thread(new Runnable() {
      public void run() {
        session.fireUntilHalt();
      }
    });
  }

  public StatefulSession getSession() {
    return this.session;
  }

  public synchronized <T> T execute(Command<T> command) {
    session.halt();

    try {

      T result = command.execute((ReteooWorkingMemory) session);

      sessionInfo.save();

      // clean up cached process and work item instances
      ((JcrProcessInstanceManager) ((ReteooWorkingMemory) session)
          .getProcessInstanceManager()).clearProcessInstances();
      ((JcrWorkItemManager) ((ReteooWorkingMemory) session).getWorkItemManager())
          .clearWorkItems();

      return result;

    } catch (Throwable t1) {
      t1.printStackTrace();
      throw new RuntimeException("Could not execute command", t1);
    } finally {
      new Thread(new Runnable() {
        public void run() {
          session.fireUntilHalt();
        }
      });
    }
  }

  public void dispose() {
    if (session != null) {
      session.dispose();
    }
  }

  public int getSessionId() {
    return (int) sessionInfo.getId();
  }

}