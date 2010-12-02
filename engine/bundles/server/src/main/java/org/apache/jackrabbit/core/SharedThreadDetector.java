package org.apache.jackrabbit.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Session;

public class SharedThreadDetector {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SharedThreadDetector.class);
  private Exception bindingTrace;
  private Thread initThread;
  private int i;
  private Session session;

  public SharedThreadDetector(Session sesison) {
    initThread = Thread.currentThread();
    bindingTrace = new Exception("Bound At");
    this.session = sesison;
  }

  public void check(Session session) {
    Thread cthread = Thread.currentThread();
    if (initThread != cthread) {
      Exception e = new Exception("Rebound " + i + " at");
      LOGGER
          .warn("Session is being shared between threads, log a debug level for more information ");
      if (LOGGER.isDebugEnabled()) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.append("Violation Bound to ").append(String.valueOf(initThread));
        pw.append(" called by ").append(String.valueOf(cthread)).append("\n");
        pw.append(" Session ").append(String.valueOf(this.session)).append("\n");
        if (session != this.session) {
          pw.append(" Calling Session Changed to ").append(String.valueOf(session))
              .append("\n");
        }
        bindingTrace.printStackTrace(pw);
        e.printStackTrace(pw);
        pw.flush();
        LOGGER.debug(sw.toString());
      }
      i++;
      initThread = Thread.currentThread();
      bindingTrace = e;
      this.session = session;
    }
  }

}
