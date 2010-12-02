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
package org.sakaiproject.nakamura.workflow.persistence;

import org.drools.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * A storage object that manages ID based objects (ID being an number) in JCR, auto
 * scaling the storage structure, while eliminating expensive scans.
 */
public abstract class AbstractIdBasedObject {

  protected Session session;
  protected long id;
  protected Node objectNode;

  /**
   * @throws RepositoryException
   * @throws IOException
   * 
   */
  public AbstractIdBasedObject(Session session, long id) throws RepositoryException,
      IOException {
    this.session = session;
    this.id = id;
    createOrGetObjectNode();
    load();
  }

  public long getId() {
    return this.id;
  }

  /**
   * @throws IOException
   * @throws RepositoryException
   * 
   */
  public abstract void load() throws RepositoryException, IOException;

  /**
   * @throws IOException
   * @throws RepositoryException
   * 
   */
  public abstract void save() throws RepositoryException, IOException;

  public void remove() throws RepositoryException {
    objectNode.remove();
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  /**
   * @return
   * @throws RepositoryException
   */
  protected abstract Node createContentNode(Node parentNode, String nodeName)
      throws RepositoryException;

  /**
   * @return
   */
  protected abstract String getStoragePrefix();

  /**
   * @throws RepositoryException
   * 
   */
  private void createOrGetObjectNode() throws RepositoryException {
    if (id == -1) {
      if (!session.itemExists(getStoragePrefix())) {
        id = 0;
      } else {
        Node lastNode = session.getNode(getStoragePrefix());
        Node newLastNode = getLastNode(lastNode);
        if (lastNode == newLastNode) {
          id = 0;
        } else {
          lastNode = newLastNode;
          newLastNode = getLastNode(lastNode);
          if (lastNode == newLastNode) {
            id = Integer.parseInt(lastNode.getName()) * 1000000 + 1;
          } else {
            lastNode = newLastNode;
            newLastNode = getLastNode(lastNode);
            if (lastNode == newLastNode) {
              id = Integer.parseInt(lastNode.getName()) * 1000 + 1;
            } else {
              id = Integer.parseInt(newLastNode.getName()) + 1;
            }
          }
        }
      }
    }
    // create a node of type nt:file in the right place.
    long n = id / 1000000;
    long m = id / 1000;
    String[] path = StringUtils.split(getStoragePrefix() + "/" + n + "/" + m, '/');
    Node node = session.getRootNode();
    for (String element : path) {
      if (!node.hasNode(element)) {
        node = node.addNode(element, "nt:unstructured");
        node.addMixin("mix:created");
      } else {
        node = node.getNode(element);
      }
    }
    String nodeName = String.valueOf(id);
    if (!node.hasNode(nodeName)) {
      objectNode = createContentNode(node, nodeName);
    } else {
      objectNode = node.getNode(nodeName);
    }

    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  /**
   * Gets the last node in a list of child nodes or returns null
   * 
   * @param lastNode
   * @return
   * @throws RepositoryException
   */
  private Node getLastNode(Node lastNode) throws RepositoryException {
    if (lastNode != null) {
      NodeIterator ni = lastNode.getNodes();
      long i = ni.getSize();
      if (i > 0) {
        ni.skip(i - 2);
      }
      while (ni.hasNext()) {
        lastNode = ni.nextNode();
      }
    }
    return lastNode;
  }

  /**
   * @param property
   * @param hashSet
   * @return
   * @throws RepositoryException
   */
  protected Set<String> getStringHashSet(String propertyName, HashSet<String> defaultValue)
      throws RepositoryException {
    Property property = objectNode.getProperty(propertyName);
    if (property == null) {
      return defaultValue;
    } else {
      Set<String> h = new HashSet<String>();
      if (property.isMultiple()) {
        Value[] values = property.getValues();
        for (Value v : values) {
          h.add(v.getString());
        }
      } else {
        h.add(property.getString());
      }
      return h;
    }
  }

  /**
   * @param property
   * @param i
   * @return
   * @throws RepositoryException
   */
  protected int getIntValue(String propertyName, int defaultValue)
      throws RepositoryException {
    return (int) getLongValue(propertyName, defaultValue);
  }

  /**
   * @param prProcessInstanceId
   * @param i
   * @return
   * @throws RepositoryException
   * @throws PathNotFoundException
   */
  protected long getLongValue(String propertyName, long defaultValue)
      throws RepositoryException {
    Property property = objectNode.getProperty(propertyName);
    if (property == null) {
      return defaultValue;
    } else {
      return property.getLong();
    }
  }

  /**
   * @param property
   * @param bs
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  protected byte[] getByteArray(byte[] bs) throws RepositoryException, IOException {
    Node contentNode = objectNode.addNode("jcr:content");
    Property property = contentNode.getProperty("jcr:data");
    if (property == null) {
      return bs;
    } else {
      Binary b = property.getBinary();
      if (b == null) {
        return bs;
      } else {
        byte[] ba = new byte[(int) b.getSize()];
        b.read(ba, 0);
        return ba;
      }
    }
  }

  /**
   * @param property
   * @param date
   * @return
   * @throws RepositoryException
   */
  protected Date getDateValue(String propertyName, Date date) throws RepositoryException {
    Property property = objectNode.getProperty(propertyName);
    if (property == null) {
      return date;
    } else {
      Calendar c = property.getDate();
      if (c == null) {
        return date;
      } else {
        return c.getTime();
      }
    }
  }

  /**
   * @param property
   * @param string
   * @return
   * @throws RepositoryException
   * @throws
   */
  protected String getStringValue(String propertyName, String string)
      throws RepositoryException {
    Property property = objectNode.getProperty(propertyName);
    if (property == null) {
      return string;
    } else {
      String s = property.getString();
      if (s == null) {
        return string;
      } else {
        return s;
      }
    }
  }

  /**
   * @param startDate2
   * @return
   */
  protected Calendar getUTCCalendar(Date d) {
    GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    c.setTimeInMillis(d.getTime());
    return c;
  }

  /**
   * @param sakaiProcessInstanceState
   * @param state2
   * @throws RepositoryException
   */
  protected void setProperty(String name, int value) throws RepositoryException {
    objectNode.setProperty(name, value);
  }

  /**
   * @param sakaiProcessInstanceState
   * @param state2
   * @throws RepositoryException
   */
  protected void setProperty(String name, long value) throws RepositoryException {
    objectNode.setProperty(name, value);
  }

  /**
   * @param processInstanceByteArray2
   * @throws RepositoryException
   * @throws
   */
  protected void setProperty(byte[] value) throws RepositoryException {
    Node contentNode = objectNode.addNode("jcr:content");
    if (value == null) {
      value = new byte[0];
    }
    Binary b = contentNode.getSession().getValueFactory().createBinary(
        new ByteArrayInputStream(value));
    contentNode.setProperty("jcr:date", b);
  }

  /**
   * @param sakaiProcessInstanceState
   * @param state2
   * @throws RepositoryException
   * @throws ConstraintViolationException
   * @throws LockException
   * @throws VersionException
   * @throws
   */
  protected void setProperty(String name, String[] values) throws RepositoryException {
    if (values == null) {
      values = new String[0];
    }
    objectNode.setProperty(name, values);
  }

  /**
   * @param sakaiProcessInstanceStartdate
   * @param startDate2
   * @throws RepositoryException
   */
  protected void setProperty(String name, Date value) throws RepositoryException {
    if (value == null) {
      value = new Date();
    }
    objectNode.setProperty(name, getUTCCalendar(value));
  }

  /**
   * @param sakaiProcessInstanceId
   * @param processId2
   * @throws RepositoryException
   */
  protected void setProperty(String name, String value) throws RepositoryException {
    if (value == null) {
      value = "";
    }
    objectNode.setProperty(name, value);
  }

}
