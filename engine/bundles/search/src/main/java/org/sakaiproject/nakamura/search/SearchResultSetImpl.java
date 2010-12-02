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
package org.sakaiproject.nakamura.search;

import org.apache.sling.api.resource.ValueMap;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;

import javax.jcr.query.RowIterator;



/**
 * A final class that provides search result as a set. The reason this class is final and 
 * protected is to avoid search result processors creating the class except via the factory that
 * ensures that the counting iterators are wired up correctly. So, Do not use this class directly or
 * be tempted to remove the protection that has been put in place. The correct way to get hold of an implementation
 * is to use one of:
 * <ul>
 * <li> {@link SearchServiceFactory#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest, javax.jcr.query.Query) } </li>
 * <li> {@Link SearchServiceFactory#getSearchResultSet(RowIterator) </li>
 * <li> {@link SearchServiceFactory#getSearchResultSet(RowIterator, int)</li>
 * </ul>
 * 
 * Your implementation of RowIterator must not iterate through all results but produce them in a lazy way to avoid loading millions of items 
 * from the JCR. If you need additional filtering you can chain row iterators.
 * eg:
 * <pre>
 * SearchResultSet srs = SearchServiceFactory.getSearchResultSet(searchResultSet.getPathFileredRowIterator(new MySpecialRowIterator(request, query)));
 * </pre>
 */
final class SearchResultSetImpl implements SearchResultSet {


  /**
   * The iterator that should be used.
   */
  private RowIterator rowIterator;

  /**
   * A set of properties that should be outputted by the SearchServlet.
   */
  private ValueMap properties;

  public SearchResultSetImpl(RowIterator rowIterator, int maxResults) {
    setRowIterator(rowIterator, maxResults);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getSize()
   */
  public long getSize() {
    return rowIterator.getSize();
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getRowIterator()
   */
  public RowIterator getRowIterator() {
    return rowIterator;
  }

  /**
   * This is protected because I only want the creator to be able to set, but extensions
   * can override the storage.
   */
  protected void setRowIterator(RowIterator rowIterator, int maxResults) {
    this.rowIterator = new CountingRowIterator(rowIterator, maxResults);
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#setProperties(java.util.Dictionary)
   */
  public void setProperties(ValueMap properties) {
    this.properties = properties;
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultSet#getProperties()
   */
  public ValueMap getProperties() {
    return properties;
  }

}
