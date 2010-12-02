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

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Creates an Input Stream from a multi value property.
 */
public class MultiValueInputStream extends InputStream {

  private Value[] values;
  private int nextStream;
  private int nvalues;
  private InputStream currentInputStream;

  /**
   * @param template
   * @throws RepositoryException 
   * @throws ValueFormatException 
   */
  public MultiValueInputStream(Property property) throws ValueFormatException, RepositoryException {
    PropertyDefinition pd = property.getDefinition();
    if ( pd.isMultiple() ) {
      values = property.getValues();      
    } else {
      values = new Value[1];
      values[0] = property.getValue();
    }
    nextStream = 0;
    nvalues = values.length;
    currentInputStream = values[nextStream].getBinary().getStream();
    nextStream++;
  }

  /**
   * {@inheritDoc}
   * @see java.io.InputStream#read()
   */
  @Override
  public int read() throws IOException {
    int c = currentInputStream.read();
    while ( c < 0 && nextStream < nvalues ) {
      currentInputStream.close();
      try {
        currentInputStream = values[nextStream].getBinary().getStream();
      } catch (IllegalStateException e) {
        throw new IOException("Failed to open property value no "+nextStream+" as stream:"+e.getMessage());
      } catch (RepositoryException e) {
        throw new IOException("Failed to open property value no "+nextStream+" as stream:"+e.getMessage());
      }
      nextStream++;
      c = currentInputStream.read();
    }
    return c;
  }
  
  /**
   * {@inheritDoc}
   * @see java.io.InputStream#close()
   */
  @Override
  public void close() throws IOException {
    currentInputStream.close();
  }
  

}
