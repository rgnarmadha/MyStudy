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
package org.apache.sling.jcr.jackrabbit.server.impl.index;

import org.apache.sling.jcr.jackrabbit.server.index.CloudTerm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A term cloud is a store of {@link Term}s which accepts the addition of new
 * terms and provides an iterator to list {@link Term}s in order of occurrence.
 * The term cloud is constructed with a maximum size, and if there are more
 * terms in the cloud than this size, the least significant terms will be
 * evicted. Callers should ensure that the size of the cloud allows frequent but
 * low counted terms to influence the cloud by making the max size greater than
 * the number of terms required, ie 1.5 times the number of requried terms in
 * the cloud.
 */
public class TermCloud {

    /**
     * 
     */
    private static final long serialVersionUID = 3363155684386506288L;
    /**
     * Internal store of terms.
     */
    private List<CloudTermImpl> termList;
    private int maxSize;
    private boolean sorted = false;

    /**
     * Create a TermCloud
     */
    public TermCloud(int maxSize) {
        termList = new ArrayList<CloudTermImpl>(maxSize + 1);
        this.maxSize = maxSize;
    }

    /**
     * Add a new term to the cloud merging with terms of the same name if they
     * exist.
     * 
     * @param term
     *            the terms to add or merge.
     */
    public synchronized void add(CloudTermImpl term) {
        for (CloudTermImpl t : termList) {
            if (term.isSameTerm(t)) {
                t.merge(term);
                sorted = false;
                return;
            }
        }
        sorted = false;
        termList.add(term);
        if (termList.size() > maxSize) {
            Collections.sort(termList);
            termList.remove(termList.size() - 1);
            sorted = true;
        }
    }

    /**
     * A list of terms, limited by the number of
     * 
     * @param limit
     * @return
     */
    public synchronized Iterator<CloudTerm> iterator(final int limit) {
        if (!sorted) {
            Collections.sort(termList);
            sorted = true;
        }
        final Iterator<CloudTermImpl> iTermList = termList.iterator();
        return new Iterator<CloudTerm>() {

            private CloudTermImpl current;

            public boolean hasNext() {
                if (iTermList.hasNext()) {
                    current = iTermList.next();
                    if (current.getCount() >= limit) {
                        return true;
                    }
                }
                current = null;
                return false;
            }

            public CloudTerm next() {
                return current;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

}
