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

import org.drools.KnowledgeBase;
import org.drools.RuleBaseFactory;
import org.drools.common.InternalRuleBase;
import org.drools.definition.KnowledgePackage;
import org.drools.definition.process.Process;
import org.drools.definition.rule.Rule;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.rule.Package;
import org.drools.util.DroolsStreamUtils;
import org.sakaiproject.nakamura.api.rules.RuleConstants;
import org.sakaiproject.nakamura.api.rules.RulePackageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

/**
 * This class holds knowledge bases, and knows how to refresh the knowledge base when the
 * source packages are refreshed or and dependent classloaders are reloaded.
 */
public class KnowledgeBaseHolder {

  private static final Logger LOGGER = LoggerFactory.getLogger(KnowledgeBaseHolder.class);
  public List<WeakReferenceClassloader> classloaders = new ArrayList<WeakReferenceClassloader>();
  private KnowledgeBaseImpl knowlegeBase;
  private long lastModified = 0;
  private InternalRuleBase ruleBase;

  public KnowledgeBaseHolder(Node ruleSetNode, RuleExecutionErrorListenerImpl errors)
      throws IOException, ClassNotFoundException, RepositoryException,
      InstantiationException, IllegalAccessException {
    load(ruleSetNode, true, errors);
  }

  private void load(Node ruleSetNode, boolean force, RuleExecutionErrorListenerImpl errors)
      throws IOException, ClassNotFoundException, RepositoryException,
      InstantiationException, IllegalAccessException {
    // not in the cache, create a knowledge base.
    // there are 2 ways of creating a rule set.
    // one is to use a resource from another bundle and also use its classloader
    // the other is to load the package from the content system

    if (force || knowlegeBase == null || !checkClassloaders()) {
      long currentLastModified = getLastModified(ruleSetNode);
      if (currentLastModified > lastModified) {
        LOGGER
            .info("{}Loading {} ", lastModified == 0 ? "" : "Re", ruleSetNode.getPath());
        classloaders.clear();

        InternalRuleBase newRuleBase = (InternalRuleBase) RuleBaseFactory.newRuleBase();

        NodeIterator ni = ruleSetNode.getNodes();
        for (; ni.hasNext();) {
          Node n = ni.nextNode();
          if (NodeType.NT_FILE.equals(n.getPrimaryNodeType().getName())) {
            LOGGER.info("Loading File from JCR node {} ", n.getPath());
            InputStream in = null;
            try {
              in = n.getNode(Node.JCR_CONTENT).getProperty(Property.JCR_DATA).getBinary()
                  .getStream();
              Object o = DroolsStreamUtils.streamIn(in);
              newRuleBase.addPackage((Package) o);
            } finally {
              try {
                in.close();
              } catch (Exception e) {

              }
            }
          } else if (n.hasProperty(RuleConstants.PROP_SAKAI_BUNDLE_LOADER_CLASS)) {
            String bundleLoaderClass = n.getProperty(
                RuleConstants.PROP_SAKAI_BUNDLE_LOADER_CLASS).getString();
            @SuppressWarnings("unchecked")
            Class<RulePackageLoader> ruleLoaderCLass = (Class<RulePackageLoader>) this
                .getClass().getClassLoader().loadClass(bundleLoaderClass);
            RulePackageLoader rpl = ruleLoaderCLass.newInstance();
            LOGGER.info("Loaded RulePackageLoader Class {} as {} ", bundleLoaderClass,
                rpl);

            InputStream in = rpl.getPackageInputStream();
            try {
              WeakReferenceClassloader wrc = new WeakReferenceClassloader(
                  rpl.getPackageClassLoader());
              classloaders.add(wrc);
              Object o = DroolsStreamUtils.streamIn(in, wrc);
              newRuleBase.addPackage((Package) o);
            } finally {
              try {
                in.close();
              } catch (Exception e) {

              }
            }
          }
        }
        lastModified = currentLastModified;

        knowlegeBase = new KnowledgeBaseImpl(newRuleBase);
        ruleBase = newRuleBase;

        logRuleBaseStructure();
      }
    }
  }

  /**
   * Output the structure of the rule base.
   */
  private void logRuleBaseStructure() {
    if (LOGGER.isInfoEnabled()) {
      Collection<KnowledgePackage> kps = knowlegeBase.getKnowledgePackages();
      for (KnowledgePackage kp : kps) {
        LOGGER.info("Package  {}", kp.getName());
        Collection<Rule> rules = kp.getRules();
        for (Rule rule : rules) {
          LOGGER.info("  rule {}:{} ", rule.getPackageName(), rule.getName());
          Collection<String> metas = rule.listMetaAttributes();
          for (String attr : metas) {
            LOGGER.info("    has {} ", attr);
          }
        }
        Collection<Process> processes = kp.getProcesses();
        for (Process process : processes) {
          LOGGER.info("  process {}:{}:{}:{} ", new Object[] { process.getPackageName(),
              process.getName(), process.getVersion(), process.getType() });
        }
      }
      Set<String> globalNames = getGlobals().keySet();
      LOGGER.info("Globals defined by knowledge base are {} ",
          Arrays.toString(globalNames.toArray()));
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, Object> getGlobals() {
    return ruleBase.getGlobals();
  }

  /**
   * @return true if classloaders are current and valid.
   */
  private boolean checkClassloaders() {
    for (WeakReferenceClassloader wrc : classloaders) {
      if (!wrc.isAvailable()) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param ruleSetNode
   * @return the last modified date of any resource in the rule set.
   * @throws RepositoryException
   */
  private long getLastModified(Node ruleSetNode) throws RepositoryException {
    NodeIterator ni = ruleSetNode.getNodes();
    long curentLastModified = 0;
    for (; ni.hasNext();) {
      Node n = ni.nextNode();
      if (NodeType.NT_FILE.equals(n.getPrimaryNodeType().getName())) {
        try {
          curentLastModified = Math.max(curentLastModified, n.getNode(Node.JCR_CONTENT)
              .getProperty(Property.JCR_LAST_MODIFIED).getDate().getTimeInMillis());
        } catch (Exception ex) {
          LOGGER.debug("Cant find last modified time ", ex);
        }
      } else if (n.hasProperty(RuleConstants.PROP_SAKAI_BUNDLE_LOADER_CLASS)) {
        try {
          curentLastModified = Math.max(curentLastModified,
              n.getProperty(Property.JCR_LAST_MODIFIED).getDate().getTimeInMillis());
        } catch (Exception ex) {
          LOGGER.debug("Cant find last modified time ", ex);
        }
      }
    }
    return curentLastModified;
  }

  /**
   * reload the rule set identified by the node.
   * 
   * @param ruleSetNode
   * @param errors
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws RepositoryException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public void refresh(Node ruleSetNode, RuleExecutionErrorListenerImpl errors)
      throws IOException, ClassNotFoundException, RepositoryException,
      InstantiationException, IllegalAccessException {
    load(ruleSetNode, false, errors);
  }

  public KnowledgeBase getKnowledgeBase() {
    return knowlegeBase;
  }

}
