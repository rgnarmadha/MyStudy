import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
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
public class ToExhibitFormat {

  public static void main(String[] argv) throws IOException, JSONException {
    JSONObject out = JSONUtil.loadJson("types.json");
    JSONObject in = JSONUtil.loadJson("model.json");
    JSONArray a = new JSONArray();
    if ( out.has("items") ) {
      a = (JSONArray) out.get("items");
    } else {
      out.put("items", a);
    }
    
    mergeExhibit("model", a);
    mergeExhibit("requirements", a);
    mergeExhibit("johnusermap", a);
    

    JSONUtil.saveJson(out,"exhibit.json");

  }

  /**
   * @param string
   * @param a
   * @throws JSONException 
   * @throws IOException 
   */
  private static void mergeExhibit(String file, JSONArray a) throws IOException, JSONException {
    JSONObject model = JSONUtil.loadJson(file+".json");
    JSONArray items = model.getJSONArray("items");
    for ( int i = 0; i< items.length(); i++ ) {
      JSONObject jo = (JSONObject) items.get(i);
      a.put(jo);
    }
 
  }

}
