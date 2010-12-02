package org.sakaiproject.nakamura.util;

import java.io.InputStream;

import javax.jcr.Node;

public class NodeInputStream {

  protected Node node;
  protected InputStream inputStream;
  protected long length;

  public NodeInputStream(Node node, InputStream stream, long length) {
    this.node = node;
    this.inputStream = stream;
    this.length = length;
  }

  public Node getNode() {
    return node;
  }

  public InputStream getInputStream() {
    return inputStream;
  }

  public long getLength() {
    return length;
  }

}
