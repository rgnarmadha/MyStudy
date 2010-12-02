package org.sakaiproject.nakamura.doc;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.DocumentationConstants;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.doc.servlet.ServletDocumentation;
import org.sakaiproject.nakamura.doc.servlet.ServletDocumentationRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

@ServiceDocumentation(
  name = "General Documentation Servlet",
  description = "Gets the documentation for servlets, proxies and search templates.",
  bindings = {
    @ServiceBinding(type = BindingType.PATH, bindings = { "system/doc" })
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = "Get the documentation.",
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      }
    )
  }
)
@SlingServlet(methods = { "GET" }, paths = { "/system/doc" })
public class GeneralDocumentationServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 6866189047081436865L;
  @Reference
  protected transient ServletDocumentationRegistry servletDocumentationRegistry;
  private byte[] style;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    RequestParameter p = request.getRequestParameter("p");
    if (p == null) {
      // Send out the categories.
      sendIndex(response);
    } else if ("style".equals(p.getString())) {
      // Send out the CSS file
      if (style == null) {
        InputStream in = this.getClass().getResourceAsStream("style.css");
        style = IOUtils.toByteArray(in);
        in.close();
      }
      response.setContentType("text/css; charset=UTF-8");
      response.setContentLength(style.length);
      response.getOutputStream().write(style);
    }

  }

  private void sendIndex(SlingHttpServletResponse response) throws IOException {
    PrintWriter writer = response.getWriter();
    writer.append(DocumentationConstants.HTML_HEADER);
    writer.append("<h1>List of Services</h1>");
    writer.append("<ul>");
    Map<String, ServletDocumentation> m = servletDocumentationRegistry.getServletDocumentation();
    List<ServletDocumentation> o = new ArrayList<ServletDocumentation>(m.values());
    Collections.sort(o);
    for (ServletDocumentation k : o) {
      if (k.isDocumentationServlet()) {
        String key = k.getKey();
        if (key != null) {
          writer.append("<li><a href=\"");
          writer.append(k.getUrl());
          writer.append("\">");
          writer.append(k.getServiceDocumentationName());
          writer.append("</a><p>");
          writer.append(k.getShortDescription());
          writer.append("</p></li>");
        }
      }
    }
    writer.append("</ul>");
    writer.append(DocumentationConstants.HTML_FOOTER);
  }


}
