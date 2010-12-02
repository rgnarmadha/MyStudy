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
package org.sakaiproject.nakamura.api.site;

import org.apache.commons.lang.StringUtils;

/**
 * A cotnainer for sort ordering.
 */
public class Sort {
  private SortField field;
  private SortOrder order;

  /**
   * 
   */
  public Sort(SortField field, SortOrder order) {
    this.field = field;
    this.order = order;
  }

  /**
   * @param string
   */
  public Sort(String sortSpec) throws IllegalArgumentException {
    String[] spec = StringUtils.split(sortSpec, ",", 2);
    this.field = SortField.valueOf(spec[0]);
    if (spec.length > 1) {
      this.order = SortOrder.valueOf(spec[1]);
    } else {
      this.order = SortOrder.asc;
    }
  }

  /**
   * @return the field
   */
  public SortField getField() {
    return field;
  }

  /**
   * @return the order
   */
  public SortOrder getOrder() {
    return order;
  }
}
