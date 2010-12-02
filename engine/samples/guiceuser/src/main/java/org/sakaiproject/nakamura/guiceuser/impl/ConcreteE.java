package org.sakaiproject.nakamura.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.nakamura.guiceuser.api.InterfaceA;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceB;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceD;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcreteE implements InterfaceE {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ConcreteE.class);
  private InterfaceD d;
  
  @Inject
  public ConcreteE(InterfaceD d)
  {
    this.d = d;
  }
  
  public void printHelloViaD()
  {
    this.d.printHello();
  }
  
  public void doPrint(InterfaceA a, InterfaceB b)
  {
    LOGGER.info("Printing from e");
    b.printString(a.getHelloWorld());
  }


}
