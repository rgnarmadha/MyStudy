/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.sakaiproject.nakamura.webdav;

import org.apache.jackrabbit.webdav.simple.DefaultItemFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Implements an optional item filter, only used if present.
 * 
 */
public class SlingItemFilter extends DefaultItemFilter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SlingItemFilter.class);

    public boolean isFilteredItem(Item item) {
        if (internalIsFilteredItem(item)) {
            LOGGER.debug("Filtered Item {} ", item);
            return true;
        }
        if (super.isFilteredItem(item)) {
            LOGGER.debug("Super Filtered Item {} ", item);
            return true;
        }
        LOGGER.debug("Non Filtered Item {} ", item);
        return false;
    }
    
    private boolean internalIsFilteredItem(Item item) {
        try {
            return isFilteredItem(item.getPath(), item.getSession());
        } catch (RepositoryException e) {
            LOGGER.warn(e.getMessage());
        }
        return false;
    }

    public boolean isFilteredItem(String name, Session session) {
        if (name.startsWith("/_user") || name.startsWith("/_group")) {
            LOGGER.debug("Filtered {} ", name);
            return true;
        }
        if (super.isFilteredItem(name, session)) {
            LOGGER.debug("Super Filtered {} ", name);
            return true;
        }
        LOGGER.debug("Non Filtered {} ", name);
        return false;
    }


}
