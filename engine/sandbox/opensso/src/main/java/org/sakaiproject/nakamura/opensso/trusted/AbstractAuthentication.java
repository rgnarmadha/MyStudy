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
package org.sakaiproject.nakamura.opensso.trusted;

import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public abstract class AbstractAuthentication {
  
  protected boolean forceLogout;
  protected boolean valid;
  protected SimpleCredentials credentials;

  

  
  /**
   * @param request
   * @param response 
   */
  protected AbstractAuthentication(HttpServletRequest request, HttpServletResponse response) {
    forceLogout = false;
    valid = false;
    dumpRequest(request);
    String tryLogin = request.getParameter(AbstractAuthServlet.TRY_LOGIN);
    if ( tryLogin != null && tryLogin.trim().length() > 0 ) {

      String userName = getUserName(request, response);
      if (userName != null) {
        credentials = new SimpleCredentials(userName, new char[0]);
        valid = true;
        credentials.setAttribute(AbstractAuthenticationHandler.AUTHENTICATION_OBJECT,
            this);

        try {
          createUser(userName);
        } catch (RepositoryException e) {
          LoggerFactory.getLogger(getClass()).debug(
              "Failed to auto populate with a user ");
        }
      }
      System.err.println("Login was requested ");
    } else {
      LoggerFactory.getLogger(getClass()).debug("Login was not requested ");
      System.err.println("Login was not requested ");
    }

  }
  
  /**
   * @param request
   */
  @SuppressWarnings("unchecked")
  private void dumpRequest(HttpServletRequest request) {
    System.err.println("Request is ["+request+"]");
    for ( Enumeration<?> e = request.getParameterNames(); e.hasMoreElements(); ) {
      String name = (String) e.nextElement();
      System.err.println("Parameter Name ["+name+"]["+request.getParameter(name)+"]");
    }
    Map<String, Object> pmap = request.getParameterMap();
    
    System.err.println("Parameter Map ["+pmap+"]");
    for ( Entry<String, Object> ev : pmap.entrySet()) {
      System.err.println("Map Name ["+ev.getKey()+"] ["+ev.getValue()+"]");
    }
    
    for ( Enumeration<?> e = request.getAttributeNames(); e.hasMoreElements(); ) {
      String name = (String) e.nextElement();
      System.err.println("Attribute Name ["+name+"]["+request.getAttribute(name)+"]");
    }
    
    for ( Enumeration<?> e = request.getHeaderNames(); e.hasMoreElements(); ) {
      String name = (String) e.nextElement();
      System.err.println("Header Name ["+name+"]["+request.getHeader(name)+"]");
    }
    
    System.err.println("The Query String IS ["+request.getQueryString()+"]");
    System.err.println("Starting to get Authentication ["+request.getParameter(AbstractAuthServlet.TRY_LOGIN)+"]");
  }

  /**
   * Create a user in the repository, this may be disabled if the Authentication Mechanism should
   * not create a user in the respository and just fail to authenticate.
   * @param userName
   * @throws RepositoryException 
   */
  protected abstract void createUser(String userName) throws RepositoryException;
  
  /**
   * Validate the request to authenticate and get the username from the request. This may
   * involve validating any token that was supplied with the request (eg CAS, OpenSSO)
   * @param request
   * @return
   */
  protected abstract String getUserName(HttpServletRequest request, HttpServletResponse response);

  

  /**
   * @return
   */
  public final boolean isValid() {
    return valid;
  }

  /**
   * @return
   */
  public final boolean isForceLogout() {
    return forceLogout;
  }

  /**
   * @return
   */
  public final Credentials getCredentials() {
    return credentials;
  }

  /**
   * @return
   */
  public final String getUserId() {
    return credentials.getUserID();
  }

}
