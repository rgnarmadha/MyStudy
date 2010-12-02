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
package org.apache.sling.jcr.jackrabbit.server.index;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.sling.jcr.jackrabbit.server.impl.index.TermCloudVectorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

/**
 *
 */
public class TermCloudExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(TermCloudExtractor.class);
    private IndexReader indexReader;
    private TermCloudVectorMapper cloudTermVectorMapper;

    public TermCloudExtractor(QueryManager queryManager, int maxSize) {
        indexReader = adaptTo(queryManager, "searchMgr", "handler", "index");
        if (indexReader == null) {
            throw new IllegalArgumentException(
                    "Failed to get index Reader from Query Manager, which should have been a QueryManagerImpl, but was "
                            + queryManager.getClass());
        }
        cloudTermVectorMapper = new TermCloudVectorMapper(maxSize);
    }

    /**
     * Testing only.
     */
    protected TermCloudExtractor() {
    }

    /**
     * Adapts a QueryManager into the underlying Lucene Index Reader
     * 
     * @param queryManager
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T> T adaptTo(Object startObject, String... fieldPath) {
        Object currentObject = startObject;
        try {
            for (String field : fieldPath) {
                Field nextField = currentObject.getClass().getDeclaredField(field);
                nextField.setAccessible(true);
                currentObject = nextField.get(currentObject);
            }
            return (T) currentObject;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    public void add(Node node) throws RepositoryException {
        try {
            Term idTerm = new Term(FieldNames.UUID, node.getIdentifier().toString());
            TermDocs tDocs = indexReader.termDocs(idTerm);
            try {
                if (tDocs.next()) {
                    int docNumber = tDocs.doc();
                    indexReader.getTermFreqVector(docNumber, FieldNames.FULLTEXT,
                            cloudTermVectorMapper);
                }
            } finally {
                tDocs.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to add Node " + node + " to the term cloud " + e.getMessage());
        }
    }
    
    public Iterator<CloudTerm> termIteator(int limit) {
        return cloudTermVectorMapper.iterator(limit);
    }
}
