package org.sakaiproject.nakamura.util;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.sakaiproject.nakamura.util.parameters.ParameterMap;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

public class RequestWrapper extends SlingHttpServletRequestWrapper {

  private RequestInfo requestInfo;
  private ParameterMap postParameterMap;

  public RequestWrapper(SlingHttpServletRequest request, RequestInfo requestInfo) {
    super(request);
    this.requestInfo = requestInfo;

  }

  private Hashtable<String, String[]> getParameters() {
    return requestInfo.getParameters();
  }

  //
  // Sling Request parameters
  //

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameter(java.lang.String)
   */
  @Override
  public RequestParameter getRequestParameter(String name) {
    return getRequestParameterMapInternal().getValue(name);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameterMap()
   */
  @Override
  public RequestParameterMap getRequestParameterMap() {
    return getRequestParameterMapInternal();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper#getRequestParameters(java.lang.String)
   */
  @Override
  public RequestParameter[] getRequestParameters(String name) {
    return getRequestParameterMapInternal().getValues(name);
  }

  private ParameterMap getRequestParameterMapInternal() {
    if (this.postParameterMap == null) {

      String encoding = getCharacterEncoding();
      // SLING-508 Try to force servlet container to decode parameters
      // as ISO-8859-1 such that we can recode later
      if (encoding == null) {
        encoding = "ISO-8859-1";
        try {
          this.setCharacterEncoding(encoding);
        } catch (UnsupportedEncodingException uee) {
          throw new RuntimeException(uee);
        }
      }
      // SLING-152 Get parameters from the servlet Container
      ParameterMap parameters = ParameterUtils.getContainerParameters(encoding,
          getParameterMap());

      this.postParameterMap = parameters;
    }
    return this.postParameterMap;
  }

  //
  // Default servlet getParameters
  //

  @Override
  public String getParameter(String name) {
    String[] param = getParameters().get(name);
    if (param != null && param.length > 0) {
      return param[0];
    }
    return null;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Map getParameterMap() {
    return getParameters();
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Enumeration getParameterNames() {
    return getParameters().keys();
  }

  @Override
  public String[] getParameterValues(String name) {
    return getParameters().get(name);
  }

  @Override
  public String getMethod() {
    return (requestInfo.getMethod() == null) ? "GET" : requestInfo.getMethod();
  }

  @Override
  public String getPathInfo() {
    return requestInfo.getUrl();
  }

  @Override
  public String getPathTranslated() {
    String url = requestInfo.getUrl();
    int i = url.indexOf("?");
    if (i != -1) {
      return url.substring(0, i);
    } else {
      return url;
    }
  }

  @Override
  public String getQueryString() {
    String url = requestInfo.getUrl();
    int i = url.indexOf("?");
    if (i != -1) {
      return url.substring(i, url.length());
    } else {
      return null;
    }
  }

  @Override
  public String getServletPath() {
    return requestInfo.getUrl();
  }

  @Override
  public String getRequestURI() {
    return requestInfo.getUrl();
  }

}
