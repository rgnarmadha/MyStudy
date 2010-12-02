package org.sakaiproject.nakamura.guiceuser;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

import org.sakaiproject.nakamura.guiceuser.api.InterfaceA;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceB;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceD;
import org.sakaiproject.nakamura.guiceuser.api.InterfaceE;
import org.sakaiproject.nakamura.guiceuser.impl.ConcreteA;
import org.sakaiproject.nakamura.guiceuser.impl.ConcreteB;
import org.sakaiproject.nakamura.guiceuser.impl.ConcreteD;
import org.sakaiproject.nakamura.guiceuser.impl.ConcreteE;
import org.sakaiproject.nakamura.guiceuser.impl.FProvider;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(InterfaceA.class).to(ConcreteA.class).in(Scopes.SINGLETON);
    bind(InterfaceB.class).to(ConcreteB.class).in(Scopes.SINGLETON);
    bind(InterfaceD.class).to(ConcreteD.class).in(Scopes.SINGLETON);
    bind(InterfaceE.class).to(ConcreteE.class).in(Scopes.SINGLETON);
    bind(FProvider.class);
  }

}
