package org.sakaiproject.nakamura.guiceuser.impl;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.sakaiproject.nakamura.guiceuser.api.InterfaceE;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FProvider implements Provider<InterfaceF> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(FProvider.class);
  private InterfaceE e;

  @Inject
  public FProvider(InterfaceE e)
  {
    this.e = e;
  }
  
  public InterfaceF get() {
    return new InterfaceF() 
    {

      public void printViaE() {
        LOGGER.info("Provided F printing via e via d");
        FProvider.this.getE().printHelloViaD();
      }
    };
  }

  protected InterfaceE getE() {
    return e;
  }

}
