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
package org.sakaiproject.nakamura.remotefiles;

import com.caucho.hessian.client.HessianProxyFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

/**
 * This is a bundle to provide access to files on a remote server
 * 
 * This processor will write/read files at a remote URI
 */
@Component(enabled = true, immediate = true, metatype = true)
@Service(value = RemoteFilesRepository.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "Sakai Foundation"),
    @Property(name = "service.description", value = "Remote Files API implementation"),
    @Property(name = "repository.type", value = "remotefiles")})
public class HessianRemoteFiles implements RemoteFilesRepository {
  
  
  @Property(name = "remotefiles.host", description = "The remote host (and port) of the remote files server")
  protected String remoteFilesHost = "http://example.com:8080";
  
  protected String remotePath = "/remoting/remoting/RemoteFilesService";
  
  RemoteFilesRepository remoteFilesRepository;

  public void createDirectory(String arg0, String arg1, String arg2, String arg3) {
    remoteFilesRepository.createDirectory(arg0, arg1, arg2, arg3);
  }

  public void createGroup(String arg0, String arg1) {
   remoteFilesRepository.createGroup(arg0, arg1);
  }

  public List<Map<String,Object>> doSearch(Map<String, Object> arg0, String arg1) {
    return remoteFilesRepository.doSearch(arg0, arg1);
  }

  public void removeDocument(String arg0, String arg1) {
    remoteFilesRepository.removeDocument(arg0, arg1);
  }

  public void toggleMember(String arg0, String arg1) {
    remoteFilesRepository.toggleMember(arg0, arg1);
  }

  public void updateFile(String arg0, byte[] arg1, Map<String, Object> arg2, String arg3) {
    remoteFilesRepository.updateFile(arg0, arg1, arg2, arg3);
  }
  
  /**
   * When the component gets activated we retrieve the OSGi properties.
   *
   * @param context
   */
  protected void activate(ComponentContext context) {
    String bundleRemoteFilesHost = context.getBundleContext().getProperty("remotefiles.host");
    if (bundleRemoteFilesHost != null) {
      remoteFilesHost = bundleRemoteFilesHost;
    }
    // Get the properties from the console.
    Dictionary<?, ?> props = context.getProperties();
    if (props.get("remotefiles.host") != null) {
      remoteFilesHost = props.get("remotefiles.host").toString();
    }

    try {
      HessianProxyFactory factory = new HessianProxyFactory();
      remoteFilesRepository = (RemoteFilesRepository) factory.create(RemoteFilesRepository.class,
          remoteFilesHost + remotePath, HessianRemoteFiles.class.getClassLoader());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  public boolean shareFileWithGroup(String groupId, String filePath, String userId) {
    return remoteFilesRepository.shareFileWithGroup(groupId, filePath, userId);
  }

  public Map<String, Object> getDocument(String path, String userId) {
    return remoteFilesRepository.getDocument(path, userId);
  }

  public byte[] getFileContent(String path, String userId) {
    return remoteFilesRepository.getFileContent(path, userId);
  }

}
