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

/**
 * Represents a term in the term cloud. Terms are considered equal based on
 * their name and not on their count, since we want to add them to Sets and
 * maps. If you need to compare terms counts then you should look at the
 * property directly.
 */
public class CloudTermImpl implements Comparable<CloudTerm>, CloudTerm {
    private int count;
    private String name;

    public CloudTermImpl(String name, int count) {
        this.name = name;
        this.count = count;
    }

    public void merge(CloudTermImpl toMerge) {
        if (isSameTerm(toMerge)) {
            this.count += toMerge.count;
        }
    }

    /**
     * {@inheritDoc}
     * Order terms by the count and then by the name.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(CloudTerm o) {
        int d = o.getCount() - count;
        if ( d == 0 ) {
            return name.compareTo(o.getName());
        }
        return d;
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) {
            return false;
        }
        if ( obj instanceof CloudTerm ) {
            CloudTerm ct = (CloudTerm) obj;
            if ( ct.getCount() == count ) {
                String ctName = ct.getName();
                if ( name == null ) {
                    if ( ctName == null ) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return name.equals(ctName);
                }
            }
        }
        return super.equals(obj);
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        if ( name == null ) {
            return count;
        } else {
            return count + name.hashCode();
        }
    }
    

    /**
     * @return
     */
    public int getCount() {
        return count;
    }
    
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param t
     * @return
     */
    public boolean isSameTerm(CloudTermImpl t) {
        return name.equals(t.name);
    }
    
    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name+":"+count;
    }

}
