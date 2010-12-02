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
package org.sakaiproject.nakamura.auth.trusted.validator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferenceStrategy;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidatorService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service tracker that contains validators indexed by name.
 */
@Component(immediate=true)
@Service(value=RequestTrustValidatorService.class)
@Reference(bind="bindRequestValidator",unbind="unbindRequestValidator",cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,name="requestValidators",policy=ReferencePolicy.DYNAMIC, referenceInterface=RequestTrustValidator.class, strategy=ReferenceStrategy.EVENT)
public class RequestTrustValidatorServiceImpl implements RequestTrustValidatorService {

  
  private Map<String, RequestTrustValidator> validators = new ConcurrentHashMap<String, RequestTrustValidator>();


  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidatorService#getValidator(java.lang.String)
   */
  public RequestTrustValidator getValidator(String name) {
    return validators.get(name);
  }

  public void bindRequestValidator(ServiceReference reference) {
    RequestTrustValidator requestService = (RequestTrustValidator) reference.getBundle().getBundleContext().getService(reference);
    String name = (String) reference.getProperty(RequestTrustValidator.VALIDATOR_NAME);
    validators.put(name, requestService);
  }
  

  public void unbindRequestValidator(ServiceReference reference) {
    String name = (String) reference.getProperty(RequestTrustValidator.VALIDATOR_NAME);
    validators.remove(name);
    reference.getBundle().getBundleContext().ungetService(reference);
  }

  
}
