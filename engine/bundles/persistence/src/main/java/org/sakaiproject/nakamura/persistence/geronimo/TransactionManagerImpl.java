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
package org.sakaiproject.nakamura.persistence.geronimo;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.configuration.ConfigurationService;
import org.sakaiproject.nakamura.api.configuration.NakamuraConstants;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;

/**
 *
 */
@Component
@Service
public class TransactionManagerImpl implements TransactionManager {

  private GeronimoTransactionManager transactionManager;

  @Reference
  private ConfigurationService configurationService;

  @SuppressWarnings(value={"NP_UNWRITTEN_FIELD","UWF_UNWRITTEN_FIELD"},justification="Managed service injection by OSGi")
  protected void activate(ComponentContext componentContext) throws XAException {
    int defaultTransactionTimeoutSeconds = Integer.parseInt(configurationService
        .getProperty(NakamuraConstants.TRANSACTION_TIMEOUT_SECONDS));
    transactionManager = new GeronimoTransactionManager(defaultTransactionTimeoutSeconds);
  }
  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#begin()
   */
  public void begin() throws NotSupportedException, SystemException {
    transactionManager.begin();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#commit()
   */
  public void commit() throws HeuristicMixedException, HeuristicRollbackException,
      IllegalStateException, RollbackException, SecurityException, SystemException {
    transactionManager.commit();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#getStatus()
   */
  public int getStatus() throws SystemException {
    return transactionManager.getStatus();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#getTransaction()
   */
  public Transaction getTransaction() throws SystemException {
    return transactionManager.getTransaction();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#resume(javax.transaction.Transaction)
   */
  public void resume(Transaction tobj) throws IllegalStateException,
      InvalidTransactionException, SystemException {
    transactionManager.resume(tobj);
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#rollback()
   */
  public void rollback() throws IllegalStateException, SecurityException, SystemException {
    transactionManager.rollback();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#setRollbackOnly()
   */
  public void setRollbackOnly() throws IllegalStateException, SystemException {
    transactionManager.setRollbackOnly();
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#setTransactionTimeout(int)
   */
  public void setTransactionTimeout(int seconds) throws SystemException {
    transactionManager.setTransactionTimeout(seconds);
  }

  /**
   * {@inheritDoc}
   * @see javax.transaction.TransactionManager#suspend()
   */
  public Transaction suspend() throws SystemException {
    // TODO Auto-generated method stub
    return null;
  }

}
