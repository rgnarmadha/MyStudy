package org.sakaiproject.nakamura.persondirectory.providers;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Person provider implementation that gets its information from an LDAP store.
 */
@Component(metatype = true, configurationFactory = true, policy = ConfigurationPolicy.REQUIRE)
@Service
public class LdapPersonProvider implements PersonProvider {
  private static final Logger LOG = LoggerFactory.getLogger(LdapPersonProvider.class);

  /** Constant for the sling resource type property name */
  static final String SLING_RESOURCE_TYPE = "sling:resourceType";

  /** Constant for the sakai user profile resource type value */
  static final String SAKAI_USER_PROFILE = "sakai/user-profile";

  /** Constant for the user id property */
  static final String REP_USER_ID = "rep:userId";

  public static final String SEPARATOR = "=>";

  @Property(value = "o=sakai")
  protected static final String BASE_DN = "sakai.pd.ldap.baseDn.pattern";
  private String baseDn;

  @Property(value = "uid={}")
  protected static final String PROP_FILTER_PATTERN = "sakai.pd.ldap.filter.pattern";
  private String filterPattern;

  @Property(cardinality = 2147483647)
  protected static final String PROP_ATTRIBUTES_MAP = "sakai.pd.ldap.attributes.map";
  private HashMap<String, String> attrsMap = new HashMap<String, String>();

  @Reference
  private LdapConnectionManager connMgr;

  /**
   * Default constructor.
   */
  public LdapPersonProvider() {
  }

  /**
   * Constructor for injecting dependencies. Targeted for tests.
   *
   * @param ldapBroker
   */
  LdapPersonProvider(LdapConnectionManager connMgr) {
    this.connMgr = connMgr;
  }

  @Activate
  protected void activate(Map<?, ?> props) {
    baseDn = OsgiUtil.toString(props.get(BASE_DN), "");
    filterPattern = OsgiUtil.toString(props.get(PROP_FILTER_PATTERN), "");

    String[] attributeMapping = (String[]) props.get(PROP_ATTRIBUTES_MAP);
    if (attributeMapping != null
        && !(attributeMapping.length == 1 && "".equals(attributeMapping[0]))) {
      for (String mapping : attributeMapping) {
        int splitIndex = mapping.indexOf(SEPARATOR);
        if (splitIndex < 1) {
          attrsMap.put(mapping.trim(), mapping.trim());
        } else {
          String key0 = mapping.substring(0, splitIndex).trim();
          String key1 = mapping.substring(splitIndex + 2).trim();
          if (key0.length() == 0 || key1.length() == 0) {
            // make sure we have 2 usable keys
            throw new ComponentException("Improperly formatted key mapping [" + mapping
                + "]. Should be fromKey " + SEPARATOR + " toKey.");
          }
          attrsMap.put(key0, key1);
        }
      }
    } else {
      attrsMap = new HashMap<String, String>();
    }

    // create an attribute array for looking things up
    Set<String> attrKeys = attrsMap.keySet();
    String[] attrs = new String[attrKeys.size()];
    attrKeys.toArray(attrs);
  }

  protected Map<String, String> getAttributesMap() {
    return attrsMap;
  }

  public Map<String, Object> getProfileSection(Node parameters)
      throws PersonProviderException {
    try {
      HashMap<String, Object> person = new HashMap<String, Object>();

      // get the user ID
      String uid = findUserId(parameters);

      // set the properties
      String filter = filterPattern.replace("{}", uid);

      String[] attributes = attrsMap.keySet().toArray(new String[]{});

      LOG.debug("searchDirectory(): [baseDN = {}][filter = {}][return attribs = {}]",
          new Object[] { baseDn, filter, attributes });

      // get a connection bound to the application user
      LDAPConnection conn = connMgr.getBoundConnection(null, null);
      LDAPSearchResults searchResults = conn.search(baseDn, LDAPConnection.SCOPE_SUB,
          filter, attributes, false);
      if (searchResults.hasMore()) {
        // pick off the first result returned
        LDAPEntry entry = searchResults.next();

        // get the attributes from the entry and loop through them
        LDAPAttributeSet attrs = entry.getAttributeSet();
        Iterator<?> attrIter = attrs.iterator();
        while (attrIter.hasNext()) {
          // get the key and values from the attribute
          LDAPAttribute attr = (LDAPAttribute) attrIter.next();
          String name = attr.getName();
          String[] vals = attr.getStringValueArray();

          // check for an aliased name
          String mappingName = attrsMap.get(name);

          // add the values under the appropriate key
          if (vals.length == 1) {
            person.put(mappingName, vals[0]);
          } else if (vals.length > 1) {
            person.put(mappingName, vals);
          }
        }
      }
      return person;
    } catch (LDAPException e) {
      throw new PersonProviderException(e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new PersonProviderException(e.getMessage(), e);
    }
  }

  private String findUserId(Node node) throws RepositoryException, PersonProviderException {
    if (node.hasProperty(SLING_RESOURCE_TYPE)
        && SAKAI_USER_PROFILE.equals(node.getProperty(SLING_RESOURCE_TYPE).getString())
        && node.hasProperty(REP_USER_ID)) {
      return node.getProperty(REP_USER_ID).getString();
    } else {
      if (!"/".equals(node.getPath())) {
        return findUserId(node.getParent());
      }
      throw new PersonProviderException("Could not retrieve userid.");
    }
  }
}
