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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.drools.KnowledgeBase;
import org.drools.command.Command;
import org.drools.command.CommandFactory;
import org.drools.runtime.ExecutionResults;
import org.drools.runtime.StatelessKnowledgeSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.rules.RuleConstants;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionErrorListener;
import org.sakaiproject.nakamura.api.rules.RuleExecutionException;
import org.sakaiproject.nakamura.api.rules.RuleExecutionPreProcessor;
import org.sakaiproject.nakamura.api.rules.RuleExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * A Drools implementation of the Rule Execution Service.
 */
@Component(label = "Drools Rule Execution Service", description = "Provides Rule Execution using Drools Knowledgebases")
@Service(value = RuleExecutionService.class)
@Reference(name = "processor", bind = "bindProcessor", unbind = "unbindProcessor", cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC, strategy = ReferenceStrategy.EVENT, referenceInterface = RuleExecutionPreProcessor.class)
public class RuleExecutionServiceImpl implements RuleExecutionService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(RuleExecutionServiceImpl.class);

  private KnowledgeBaseFactory knowledgeBaseFactory;

  private BundleContext bundleContext;

  private Map<String, ServiceReference> processorReferences = new HashMap<String, ServiceReference>();

  private Map<String, RuleExecutionPreProcessor> processors = new ConcurrentHashMap<String, RuleExecutionPreProcessor>();

  protected void activate(ComponentContext context) {
    knowledgeBaseFactory = new KnowledgeBaseFactory();
    BundleContext bundleContext = context.getBundleContext();
    synchronized (processorReferences) {
      processors.clear();
      for (Entry<String, ServiceReference> e : processorReferences.entrySet()) {
        RuleExecutionPreProcessor repp = (RuleExecutionPreProcessor) bundleContext
            .getService(e.getValue());
        if (repp != null) {
          processors.put(e.getKey(), repp);
        }
      }
      processorReferences.clear();
      this.bundleContext = bundleContext;
    }
  }

  protected void deactivate(ComponentContext componentContext) {
    synchronized (processorReferences) {
      processors.clear();
      processorReferences.clear();
      this.bundleContext = null;
    }
    knowledgeBaseFactory = null;
  }

  /**
   * {@inheritDoc}
   * 
   * @param ruleContext
   * @throws RuleExecutionException
   * @see org.sakaiproject.nakamura.api.rules.RuleExecutionService#executeRuleSet(java.lang.String,
   *      org.apache.sling.api.SlingHttpServletRequest)
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> executeRuleSet(String pathToRuleSet,
      SlingHttpServletRequest request, Resource resource, RuleContext ruleContext,
      RuleExecutionErrorListener userErrorListener) throws RuleExecutionException {
    ResourceResolver resourceResolver = request.getResourceResolver();
    Resource ruleSet = resourceResolver.getResource(pathToRuleSet);
    if (ruleSet != null && RuleConstants.SAKAI_RULE_SET.equals(ruleSet.getResourceType())) {
      try {
        Node ruleSetNode = ruleSet.adaptTo(Node.class);
        RuleExecutionErrorListenerImpl errors = new RuleExecutionErrorListenerImpl(
            userErrorListener);
        KnowledgeBaseHolder knowledgeBaseHolder = knowledgeBaseFactory.getKnowledgeBase(
            ruleSetNode, errors);
        if (errors.hasErrorMessages()) {
          errors.listErrorMessages();
          throw new RuleExecutionException(errors.getErrorMessages(),
              "Failed to load rule set " + pathToRuleSet);
        }
        KnowledgeBase knowledgeBase = knowledgeBaseHolder.getKnowledgeBase();
        if ( knowledgeBase == null ) {
          throw new RuleExecutionException(errors.getErrorMessages(),
              "Failed to load rule set, no knowledgebase " + pathToRuleSet);          
        }
        StatelessKnowledgeSession ksession = knowledgeBase.newStatelessKnowledgeSession();
        @SuppressWarnings("unused")
        RuleExecutionLogger logger = new RuleExecutionLogger(ksession, pathToRuleSet, ruleSetNode.hasProperty(RuleConstants.PROP_SAKAI_RULE_DEBUG));
        Session session = resourceResolver.adaptTo(Session.class);

        Set<String> globalNames = knowledgeBaseHolder.getGlobals().keySet();

        List<Command<?>> cmds = new ArrayList<Command<?>>();
        conditionallyAddGlobal(globalNames, cmds, "session", session, false, false,
            errors);
        conditionallyAddGlobal(globalNames, cmds, "request", request, false, false,
            errors);
        conditionallyAddGlobal(globalNames, cmds, "resource", resource, false, false,
            errors);
        conditionallyAddGlobal(globalNames, cmds, "ruleset", ruleSet, false, false,
            errors);
        conditionallyAddGlobal(globalNames, cmds, "resourceResolver", resourceResolver,
            false, false, errors);
        conditionallyAddGlobal(globalNames, cmds, "currentUser", session.getUserID(),
            false, false, errors);
        conditionallyAddGlobal(globalNames, cmds, "results",
            new HashMap<String, Object>(), true, true, errors); // add an out parameter
        
        

        // add other globals and input instances with the RuleExecutionPreProcessor ....
        RuleExecutionPreProcessor preProcessor = getProcessor(ruleSetNode, errors);
        if (preProcessor != null) {

          Map<RulesObjectIdentifier, Object> globals = preProcessor
              .getAdditonalGlobals(ruleContext);
          for (Entry<RulesObjectIdentifier, Object> g : globals.entrySet()) {
            String in = g.getKey().getInIdentifier();
            String out = g.getKey().getOutIdentifier();
            if (out != null) {
              conditionallyAddGlobal(globalNames, cmds, in, g.getValue(), out, errors);
            } else {
              conditionallyAddGlobal(globalNames, cmds, in, g.getValue(), false, false,
                  errors);
            }
          }

          Map<RulesObjectIdentifier, Object> inputs = preProcessor
              .getAdditonalInputs(ruleContext);
          for (Entry<RulesObjectIdentifier, Object> g : inputs.entrySet()) {
            String out = g.getKey().getOutIdentifier();
            if (out != null) {
              cmds.add(CommandFactory.newInsert(g.getValue(), out));
            } else {
              Object o = g.getValue();
              LOGGER.info("Adding as insert "+o);
              cmds.add(CommandFactory.newInsert(o));
            }
          }
        }
        
        

        if (errors.hasErrorMessages()) {
          errors.listErrorMessages();
          throw new RuleExecutionException(errors.getErrorMessages(),
              "Unable to execute rule at " + pathToRuleSet + " due to previous Errors");
        }
        
        // Fire all the rules
        ExecutionResults results = ksession.execute(CommandFactory
            .newBatchExecution(cmds));

        Map<String, Object> resultsMap = (Map<String, Object>) results
            .getValue("results"); // returns the Map containing the results.
        return resultsMap;
      } catch (IllegalStateException e) {
        throw new RuleExecutionException(null, e.getMessage(), e);
      } catch (RepositoryException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
        throw new RuleExecutionException(null, e.getMessage(), e);
      } catch (IOException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
        throw new RuleExecutionException(null, e.getMessage(), e);
      } catch (ClassNotFoundException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
        throw new RuleExecutionException(null, e.getMessage(), e);
      } catch (InstantiationException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
        throw new RuleExecutionException(null, e.getMessage(), e);
      } catch (IllegalAccessException e) {
        LOGGER.info("Failed to invoke rule {} ", pathToRuleSet, e);
        throw new RuleExecutionException(null, e.getMessage(), e);
      }

    }
    return null;
  }

  private void conditionallyAddGlobal(Set<String> globalNames, List<Command<?>> cmds,
      String globalName, Object global, boolean out, boolean required,
      RuleExecutionErrorListener errors) {
    if (globalNames.contains(globalName)) {
      cmds.add(CommandFactory.newSetGlobal(globalName, global, out));
    } else {
      if (required) {
        errors
            .error("Required global "
                + globalName
                + " has not been defined in the rules set, it must be preset, please add with a \"global "
                + global.getClass() + " " + globalName
                + ";\" in at least one of the rules definitions ");
      }
      LOGGER.info("Didnt add global {} as it was not a global ", globalName, globalName);
    }
  }

  private void conditionallyAddGlobal(Set<String> globalNames, List<Command<?>> cmds,
      String globalName, Object global, String outIdentifier,
      RuleExecutionErrorListener errors) {
    if (globalNames.contains(globalName) && globalNames.contains(outIdentifier)) {
      cmds.add(CommandFactory.newSetGlobal(globalName, global, outIdentifier));
    } else {
      LOGGER.info("Didnt add global {} as either {} or {} was not a global ",
          new Object[] { globalName, globalName, outIdentifier });
    }
  }

  /**
   * @param ruleSetNode
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public RuleExecutionPreProcessor getProcessor(Node ruleSetNode, RuleExecutionErrorListener errors)
      throws RepositoryException {
    if (ruleSetNode.hasProperty(RuleConstants.PROP_SAKAI_RULE_EXECUTION_PREPROCESSOR)) {
      String preprocessorName = ruleSetNode.getProperty(
          RuleConstants.PROP_SAKAI_RULE_EXECUTION_PREPROCESSOR).getString();
      RuleExecutionPreProcessor preprocessor = processors.get(preprocessorName);
      if (preprocessor == null ) {
        errors.error("Pre Processor "+preprocessorName+" was not found");
      }
      return preprocessor;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
   */
  protected void bindProcessor(ServiceReference reference) {
    String name = (String) reference.getProperty(RuleConstants.PROCESSOR_NAME);
    synchronized (processorReferences) {
      if (bundleContext == null) {
        processorReferences.put(name, reference);
      } else {
        RuleExecutionPreProcessor o = (RuleExecutionPreProcessor) bundleContext
            .getService(reference);
        processors.put(name, o);
      }
    }
  }

  protected void unbindProcessor(ServiceReference reference) {
    String name = (String) reference.getProperty(RuleConstants.PROCESSOR_NAME);
    synchronized (processorReferences) {
      if (bundleContext == null) {
        processorReferences.remove(name);
      } else {
        processors.remove(name);
      }
    }
  }

}
