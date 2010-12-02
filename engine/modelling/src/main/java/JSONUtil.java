import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
public class JSONUtil {
  /**
   * @param string
   * @return
   * @throws IOException
   * @throws JSONException
   */
  public static JSONObject loadJson(String file) throws IOException, JSONException {
    File f = new File(file);
    FileReader fr = new FileReader(f);
    JSONTokener j = new JSONTokener(fr);
    JSONObject in = new JSONObject(j);
    fr.close();
    return in;
  }

  /**
   * @param out
   * @param string
   * @throws IOException
   * @throws JSONException
   */
  public static void saveJson(JSONObject out, String file) throws IOException,
      JSONException {
    File fo = new File(file);
    FileWriter fw = new FileWriter(fo);
    fw.append(out.toString(4));
    fw.close();
  }

  /**
   * @param namedItem
   * @return
   */
  public static String safeId(String nodeName) {
    nodeName = nodeName.replace(' ', '-');
    nodeName = nodeName.replace('_', '-');
    nodeName = nodeName.replace('"', '-');
    nodeName = nodeName.replace(';', '-');
    nodeName = nodeName.replace(',', '-');
    nodeName = nodeName.replace('\'', '-');
    nodeName = nodeName.replace('(', '-');
    nodeName = nodeName.replace(')', '-');
    nodeName = nodeName.replace('.', '-');
    nodeName = nodeName.replace('/', '-');
    nodeName = nodeName.replace('?', '-');
    return nodeName;
  }

  /**
   * @param o
   * @param string
   * @throws JSONException
   */
  public static void toArray(JSONObject o, String key) throws JSONException {
    if (o.has(key)) {
      Object ob = o.get(key);
      if (ob instanceof JSONArray) {
        // no action
      } else if (ob instanceof JSONObject) {
        // no action
      } else {
        String v = o.getString(key);
        String[] values = StringUtils.split(v, ',');
        JSONArray a = new JSONArray();
        for (String value : values) {
          a.put(value);
        }
        o.put(key, a);
      }
    } else {
      JSONArray a = new JSONArray();
      o.put(key, a);
    }

  }

}
