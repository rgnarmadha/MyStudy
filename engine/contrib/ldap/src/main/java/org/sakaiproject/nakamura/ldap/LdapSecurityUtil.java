package org.sakaiproject.nakamura.ldap;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPJSSEStartTLSFactory;
import com.novell.ldap.LDAPSocketFactory;

import org.sakaiproject.nakamura.api.ldap.LdapConnectionManagerConfig;
import org.sakaiproject.nakamura.api.ldap.LdapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class LdapSecurityUtil {
  private static final Logger log = LoggerFactory.getLogger(LdapUtil.class);

  /**
   * Initializes an LDAP socket factory is a non-default socket factory is needed. This
   * scenario becomes relevant when needing to connect using SSL or TLS. If no special
   * socket factory is needed, null is returned which is safe to provide to the
   * constructor of {@link LDAPConnection} or to
   * {@link LDAPConnection#setSocketFactory(LDAPSocketFactory)}.
   *
   * @param config
   *          The configuration used for connecting.
   * @return The proper socket factory based on the provided configuration. null if
   *         special socket factory is required.
   */
  public static LDAPSocketFactory initLDAPSocketFactory(LdapConnectionManagerConfig config) {
    LDAPSocketFactory socketFactory = null;

    if (config.isSecureConnection() || config.isTLS()) {
      log.debug("init(): initializing secure socket factory");
      try {
        // initialize the keystore which will create an SSL context by which
        // socket factories can be created. this allows for multiple keystores
        // to be managed without the use of system properties.
        SSLContext ctx = initKeystore(config.getKeystoreLocation(),
            config.getKeystorePassword());
        SSLSocketFactory sslSocketFactory = ctx.getSocketFactory();
        if (config.isTLS()) {
          socketFactory = new LDAPJSSEStartTLSFactory(sslSocketFactory);
        } else {
          socketFactory = new LDAPJSSESecureSocketFactory(sslSocketFactory);
        }
      } catch (GeneralSecurityException e) {
        log.error(e.getMessage(), e);
        throw new RuntimeException(e.getMessage(), e);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    return socketFactory;
  }

  /**
   * Loads a keystore and sets up an SSL context that can be used to create socket
   * factories that use the suggested keystore.
   *
   * @param keystoreLocation
   * @param keystorePassword
   * @throws CertificateException
   * @throws KeyStoreException
   * @throws NoSuchProviderException
   * @throws NoSuchAlgorithmException
   * @throws IOException
   * @throws KeyManagementException
   * @throws NullPointerException
   *           if a non-null keystore location cannot be resolved
   */
  public static SSLContext initKeystore(String keystoreLocation, String keystorePassword)
      throws GeneralSecurityException, IOException {
    FileInputStream fis = new FileInputStream(keystoreLocation);
    char[] passChars = (keystorePassword != null) ? keystorePassword.toCharArray() : null;
    TrustManager[] myTM = new TrustManager[] { new LdapX509TrustManager(fis, passChars) };
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, myTM, null);
    return ctx;
  }
}
