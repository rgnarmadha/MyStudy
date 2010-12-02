import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;

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

/**
 *
 */
public class GP {

  public static void main(String[] args) throws IOException, JSONException {
  }

  
  
  /**
   * @throws JSONException 
   * @throws IOException 
   * 
   */
  private static void restructureRequrements() throws IOException, JSONException {
    JSONObject requirements = JSONUtil.loadJson("requirements.json");
    JSONArray folders = (JSONArray) requirements.get("requirements");
    JSONObject n = new JSONObject();
    JSONArray a = new JSONArray();
    n.put("items", a);
    for ( int i = 0; i< folders.length(); i++ ) {
      JSONObject jo = (JSONObject) folders.get(i);
      String name = jo.getString("id");
      jo.put("requirementPath", name);
      int j = name.lastIndexOf('.');
      if ( j > 0 ) {
        String parent = name.substring(0,j);
        jo.put("parent", parent);
      }
      jo.put("label", name);
      jo.put("type", "RequirementPath");
      a.put(jo);
    }
    
    JSONUtil.saveJson(n, "requirements2.json");
  }



  /*/**
   * 
   */
  private static void restructureModel() throws IOException, JSONException {
    JSONObject o = JSONUtil.loadJson("model.json");
    JSONObject n = new JSONObject();
    JSONArray a = new JSONArray();
    n.put("items", a);
    for (Iterator<String> k = o.keys(); k.hasNext();) {
      String key = k.next();
      Object ox = o.get(key);
      if (ox instanceof JSONObject) {
        ((JSONObject) ox).put("id", key);
        ((JSONObject) ox).put("label",key);
        ((JSONObject) ox).put("type","LearningCapability");
        a.put(ox);
      } else {
        n.put(key, ox);
      }
    }
    JSONUtil.saveJson(n, "model2.json");
  }


  /**
   * @throws JSONException
   * @throws IOException
   * 
   */
  private static void fixPov() throws JSONException, IOException {
    JSONObject o = JSONUtil.loadJson("model.json");
    for (Iterator<String> k = o.keys(); k.hasNext();) {
      String key = k.next();
      Object ox = o.get(key);
      if (ox instanceof JSONObject) {
        JSONObject req = (JSONObject) ox;
        if (req.has("sakai:pov")) {
          String pov = req.getString("sakai:pov");
          
          JSONArray a = new JSONArray();
          splitAndSet(a,pov);
          req.put("sakai:pov", a);
        }
      }
    }
    JSONUtil.saveJson(o, "model2.json");
  }

  /**
   * @param a
   * @param pov
   */
  private static void splitAndSet(JSONArray a, String pov) {
    String povs[] = pov.split("\\W+");
    if ( povs.length > 1 ) {
      for (String p : povs) {
        if (p.trim().length() > 0) {
          splitAndSet(a, p.trim());
        }
      } 
    } else if ( povs.length == 1){
      a.put(povs[0].trim().toLowerCase());
    }
    
  }

  /**
   * @throws JSONException
   * @throws IOException
   * 
   */
  private static void addField() throws IOException, JSONException {
    JSONObject o = JSONUtil.loadJson("requirements.json");
    JSONArray a = o.getJSONArray("requirements");
    for (int i = 0; i < a.length(); i++) {
      JSONObject req = (JSONObject) a.get(i);
      req.put("implementationPhase", "phase1");
    }
    JSONUtil.saveJson(o, "requirements2.json");
  }

}
