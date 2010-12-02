package org.sakaiproject.nakamura.guiceuser.impl;

import com.google.inject.Inject;

import org.sakaiproject.nakamura.guiceuser.api.InterfaceA;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceB;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceD;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceE;

public class ConcreteD implements InterfaceD {

  private InterfaceE e;
  private InterfaceB b;
  private InterfaceA a;

  @Inject
  public ConcreteD(InterfaceA a, InterfaceB b, InterfaceE e)
  {
    this.a = a;
    this.b = b;
    this.e = e;
  }

  public void printHello() {
    e.doPrint(a, b);
  }
}
