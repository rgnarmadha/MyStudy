package org.sakaiproject.nakamura.rules;

import org.drools.audit.WorkingMemoryLogger;
import org.drools.audit.event.LogEvent;
import org.drools.event.KnowledgeRuntimeEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuleExecutionLogger extends WorkingMemoryLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuleExecutionLogger.class);
  private String rulePath;
  private boolean debugRule;

  public RuleExecutionLogger(KnowledgeRuntimeEventManager ksession, String rulePath,
      boolean debugRule) {
    super(ksession);
    this.rulePath = rulePath;
    this.debugRule = debugRule;
  }

  @Override
  public void logEventCreated(LogEvent logEvent) {
    if (debugRule) {
      LOGGER.info("Rule Execution Log for {} {} ", rulePath, logEvent);
    } else {
      LOGGER.debug("Rule Execution Log for {} {} ", rulePath, logEvent);
    }
  }

}
