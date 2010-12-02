package org.sakaiproject.nakamura.meservice;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractMyGroupsServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = -8743012430930506449L;
  private static final Logger LOGGER = LoggerFactory.getLogger(MyManagedGroupsServlet.class);

  public static final String PARAM_TEXT_TO_MATCH = "q";

  /**
  * This and related code attempts to mimic the usual client-server API for paged searching.
  *
  * TODO Replace the MyGroupsServlet and MyManagedGroupsServlet with search queries.
  * (This was not possible when the servlets were first written, but should be now.)
  */
  public static final String PARAMS_ITEMS_PER_PAGE = "items";
  /**
  *
  */
  public static final String PARAMS_PAGE = "page";
  /**
   * The default amount of items in a page.
   */
  public static final int DEFAULT_PAGED_ITEMS = 25;

  /**
  *
  */
  public static final String TOTAL = "total";
 /**
  *
  */
  public static final String JSON_RESULTS = "results";




  protected transient ProfileService profileService;

  protected void bindProfileService(ProfileService profileService) {
    this.profileService = profileService;
  }
  protected void unbindProfileService(ProfileService profileService) {
    this.profileService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    String userId = session.getUserID();
    try {
      // Find the Group entities associated with the user.
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable authorizable = userManager.getAuthorizable(userId);
      TreeMap<String, Group> groups = getGroups(authorizable, userManager);

      // Get the specified search query filter, if any.
      Pattern filterPattern = getFilterPattern(request.getParameter(PARAM_TEXT_TO_MATCH));

      // Filter the Profiles so as to set up proper paging.
      List<ValueMap> filteredProfiles = new ArrayList<ValueMap>();
      for (Group group : groups.values()) {
        ValueMap profile = profileService.getProfileMap(group, session);
        if (profile != null) {
          if ((filterPattern == null) || (isValueMapPattternMatch(profile, filterPattern))) {
            filteredProfiles.add(profile);
          }
        } else {
          LOGGER.info("No Profile found for group {}", group.getID());
        }
      }

      // Write out the Profiles.
      long nitems = longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
          DEFAULT_PAGED_ITEMS);
      long page = longRequestParameter(request, PARAMS_PAGE, 0);
      long offset = page * nitems;

      List<String> selectors = Arrays.asList(request.getRequestPathInfo().getSelectors());
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      ExtendedJSONWriter writer = new ExtendedJSONWriter(response.getWriter());
      writer.setTidy(selectors.contains("tidy"));

      writer.object();
      writer.key(PARAMS_ITEMS_PER_PAGE);
      writer.value(nitems);
      writer.key(JSON_RESULTS);

      writer.array();
      int i = 0;
      for (ValueMap profile : filteredProfiles) {
        if ( i >= (offset + nitems) ) {
          break;
        } else if ( i >= offset ) {
          writer.valueMap(profile);
        }
        i++;
      }
      writer.endArray();

      writer.key(TOTAL);
      writer.value(filteredProfiles.size());

      writer.endObject();


    } catch (RepositoryException e) {
      LOGGER.error("Failed to retrieve groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to retrieve groups.");
    } catch (JSONException e) {
      LOGGER.error("Failed to write out groups for user " + userId, e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to build a proper JSON output.");
    }
  }

  private long longRequestParameter(SlingHttpServletRequest request,
      String paramName, long defaultValue) {
    String p = request.getParameter(paramName);
    if ( p == null || p.trim().length() == 0 ) {
      return defaultValue;
    }
    try {
      return Long.valueOf(p);
    } catch ( Exception e) {
      return defaultValue;
    }
  }
  protected abstract TreeMap<String, Group> getGroups(Authorizable member, UserManager userManager)
      throws RepositoryException;

  protected static Pattern getFilterPattern(String filterParameter) {
    Pattern filterPattern;
    if (filterParameter == null) {
      filterPattern = null;
    } else {
      // Translate Jackrabbit-style wildcards to Java-style wildcards.
      String translatedParameter = "(?i).*\\b" + filterParameter.replaceAll("\\*", ".*") + "\\b.*";
      filterPattern = Pattern.compile(translatedParameter);
    }
    return filterPattern;
  }

  private boolean isObjectPatternMatch(Object object, Pattern queryFilter) {
    if (object instanceof ValueMap) {
      return isValueMapPattternMatch((ValueMap) object, queryFilter);
    } else if (object instanceof Object[]) {
      return isArrayPatternMatch((Object[]) object, queryFilter);
    } else {
      return isStringPatternMatch(object.toString(), queryFilter);
    }
  }

  private boolean isArrayPatternMatch(Object[] array, Pattern queryFilter) {
    for (Object object : array) {
      if (isObjectPatternMatch(object, queryFilter)) {
        return true;
      }
    }
    return false;
  }

  private boolean isValueMapPattternMatch(ValueMap valueMap, Pattern queryFilter) {
    for (Entry<String, Object> entry : valueMap.entrySet()) {
      Object rawValue = entry.getValue();
      if (isObjectPatternMatch(rawValue, queryFilter)) {
        return true;
      }
    }
    return false;
  }

  private boolean isStringPatternMatch(String stringValue, Pattern queryFilter) {
    return queryFilter.matcher(stringValue).matches();
  }

}