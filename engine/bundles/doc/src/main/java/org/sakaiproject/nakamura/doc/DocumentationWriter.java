package org.sakaiproject.nakamura.doc;

import static org.sakaiproject.nakamura.api.doc.DocumentationConstants.CSS_CLASS_PATH;
import static org.sakaiproject.nakamura.api.doc.DocumentationConstants.CSS_CLASS_SHORT_DESCRIPTION;

import org.sakaiproject.nakamura.api.doc.DocumentationConstants;

import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

public class DocumentationWriter {

  /**
   * The title that should be outputted as a h1 on the listing.
   */
  private String title;
  private PrintWriter writer;

  /**
   * @param title The title that this writer should print.
   * @param printWriter The actual writer where content should be written to.
   */
  public DocumentationWriter(String title, PrintWriter printWriter) {
    this.title = title;
    this.setWriter(printWriter);
  }

  /**
   * Write out a list of nodes.
   * 
   * @param session
   *          The current JCR session.
   * @param writer
   *          The writer where the response should go to.
   * @param query
   *          The query to use to retrieve the nodes.
   * @throws InvalidQueryException
   * @throws RepositoryException
   */
  public void writeNodes(Session session, String query, String servlet)
      throws InvalidQueryException, RepositoryException {
    // Write the HTML header.
    writer.append(DocumentationConstants.HTML_HEADER);

    // Begin list
    writer.append("<h1>").append(getTitle()).append("</h1>");
    writer.append("<ul class=\"").append(
        DocumentationConstants.CSS_CLASS_DOCUMENTATION_LIST).append("\">");

    QueryManager qm = session.getWorkspace().getQueryManager();
    Query q = qm.createQuery(query, Query.XPATH);
    QueryResult result = q.execute();
    NodeIterator iterator = result.getNodes();
    while (iterator.hasNext()) {
      Node node = iterator.nextNode();
      NodeDocumentation doc = new NodeDocumentation(node);

      writer.append("<li><a href=\"");
      writer.append(servlet);
      writer.append("?p=");
      writer.append(doc.getPath());
      writer.append("\">");
      if (doc.getTitle() != null && !doc.getTitle().equals("")) {
        writer.append(doc.getTitle());
      } else {
        writer.append(doc.getPath());
      }
      writer.append("</a><span class=\"").append(CSS_CLASS_PATH).append("\">");
      writer.append(doc.getPath());
      writer.append("</span><p class=\"").append(CSS_CLASS_SHORT_DESCRIPTION).append(
          "\">");
      writer.append(doc.getShortDescription());
      writer.append("</p></li>");

    }

    // End list
    writer.append("</ul>");

    // Footer
    writer.append(DocumentationConstants.HTML_FOOTER);
  }

  /**
   * Write info for a specific node.
   *
   * @param path
   *          The path to the node.
   * @param session
   *          The current JCR session.
   * @param writer
   *          The writer to send the response to.
   * @throws RepositoryException
   */
  public void writeSearchInfo(String path, Session session) throws RepositoryException {
    Node node = (Node) session.getItem(path);
    NodeDocumentation doc = new NodeDocumentation(node);
    writer.append(DocumentationConstants.HTML_HEADER);
    writer.append("<h1>");
    writer.append(doc.getTitle());
    writer.append("</h1>");
    doc.send(writer);
    writer.append(DocumentationConstants.HTML_FOOTER);
  }

  /**
   * @param title
   *          The title that should be outputted as a h1 on the listing.
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the The title that should be outputted as a h1 on the listing.
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param writer
   *          the writer to write the info to.
   */
  public void setWriter(PrintWriter writer) {
    this.writer = writer;
  }

  /**
   * @return the writer
   */
  public PrintWriter getWriter() {
    return writer;
  }
}
