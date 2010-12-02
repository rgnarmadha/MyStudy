package org.sakaiproject.nakamura.api.doc;


public interface DocumentationConstants {

  public static final CharSequence HTML_HEADER = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 TRANSITIONAL//EN\">"
  + "<html><head>"
  + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
  + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/sling.css\" >"
  + "<link rel=\"stylesheet\" type=\"text/css\" href=\""
  + DocumentationConstants.PREFIX
  + "?p=style\">"
  + "</head><body>";
  public static final CharSequence HTML_FOOTER = "</body></html>";
  public static final String PREFIX = "/system/doc";

  public static final CharSequence CSS_CLASS_DOCUMENTATION_LIST = "documentation-list";
  public static final CharSequence CSS_CLASS_NODOC = "nodoc";
  public static final CharSequence CSS_CLASS_PARAMETERS = "parameters";
  public static final CharSequence CSS_CLASS_PARAMETER_NAME = "parameter-name";
  public static final CharSequence CSS_CLASS_PARAMETER_DESCRIPTION = "parameter-description";
  public static final CharSequence CSS_CLASS_PATH = "path";
  public static final CharSequence CSS_CLASS_SHORT_DESCRIPTION = "short-description";

}
