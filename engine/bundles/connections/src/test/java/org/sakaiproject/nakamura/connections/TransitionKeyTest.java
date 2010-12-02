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
package org.sakaiproject.nakamura.connections;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sakaiproject.nakamura.api.connections.ConnectionOperation;
import org.sakaiproject.nakamura.api.connections.ConnectionState;

import java.util.HashMap;
import java.util.Map;

;

/**
 * Test the hasing and equals for validity
 */
public class TransitionKeyTest {

  @Test
  public void testHashCode() {
    Map<Integer, TransitionKey> map = new HashMap<Integer, TransitionKey>();
    for (ConnectionState thisState : ConnectionState.values()) {
      for (ConnectionState otherState : ConnectionState.values()) {
        StatePair sp = new StatePairFinal(thisState, otherState);
        for (ConnectionOperation connectionOperation : ConnectionOperation.values()) {
          TransitionKey tk = new TransitionKey(sp, connectionOperation);
          if (map.containsKey(tk.hashCode())) {
            fail("Key Clash " + tk.toString());
          }
          map.put(tk.hashCode(), tk);
        }
      }
    }
    for (ConnectionState thisState : ConnectionState.values()) {
      for (ConnectionState otherState : ConnectionState.values()) {
        StatePair sp = new StatePairFinal(thisState, otherState);
        for (ConnectionOperation connectionOperation : ConnectionOperation.values()) {
          TransitionKey tk = new TransitionKey(sp, connectionOperation);
          TransitionKey ttk = map.get(tk.hashCode());
          assertTrue(tk.equals(ttk));
          assertTrue(tk.toString().equals(ttk.toString()));
          int c = 0;
          for (TransitionKey tkv : map.values()) {
            if (tk.equals(tkv)) {
              c++;
            }
          }
          assertEquals(1, c);
        }
      }
    }

  }

}
