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
package org.sakaiproject.nakamura.proxy.velocity;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy class from velocity to slf4j logging.
 */
public class VelocityLogger implements LogChute {

  private Logger logger;

  /**
   * @param class1
   */
  public VelocityLogger(Class<?> toLogClass) {
    logger = LoggerFactory.getLogger(toLogClass);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.log.LogChute#init(org.apache.velocity.runtime.RuntimeServices)
   */
  public void init(RuntimeServices arg0) throws Exception {
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.log.LogChute#isLevelEnabled(int)
   */
  public boolean isLevelEnabled(int level) {
    switch (level) {
    case LogChute.DEBUG_ID:
      return logger.isDebugEnabled();
    case LogChute.ERROR_ID:
      return logger.isErrorEnabled();
    case LogChute.INFO_ID:
      return logger.isInfoEnabled();
    case LogChute.TRACE_ID:
      return logger.isTraceEnabled();
    case LogChute.WARN_ID:
      return logger.isWarnEnabled();
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.log.LogChute#log(int, java.lang.String)
   */
  public void log(int level, String msg) {
    switch (level) {
    case LogChute.DEBUG_ID:
      logger.debug(msg);
      break;
    case LogChute.ERROR_ID:
      logger.error(msg);
      break;
    case LogChute.INFO_ID:
      logger.info(msg);
      break;
    case LogChute.TRACE_ID:
      logger.trace(msg);
      break;
    case LogChute.WARN_ID:
      logger.warn(msg);
      break;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.velocity.runtime.log.LogChute#log(int, java.lang.String,
   *      java.lang.Throwable)
   */
  public void log(int level, String msg, Throwable t) {
    switch (level) {    
    case LogChute.DEBUG_ID:
      logger.debug(msg, t);
      break;
    case LogChute.ERROR_ID:
      logger.error(msg, t);
      break;
    case LogChute.INFO_ID:
      logger.info(msg, t);
      break;
    case LogChute.TRACE_ID:
      logger.trace(msg, t);
      break;
    case LogChute.WARN_ID:
      logger.warn(msg, t);
      break;
    }
  }

}
