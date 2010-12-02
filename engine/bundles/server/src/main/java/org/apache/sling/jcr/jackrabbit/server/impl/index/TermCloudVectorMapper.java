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

import org.apache.lucene.index.TermVectorMapper;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.sling.jcr.jackrabbit.server.index.CloudTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 *
 */
public class TermCloudVectorMapper extends TermVectorMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermCloudVectorMapper.class);
    private TermCloud termCloud;
    private int maxSize;

    /**
     * @param maxSize
     */
    public TermCloudVectorMapper(int maxSize) {
        super(true,true);
        termCloud = new TermCloud(maxSize);
        this.maxSize = maxSize;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.lucene.index.TermVectorMapper#map(java.lang.String, int, org.apache.lucene.index.TermVectorOffsetInfo[], int[])
     */
    @Override
    public void map(String term, int frequency, TermVectorOffsetInfo[] offsets, int[] positions) {
        termCloud.add(new CloudTermImpl(term, frequency));
    }

    /**
     * {@inheritDoc}
     * @see org.apache.lucene.index.TermVectorMapper#setExpectations(java.lang.String, int, boolean, boolean)
     */
    @Override
    public void setExpectations(String field, int numTerms, boolean storeOffsets,
            boolean storePositions) {
        if ( numTerms > maxSize*2 ) {
            LOGGER.warn("There are many more terms than there is space for in the cloud, this could loose critical terms.");
        }
    }

    /**
     * @param i
     * @return
     */
    public Iterator<CloudTerm> iterator(int limit) {
        return termCloud.iterator(limit);
    }

}
