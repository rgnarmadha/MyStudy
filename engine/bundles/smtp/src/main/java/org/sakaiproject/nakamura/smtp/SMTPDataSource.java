package org.sakaiproject.nakamura.smtp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.activation.DataSource;

public class SMTPDataSource implements DataSource {

  private String contentType;
  private InputStream inputStream;

  public SMTPDataSource(Map<String,Object> properties, InputStream inputStream) {
    this((String) properties.get("content-type"), inputStream);
  }
  
  public SMTPDataSource(String contentType, InputStream inputStream) {
    this.contentType = contentType;
    this.inputStream = inputStream;
  }

  public String getContentType() {
    return contentType;
  }

  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  public String getName() {
    return "SMTP stream";
  }

  public OutputStream getOutputStream() throws IOException {
    return null;
  }

}
