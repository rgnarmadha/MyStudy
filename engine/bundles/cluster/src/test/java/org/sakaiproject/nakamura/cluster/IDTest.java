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
package org.sakaiproject.nakamura.cluster;

import junit.framework.Assert;

import org.junit.Test;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class IDTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(IDTest.class);
  private Map<BigInteger, BigInteger> hash = new ConcurrentHashMap<BigInteger, BigInteger>();
  private Object lockObject = new Object();
  private int nrunning = 0;
  protected int failed;


  @Test
  public void testId() {

    for (int i = 0; i < 100; i++) {
      final int tstart = i;
      Thread t = new Thread(new Runnable() {

        private long epoch;
        private long next;
        public void run() {
          GregorianCalendar calendar = new GregorianCalendar(2010, 8, 6);
          epoch = calendar.getTimeInMillis();
          synchronized (lockObject) {
            nrunning++;
          }
          try {
            for (int j = 0; j < 100; j++) {
              BigInteger id = getId();
              if ( hash.containsKey(id) ) {
                failed++;
              }
              hash.put(id, id);
            }
          } finally {
            synchronized (lockObject) {
              nrunning--;
            }
          }

        }
        private BigInteger getId() {
          synchronized (lockObject) {
            if (next == 0) {
              next = System.currentTimeMillis() - epoch;
            } else {
              next++;
            }
          }
          BigInteger ret = new BigInteger(String.valueOf(next)+ String.valueOf(tstart + 1000));
          return ret;
        }

      });
      t.start();
    }

    do {
      LOGGER.info("Running " + nrunning + " Hash Size is " + hash.size());
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        LOGGER.info(e.getMessage(), e);
      }
    } while (nrunning > 0);

    for (Entry<BigInteger, BigInteger> e : hash.entrySet()) {
      LOGGER.info(" Entry is "
          + e.getValue()
          + " "
          + StringUtils.encode(e.getValue().toByteArray(), StringUtils.URL_SAFE_ENCODING)
              .trim());

    }
    LOGGER.info("Finished Running,  Hash Size is " + hash.size()+ " Collisions "+failed);
    Assert.assertEquals(0, failed);

  }


}
