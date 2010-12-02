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
package org.sakaiproject.nakamura.site;

import org.apache.jackrabbit.api.security.user.Authorizable;

/**
 * 
 */
public class Membership {

  private Authorizable parent;
  private Authorizable member;

  /**
   * 
   */
  public Membership(Authorizable parent, Authorizable member) {
    this.parent = parent;
    this.member = member;
  }

  /**
   * @return the parent
   */
  public Authorizable getParent() {
    return parent;
  }

  /**
   * @return the member
   */
  public Authorizable getMember() {
    return member;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return parent.hashCode() + member.hashCode();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Membership) {
      Membership m = (Membership) obj;
      if ( parent == null ) {
        if ( m.getParent() == null ) {
          return true;
        } else {
          return false;
        }
      } else {
        return ( parent.equals(m.getParent()))
            && member.equals(m.getMember());
      }
    }
    return super.equals(obj);
  }
}
