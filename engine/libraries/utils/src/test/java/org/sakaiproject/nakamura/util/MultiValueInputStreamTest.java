/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class MultiValueInputStreamTest {

  @Test
  public void testMultiValueInputStreamMulti() throws ValueFormatException,
      RepositoryException, IOException {
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    InputStream[] stream = new InputStream[3];
    Value[] values = new Value[3];

    byte[][] buffer = new byte[3][100];
    Binary[] bin = new Binary[3];
    for (int i = 0; i < 3; i++) {
      values[i] = createMock(Value.class);
      for (int j = 0; j < 100; j++) {
        buffer[i][j] = (byte) (i + j);
      }
      stream[i] = new ByteArrayInputStream(buffer[i]);
      bin[i] = createMock(Binary.class);
    }

    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(true);
    expect(property.getValues()).andReturn(values);
    for ( int i = 0; i < 3; i++ ) {
      expect(values[i].getBinary()).andReturn(bin[i]);
      expect(bin[i].getStream()).andReturn(stream[i]);

    }


    replay(property, propertyDefinition, values[0], values[1], values[2], bin[0], bin[1], bin[2]);
    MultiValueInputStream multiValueInputStream = new MultiValueInputStream(property);

    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 100; j++) {
        assertEquals("Missmatch at " + i + ":" + j, buffer[i][j], multiValueInputStream
            .read());
      }
    }

    multiValueInputStream.close();
    verify(property, propertyDefinition, values[0], values[1], values[2], bin[0], bin[1], bin[2]);
  }

  @Test
  public void testMultiValueInputStreamSingle() throws ValueFormatException,
      RepositoryException, IOException {
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);

    byte[] buffer = new byte[100];

    Value value = createMock(Value.class);
    for (int j = 0; j < 100; j++) {
      buffer[j] = (byte) (j);
    }
    InputStream stream = new ByteArrayInputStream(buffer);

    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false);
    expect(property.getValue()).andReturn(value);
    Binary bin = createMock(Binary.class);

    expect(value.getBinary()).andReturn(bin);
    expect(bin.getStream()).andReturn(stream);

    replay(property, propertyDefinition, value, bin);
    MultiValueInputStream multiValueInputStream = new MultiValueInputStream(property);

    for (int j = 0; j < 100; j++) {
      assertEquals("Missmatch at " + j, buffer[j], multiValueInputStream.read());
    }
    
    multiValueInputStream.close();

    verify(property, propertyDefinition, value, bin);
  }

}
