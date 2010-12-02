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

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * A factory that created rule Knowledge bases from JCR nodes.
 */
public class KnowledgeBaseFactory {

  Map<String, KnowledgeBaseHolder> cache = new WeakHashMap<String, KnowledgeBaseHolder>();

  /**
   * @param ruleSetNode
   * @param errors 
   * @return
   * @throws RepositoryException
   * @throws IOException
   * @throws IllegalAccessException 
   * @throws InstantiationException 
   * @throws ClassNotFoundException 
   */
  public KnowledgeBaseHolder getKnowledgeBase(Node ruleSetNode, RuleExecutionErrorListenerImpl errors) throws RepositoryException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    
    String key = ruleSetNode.getPath();
    synchronized (cache) {
      if ( cache.containsKey(key)) {
        KnowledgeBaseHolder kb = cache.get(key);
        if ( kb != null ) {
          kb.refresh(ruleSetNode, errors);
          return kb;
        }
      }
      KnowledgeBaseHolder kb = new KnowledgeBaseHolder(ruleSetNode, errors);
      cache.put(key, kb);
      return kb;
    }
  }
  
}
