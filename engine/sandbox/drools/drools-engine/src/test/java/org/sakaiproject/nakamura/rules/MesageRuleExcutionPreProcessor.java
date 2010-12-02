package org.sakaiproject.nakamura.rules;

import com.sample.DroolsTest.Message;

import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionPreProcessor;

import java.util.HashMap;
import java.util.Map;

public class MesageRuleExcutionPreProcessor implements RuleExecutionPreProcessor {

  public Map<RulesObjectIdentifier, Object> getAdditonalGlobals(RuleContext ruleContext) {
    Map<RulesObjectIdentifier, Object> inputs = new HashMap<RulesObjectIdentifier, Object>();
    // this should be ignored and not cause a failure
    RulesObjectIdentifier invalid = new RulesObjectIdentifier("ignore-this-global", null);
    inputs.put(invalid, new Object());
    return inputs;
  }

  public Map<RulesObjectIdentifier, Object> getAdditonalInputs(RuleContext ruleContext) {
    Map<RulesObjectIdentifier, Object> inputs = new HashMap<RulesObjectIdentifier, Object>();
    RulesObjectIdentifier ihello = new RulesObjectIdentifier("message", null);
    Message message = new Message();
    message.setStatus(Message.HELLO);
    message.setMessage("Hi there");
    inputs.put(ihello, message);
    RulesObjectIdentifier igoodby = new RulesObjectIdentifier("goodbyMessage", null);
    Message goodbyMessage = new Message();
    message.setStatus(Message.GOODBYE);
    message.setMessage("Bye");
    inputs.put(igoodby, goodbyMessage);
    return inputs;
  }

}
