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

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

/**
 * Implements an optional item filter, only used if present.
 */
public class SlingLocatorMapper  {

    private ThreadLocal<Session> sessionHolder = new ThreadLocal<Session>();

    public void unbindSession() {
        sessionHolder.set(null);
    }

    public void bindSession(SessionProvider sessionProvider,
            WebdavRequest request, WebdavResponse response, DavResource resource) {
        try {
            sessionHolder.set(sessionProvider.getSession(request, null, null));
        } catch (RepositoryException e) {
            e.printStackTrace();
        } catch (ServletException e) {
            e.printStackTrace();
        }

    }

    public String mapPath(String resourcePath) {
        Session session = sessionHolder.get();
        if (session != null) {
            if ("/_user".equals(resourcePath)) {
                return "/user";
            } else if ("/_group".equals(resourcePath)) {
                return "/group";
            }
        }
        return resourcePath;
    }

}
