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
package org.sakaiproject.nakamura.basiclti;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.basiclti.BasicLTIContextIdResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * The default mapping strategy will be to simply return the {@link Node#getPath()} as the
 * LTI context_id. For example: <code>/sites/12345</code>.
 * 
 * If the {@link Node} has a special property, <code>lti_context_id</code>, then use the
 * value of that property as the context_id. See: {@link #LTI_CONTEXT_ID}.
 * 
 */
@Component(immediate = true, metatype = true)
@Service
public class DefaultContextIdResolver implements BasicLTIContextIdResolver {
  private static final Logger LOG = LoggerFactory
      .getLogger(DefaultContextIdResolver.class);

  /**
   * The {@link Node} property key used to store an LTI context_id.
   */
  @Property(value = "lti_context_id", name = "LTI_CONTEXT_ID", label = "key", description = "The Node property key used to store an LTI context_id.")
  public static final String LTI_CONTEXT_ID = DefaultContextIdResolver.class.getName()
      + ".key";
  private String key = "lti_context_id";

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * 
   * @see org.sakaiproject.nakamura.api.basiclti.BasicLTIContextIdResolver#resolveContextId(javax.jcr.Node)
   */
  public String resolveContextId(final Node node) throws RepositoryException {
    LOG.debug("resolveContextId(Node {})", node);
    if (node == null) {
      throw new IllegalArgumentException("Node cannot be null");
    }

    String contextId = null;
    if (node.hasProperty(key)) {
      // we have a special context_id we can use
      contextId = node.getProperty(key).getString();
    } else {
      // just use the path
      contextId = node.getPath();
    }
    return contextId;
  }

  protected void activate(ComponentContext context) {
    @SuppressWarnings("rawtypes")
    final Dictionary props = context.getProperties();
    if (props.get(LTI_CONTEXT_ID) != null) {
      key = (String) props.get(LTI_CONTEXT_ID);
    }
  }
}
