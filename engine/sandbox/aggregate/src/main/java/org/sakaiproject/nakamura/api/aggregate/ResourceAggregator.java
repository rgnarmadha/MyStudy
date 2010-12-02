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
package org.sakaiproject.nakamura.api.aggregate;

import org.apache.sling.api.resource.Resource;

import java.util.Map;

/**
 * Implementations of this interface manage the aggregation of modifications to resources
 * onto a set of target resources.
 */
public interface ResourceAggregator {

  /**
   * Aggregate the resource onto the target resoruces informed by the set of properties in the aggregate Properties.
   * @param resource the resource identified by the event
   * @param targetResource a resource to which aggregation should be applied.
   * @param agregateProperties a map of properties derived from the event that triggered the aggregation operation.
   */
  public void aggregate(Resource resource, Resource targetResource, Map<String, Object> agregateProperties );
  
}
