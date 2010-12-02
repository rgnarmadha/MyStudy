/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.ldap;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.classextension.EasyMock.createMock;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Before;
import org.junit.Test;

/**
 * This class exists to demonstrate lifecycle method firing from
 * {@link GenericObjectPool}. It was specififcally motivated by problems with
 * stale {@link PooledLDAPConnection} objects being returned to the pool.
 *
 * <p>
 * The fixture is intended to mimic the pool created by
 * {@link PoolingLdapConnectionManager#init()} and new tests should probably be
 * created if that behavior/configuration changes.
 * </p>
 *
 * <p>
 * Again, keep in mind that this class exists purely for demonstation purposes,
 * i.e. to learn about the pooling library, not to verify LDAP provider
 * behaviors. Also keep in mind that pool behaviors, including lifecycle method
 * invocation, are highly dependent on configuration, so the behaviors verified
 * in this class may not hold for a different fixture configuration
 * </p>
 *
 * @author Dan McCallum (dmccallum@unicon.net)
 *
 */
public class GenericObjectPoolTest {

  private GenericObjectPool pool;
  private PoolableObjectFactory factory;

  @Before
  public void setUp() throws Exception {

    factory = createMock(PoolableObjectFactory.class);

    pool = new GenericObjectPool(factory, 1, // maxActive
        GenericObjectPool.WHEN_EXHAUSTED_BLOCK, // whenExhaustedAction
        60000, // maxWait (millis)
        1, // maxIdle
        true, // testOnBorrow
        false // testOnReturn
    );
  }

  /**
   * Verifies that {@link GenericObjectPool#borrowObject()} fires
   * {@link PoolableObjectFactory#makeObject()},
   * {@link PoolableObjectFactory#activateObject(Object)}, and
   * {@link PoolableObjectFactory#validateObject(Object)}, in that order.
   *
   * @throws Exception
   *           test error
   */
  @Test
  public void testBorrowObjectFiresMakeActivateAndValidate() throws Exception {

    Object pooledObject = new Object();

    // expectations are implemented as a stack, so are searched in the reverse
    // order from which they were created
    expect(factory.validateObject(pooledObject)).andReturn(true);
    factory.activateObject(pooledObject);
    expectLastCall();
    expect(factory.makeObject()).andReturn(pooledObject);

    replay(factory);
    Object borrowedObject = pool.borrowObject();
    assertEquals("Unexpected object returned from pool", pooledObject, borrowedObject);

  }

  /**
   * Verifies that {@link GenericObjectPool} makes no subsequent calls to
   * {@link PoolableObjectFactory#makeObject()} following a failed
   * {@link PoolableObjectFactory#validateObject(Object)} <em>on a newly
   * created poolable object</em>. This was unexpected behavior -- initially
   * this was a test case that verified the pool falling back to alloc a second
   * new object when validation on a new object failed. It makes sense --
   * without a retry limit, the pool could fall into an endless alloc-validate
   * loop.
   *
   * <p>
   * Note that this test also verifies that the pool fires
   * {@link PoolableObjectFactory#destroyObject(Object)} when validation fails
   * </p>
   *
   * @throws Exception
   *           test error
   */
  @Test
  public void testWillNotRetryMakeObjectOnFailedValidate() throws Exception {

    Object pooledObject1 = new Object();

    expect(factory.makeObject()).andReturn(pooledObject1);
    factory.activateObject(pooledObject1);
    expectLastCall();
    expect(factory.validateObject(pooledObject1)).andReturn(false);
    factory.destroyObject(pooledObject1);
    expectLastCall();
    replay(factory);

    try {
      pool.borrowObject();
      fail("Should failed to borrow object");
    } catch (Exception e) {
      // success
    }
  }

  /**
   * Similar to {@link #testWillNotRetryMakeObjectOnFailedValidate()} but
   * verifies that the pool attempts new object creation when validation fails
   * on an object <em>already present in the pool</em>.
   *
   * @throws Exception
   *           test error
   */
  @Test
  public void testAttemptsToPoolNewObjectOnFailedValidationOfPooledObject() throws Exception {

    Object pooledObject1 = new Object();
    Object pooledObject2 = new Object();

    expect(factory.makeObject()).andReturn(pooledObject1);
    factory.activateObject(pooledObject1);
    expectLastCall();
    // pass the validation, pooledObject1 should be in the pool
    expect(factory.validateObject(pooledObject1)).andReturn(true);
    // client adds the object back to the pool
    factory.passivateObject(pooledObject1);
    expectLastCall();
    factory.activateObject(pooledObject1);
    expectLastCall();
    // a second borrow should reactivate and reverify the original object --
    // we'll fail the validation to force alloc of a new poolable object
    expect(factory.validateObject(pooledObject1)).andReturn(false);
    // make sure the original object is destroyed
    factory.destroyObject(pooledObject1);
    expectLastCall();
    // alloc a second object
    expect(factory.makeObject()).andReturn(pooledObject2);
    factory.activateObject(pooledObject2);
    expectLastCall();
    expect(factory.validateObject(pooledObject2)).andReturn(true);
    replay(factory);

    Object borrowedObject1 = pool.borrowObject();
    // something of a sanity check
    assertEquals("Unexpected object returned from pool", pooledObject1, borrowedObject1);
    pool.returnObject(borrowedObject1);

    Object borrowedObject2 = pool.borrowObject();
    // now make sure we got a branch new object
    assertEquals("Unexpected object returned from pool", pooledObject2, borrowedObject2);

  }

  /**
   * Verifies a suspicion that if a null reference is returned to the pool, the
   * pool will return that reference to subsequent
   * {@link GenericObjectPool#borrowObject()} calls, so long as the object
   * passes activation and validation lifecycle phases.
   *
   * @throws Exception
   */
  @Test
  public void testPoolAllowsNullObjectReferencesToBeReturnedAndSubsequentlyBorrowed()
      throws Exception {

    factory.passivateObject(null);
    expectLastCall();
    factory.activateObject(null);
    expectLastCall();

    // this is really the important expectation -- the underlying factory must
    // be
    // implemented such that it fails to identify "null" objects as invalid
    expect(factory.validateObject(null)).andReturn(true);
    replay(factory);

    pool.returnObject(null); // the code exercise
    assertNull(pool.borrowObject());
  }

}
