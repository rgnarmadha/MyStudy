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
package org.sakaiproject.nakamura.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An Implementation of a Future that is available immediately.
 */
public class ImmediateFuture<T>  implements Future<T> {

  
  private T result;

  public ImmediateFuture(T result) {
    this.result = result;
  }
  /**
   * {@inheritDoc}
   * @see java.util.concurrent.Future#cancel(boolean)
   */
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  /**
   * {@inheritDoc}
   * @see java.util.concurrent.Future#get()
   */
  public T get() throws InterruptedException, ExecutionException {
    return result;
  }

  /**
   * {@inheritDoc}
   * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
   */
  public T get(long timeout, TimeUnit unit) throws InterruptedException {
    return result;
  }

  /**
   * {@inheritDoc}
   * @see java.util.concurrent.Future#isCancelled()
   */
  public boolean isCancelled() {
    return false;
  }

  /**
   * {@inheritDoc}
   * @see java.util.concurrent.Future#isDone()
   */
  public boolean isDone() {
    return true;
  }

}
