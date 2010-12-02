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
package org.sakaiproject.nakamura.proxy;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.sakaiproject.nakamura.util.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SignatureException;
import java.util.Dictionary;
import java.util.Map;

/**
 * This pre processor adds a header to the proxy request that is picked up by the far end
 * to identify the users. The far end has to a) share the same shared token and b) have
 * something to decode the token. The class was originally designed to work with a
 * TrustedTokenLoginFilter for Sakai 2, but the handshake protocol is so simple it could
 * be used with any end point. There is one configuration item, the sharedSecret that must
 * match the far end. At the moment this component is configured to be a singleton service
 * but if this mechanism of authenticating proxies becomes wide spread we may want this
 * class to be come a service factory so that we can support many trust relationships.
 *
 */
@Service(value = ProxyPreProcessor.class)
@Component(metatype = true, immediate = true)
@Properties(value = {
    @Property(name = "service.description", value = { "Pre processor for proxy requests to Sakai 2 instance with a trusted token filter." }),
    @Property(name = "service.vendor", value = { "The Sakai Foundation" }) })
public class TrustedLoginTokenProxyPreProcessor implements ProxyPreProcessor {

  public static final String SECURE_TOKEN_HEADER_NAME = "x-sakai-token";
  public static final String TOKEN_SEPARATOR = ";";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TrustedLoginTokenProxyPreProcessor.class);

  @Property(name = "sharedSecret")
  private String sharedSecret = "e2KS54H35j6vS5Z38nK40";

  @Property(name = "port", intValue = 80)
  protected int port;

  @Property(name = "hostname", value = {"localhost"})
  protected String hostname;

  public String getName() {
    return "trusted-token";
  }

  public void preProcessRequest(SlingHttpServletRequest request,
      Map<String, String> headers, Map<String, Object> templateParams) {

    String user = request.getRemoteUser();
    String hmac;
    final String message = user + TOKEN_SEPARATOR + System.currentTimeMillis();
    try {
      hmac = Signature.calculateRFC2104HMAC(message, sharedSecret);
    } catch (SignatureException e) {
      LOGGER.error(e.getLocalizedMessage(), e);
      throw new Error(e);
    }
    final String token = hmac + TOKEN_SEPARATOR + message;
    headers.put(SECURE_TOKEN_HEADER_NAME, token);

    templateParams.put("port", port);
    templateParams.put("hostname", hostname);
  }

  /**
   * When the bundle gets activated we retrieve the OSGi properties.
   *
   * @param context
   */
  @SuppressWarnings("rawtypes")
  protected void activate(ComponentContext context) {
    // Get the properties from the console.
    Dictionary props = context.getProperties();
    if (props.get("sharedSecret") != null) {
      sharedSecret = props.get("sharedSecret").toString();
    }
    if (props.get("hostname") != null) {
      hostname = props.get("hostname").toString();
      LOGGER.info("Sakai 2 hostname: " + hostname);
    }
    if (props.get("port") != null) {
      try {
        port = Integer.parseInt(props.get("port").toString());
        LOGGER.info("Sakai 2 port: " + port);
      } catch (NumberFormatException e) {
        LOGGER.warn("Failed to cast the sakai 2 port from the properties.", e);
      }
    }
  }

}
