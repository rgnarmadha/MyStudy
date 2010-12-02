package org.sakaiproject.nakamura.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.nakamura.guiceuser.api.InterfaceA;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceB;

public class InjectableC {

  @Inject
  public InjectableC(InterfaceA a, InterfaceB b)
  {
    b.printString(a.getHelloWorld());
  }
}
