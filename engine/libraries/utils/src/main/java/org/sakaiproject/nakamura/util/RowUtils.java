package org.sakaiproject.nakamura.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;

public class RowUtils {

  private static final String JCR_PATH = "jcr:path";
  private static final String JCR_SCORE = "jcr:score";

  /**
   * Get a node for a certain row.
   * 
   * @param row
   *          The row you want to get the node out of.
   * @param session
   *          The session you got this row with.
   * @return The node for this row. If it can't be found it returns null.
   * @throws RepositoryException
   */
  public static Node getNode(Row row, Session session) throws RepositoryException {
    Value path = row.getValue(JCR_PATH);
    if (path == null) {
      return null;
    }
    Node node = (Node) session.getItem(path.getString());
    return node;
  }

  /**
   * Get the absolute path for this row.
   * 
   * @param row
   * @return The absolute path in the repository where this row has found a result.
   * @throws RepositoryException
   */
  public static String getPath(Row row) throws RepositoryException {
    Value val = row.getValue(JCR_PATH);
    return (val != null) ? val.getString() : null;
  }

  /**
   * Get the score for this row.
   * 
   * @param row
   *          The row you want to get the score for.
   * @return The jcr:score for this row.
   * @throws RepositoryException
   */
  public static long getScore(Row row) throws RepositoryException {
    Value val = row.getValue(JCR_SCORE);
    return (val != null) ? val.getLong() : 0;
  }

  /**
   * Get an excerpt for a certain name. This can be a property name or a relative child
   * node. (e.x.: jcr:content)
   * 
   * @param row
   *          The row you want to get the excerpt out.
   * @param excerptName
   *          The name of the property/relative child node
   * @return The excerpt in a string format if it exists, null if it doesn't.
   * @throws RepositoryException
   */
  public static String getExcerpt(Row row, String excerptName) throws RepositoryException {
    Value val = row.getValue("rep:excerpt(" + excerptName + ")");
    return (val != null) ? val.getString() : null;
  }

  /**
   * Get the excerpt out of the row.
   * 
   * @param row
   *          The row you want to get the excerpt out.
   * @return The excerpt or null
   * @throws RepositoryException
   */
  public static String getExcerpt(Row row) throws RepositoryException {
    return getExcerpt(row, ".");
  }

  /**
   * Gets an excerpt out of a row. It will first check for a match against jcr:content. If
   * none is found, it will check the .
   * 
   * @param row
   * @return
   * @throws RepositoryException
   */
  public static String getDefaultExcerpt(Row row) throws RepositoryException {
    String excerpt = getExcerpt(row, "jcr:content");
    if (excerpt == null) {
      return getExcerpt(row);
    }
    return excerpt;
  }
}
