package org.sakaiproject.nakamura.smtp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamCopier extends InputStream {

  private InputStream wrapped;
  private ByteArrayOutputStream buffer;

  public StreamCopier(InputStream wrapped) {
    this.wrapped = wrapped;
    this.buffer = new ByteArrayOutputStream();
  }

  @Override
  public int read() throws IOException {
    int i = wrapped.read();
    if (i != -1) {
      buffer.write((char)i);
    }
    return i;
  }
  
  public String getContents() {
    return buffer.toString();
  }

}
