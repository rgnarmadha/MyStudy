package org.sakaiproject.nakamura.ldap;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Trust manager implementation to allow for multiple keystores to be used when
 * creating LDAP connections. Implementation details derived from <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jsse/JSSERefGuide.html#X509TrustManager"
 * >here.</a>
 */
class LdapX509TrustManager implements X509TrustManager {

  /*
   * The default X509TrustManager returned by SunX509. We'll delegate decisions
   * to it, and fall back to the logic in this class if the default
   * X509TrustManager doesn't trust it.
   */
  X509TrustManager sunJSSEX509TrustManager;

  LdapX509TrustManager(InputStream keystore, char[] password) throws GeneralSecurityException,
      IOException {
    // SunX509, SunJSSE
    this(keystore, password, "PKIX", null);
  }

  LdapX509TrustManager(InputStream keystore, char[] password, String algorithm, String provider)
      throws GeneralSecurityException, IOException {
    // create a "default" JSSE X509TrustManager.

    KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(keystore, password);

    TrustManagerFactory tmf = null;
    if (provider == null) {
      tmf = TrustManagerFactory.getInstance(algorithm);
    } else {
      tmf = TrustManagerFactory.getInstance(algorithm, provider);
    }
    tmf.init(ks);

    TrustManager[] tms = tmf.getTrustManagers();
    /*
     * Iterate over the returned trustmanagers, look for an instance of
     * X509TrustManager. If found, use that as our "default" trust manager.
     */
    for (TrustManager tm : tms) {
      if (tm instanceof X509TrustManager) {
        sunJSSEX509TrustManager = (X509TrustManager) tm;
        break;
      }
    }

    /*
     * Find some other way to initialize, or else we have to fail the
     * constructor.
     */
    if (sunJSSEX509TrustManager == null) {
      throw new GeneralSecurityException(
          "Couldn't find default trust manager during initialization.");
    }
  }

  /*
   * Delegate to the default trust manager.
   */
  public void checkClientTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    try {
      sunJSSEX509TrustManager.checkClientTrusted(chain, authType);
    } catch (CertificateException excep) {
      // do any special handling here, or rethrow exception.
    }
  }

  /*
   * Delegate to the default trust manager.
   */
  public void checkServerTrusted(X509Certificate[] chain, String authType)
      throws CertificateException {
    try {
      sunJSSEX509TrustManager.checkServerTrusted(chain, authType);
    } catch (CertificateException excep) {
      /*
       * Possibly pop up a dialog box asking whether to trust the cert chain.
       */
    }
  }

  /*
   * Merely pass this through.
   */
  public X509Certificate[] getAcceptedIssuers() {
    return sunJSSEX509TrustManager.getAcceptedIssuers();
  }

}
