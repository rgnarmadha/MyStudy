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

package org.sakaiproject.nakamura.persistence;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

/**
 * The <code>SakaiRequestFilter</code> class is a request level filter, which manages the
 * Sakai Cache and Transaction services..
 */
@Service(value=Filter.class)
@Component(immediate=true, metatype=false)
@Properties(value={@Property(name="service.description", value="Transaction Support Filter"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name="filter.scope",value="request", propertyPrivate=true),
    @Property(name="filter.order",intValue={10}, propertyPrivate=true)})
public class TransactionManagerFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManagerFilter.class);

  private static final boolean debug = LOGGER.isDebugEnabled();


  @Reference
  private TransactionManager transactionManager;


  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig config) throws ServletException {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletResponse hresponse = (HttpServletResponse) response;
    try {
      begin();
      chain.doFilter(request, response);
      commit();
    } catch (SecurityException se) {
      rollback();
      // catch any Security exceptions and send a 401
      hresponse.reset();
      hresponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, se.getMessage());
    } catch (RuntimeException e) {
      rollback();
      throw e;
    } catch (IOException e) {
      rollback();
      throw e;
    } catch (ServletException e) {
      rollback();
      throw e;
    } catch (Throwable t) {
      rollback();
      throw new ServletException(t.getMessage(), t);
    }
  }

  /**
   * @throws SystemException
   * @throws NotSupportedException
   * 
   */
  protected void begin() throws NotSupportedException, SystemException {
    transactionManager.begin();
  }

  /**
   * @throws SystemException
   * @throws SecurityException
   * 
   */
  protected void commit() throws SecurityException, SystemException {
    try {
      if (Status.STATUS_NO_TRANSACTION != transactionManager.getStatus()) {
        transactionManager.commit();
      }
    } catch (RollbackException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (IllegalStateException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (HeuristicMixedException e) {
      LOGGER.warn(e.getMessage(), e);
    } catch (HeuristicRollbackException e) {
      LOGGER.warn(e.getMessage(), e);
    }
  }

  /**
   *
   */
  protected void rollback() {
    try {
      transactionManager.rollback();
    } catch (IllegalStateException e) {
      if (debug) {
        LOGGER.debug(e.getMessage(), e);
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

}
