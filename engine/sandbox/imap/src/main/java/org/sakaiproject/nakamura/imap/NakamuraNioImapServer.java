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
package org.sakaiproject.nakamura.imap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.james.dnsserver.DNSServer;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.main.ImapRequestStreamHandler;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.ImapStreamChannelUpstreamHandler;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.jcr.GlobalMailboxSessionJCRRepository;
import org.apache.james.mailbox.jcr.JCRMailboxManager;
import org.apache.james.mailbox.jcr.JCRMailboxSessionMapperFactory;
import org.apache.james.mailbox.jcr.JCRSubscriptionManager;
import org.apache.james.mailbox.jcr.JCRVmNodeLocker;
import org.apache.james.mailbox.store.Authenticator;
import org.apache.james.socket.netty.AbstractConfigurableAsyncServer;
import org.apache.sling.jcr.api.SlingRepository;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.connection.ConnectionLimitUpstreamHandler;
import org.jboss.netty.handler.connection.ConnectionPerIpLimitUpstreamHandler;
import org.jboss.netty.handler.ssl.SslHandler;
import org.osgi.service.component.ComponentContext;

import java.net.URL;

/**
 *
 */
@Component
public class NakamuraNioImapServer extends AbstractConfigurableAsyncServer implements ImapConstants {

  private static final Log jcLog = SLF4JLogFactory.getLog(NakamuraNioImapServer.class);

  @Reference
  private SlingRepository slingRepository;

  private DNSServer dnsServer;

  private static final String softwaretype = "JAMES/Sakai Nakamura "+VERSION+" Server "; //+ Constants.SOFTWARE_VERSION;

  private String hello;
  private ImapProcessor processor;
  private ImapEncoder encoder;

  private ImapDecoder decoder;

  public void activate(ComponentContext componentContext) throws Exception {

    dnsServer = new DNSServer();
    URL dnsConfigUrl = getClass().getResource("/dns-config.xml");
    if ( dnsConfigUrl == null ) {
      throw new IllegalArgumentException("Unalbe to configure DNS server, cant find configuration file dns-config.xml");
    }
    DefaultConfigurationBuilder dnsConfig = new DefaultConfigurationBuilder(dnsConfigUrl);
    dnsConfig.load();
    dnsServer.setLog(jcLog);
    dnsServer.configure(dnsConfig);
    dnsServer.init();

    GlobalMailboxSessionJCRRepository sessionRepos = new GlobalMailboxSessionJCRRepository(slingRepository, null, "admin", "admin");

    // Register imap cnd file org/apache/james/imap/jcr/imap.cnd when the bundle starts up
    // JCRUtils.registerCnd(repository, workspace, user, pass);

    Authenticator userManager = new Authenticator() {

      public boolean isAuthentic(String userid, CharSequence passwd) {
        System.err.println("Said "+userid+" with password "+passwd+" was Ok");
        return true; // delegate authentication to JCR, the JCR session should use the same user as the imap session. We might want to
        // integrate this with the sling authentication handlers.
      }
    };

    //TODO: Fix the scaling stuff so the tests will pass with max scaling too
    // TODO: this node locker will only work with non-clustered jcr per the developer's
    // notes in the class
    JCRVmNodeLocker nodeLocker = new JCRVmNodeLocker();
    JCRMailboxSessionMapperFactory jcrMailboxSessionMapperFactory = new JCRMailboxSessionMapperFactory(sessionRepos, nodeLocker);
    JCRSubscriptionManager jcrSubscriptionManager = new JCRSubscriptionManager(jcrMailboxSessionMapperFactory);
//    MailboxManager mailboxManager = new JCRMailboxManager( jcrMailboxSessionMapperFactory, userManager, jcrSubscriptionManager );
    MailboxManager mailboxManager = new JCRMailboxManager( jcrMailboxSessionMapperFactory, userManager, nodeLocker );

    MailboxSession session = mailboxManager.createSystemSession("test", jcLog);
    mailboxManager.startProcessingRequest(session);
    //mailboxManager.deleteEverything(session);
    mailboxManager.endProcessingRequest(session);
    mailboxManager.logout(session, false);

    decoder = new DefaultImapDecoderFactory().buildImapDecoder();
    encoder = new DefaultImapEncoderFactory().buildImapEncoder();
    processor = DefaultImapProcessorFactory.createDefaultProcessor(mailboxManager,
        jcrSubscriptionManager);
    setDNSService(dnsServer);
    setLog(jcLog);
    URL configUrl = getClass().getResource("/imap-config.xml");
    if ( configUrl == null ) {
      throw new IllegalArgumentException("Unalbe to configure IMAP server, cant find configuration file imap-config.xml");
    }
    DefaultConfigurationBuilder config = new DefaultConfigurationBuilder(configUrl);
    config.load();
    configure(config);
    init();
  }


  public void deactivate(ComponentContext componentContext) {

    destroy();
  }


  @Override
  public void doConfigure( final HierarchicalConfiguration configuration ) throws ConfigurationException {
      super.doConfigure(configuration);
      hello  = softwaretype + " Server " + getHelloName() + " is ready.";
  }


  /*
   * (non-Javadoc)
   * @see org.apache.james.socket.mina.AbstractAsyncServer#getDefaultPort()
   */
  @Override
  public int getDefaultPort() {
      return 143;
  }


  /*
   * (non-Javadoc)
   * @see org.apache.james.socket.mina.AbstractAsyncServer#getServiceType()
   */
  @Override
  public String getServiceType() {
      return "IMAP Service";
  }

  @Override
  protected ChannelPipelineFactory createPipelineFactory(ChannelGroup channelGroup) {
      return new ChannelPipelineFactory() {

          public ChannelPipeline getPipeline() throws Exception {
              ChannelPipeline pipeline = new DefaultChannelPipeline();
              pipeline.addLast("connectionLimit", new ConnectionLimitUpstreamHandler(NakamuraNioImapServer.this.connectionLimit));

              pipeline.addLast("connectionPerIpLimit", new ConnectionPerIpLimitUpstreamHandler(NakamuraNioImapServer.this.connPerIP));

              if (isSSLSocket()) {
                  pipeline.addFirst("sslHandler", new SslHandler(getSSLContext().createSSLEngine()));
              }
              final ImapRequestStreamHandler handler = new ImapRequestStreamHandler(decoder, processor, encoder);

              pipeline.addLast("coreHandler",  new ImapStreamChannelUpstreamHandler(hello, handler, getLogger(), NakamuraNioImapServer.this.getTimeout()));
              return pipeline;
          }

      };
  }


}
