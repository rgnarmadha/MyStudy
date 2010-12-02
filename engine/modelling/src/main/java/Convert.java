import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVReader;

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
public class Convert {

  private static final String[] COLUMNS = { "LCID", "B Point Of View", "C Simple Goal",
      "D Alternative Goal", "E Capability", "F Capability+", "G More Complex example",
      "H Innovations", "I 1st Round Major Themes", "J Activity Flows", "K Categories",
      "L Chickering & Gamsen - T&L Good Practices", "M Comments" };

  private static final String[] FILES = { null, "B", "C", "D", "E", "F", "G" };

  /**
   * @param args
   * @throws IOException
   * @throws JSONException
   */
  public static void main(String[] args) throws IOException, JSONException {

    // convert("eddmaster1","EddMaster1");
    // convert("nyuedd","NYUviaEdd");
    // convert("requirements","requirementpath", false);
//     convert("translate","TranslateRequirements", true);
//     convertAndCompact("requirements.json","translate.json");
//    convertAndTranslate("eddmaster1.json", "translate.json", "requirementpath", "rpv1",
//        "requirementpath");
//    convertAndTranslate("johnusermap.json", "translate.json", "requirementpath", "rpv1",
//        "requirementpath");
//    convertAndTranslate("model.json", "translate.json", "requirementpath", "rpv1",
//        "requirementpath");
//    convertAndTranslate("nyuedd.json", "translate.json", "requirementpath", "rpv1",
//        "requirementpath");
//   convertAndTranslate("screens.json", "translate.json", "requirementpath", "rpv1",
//        "requirementpath");
    
      expandHirachyReferences("eddmaster1.json", "requirementpath");
      expandHirachyReferences("model.json", "requirementpath");
      expandHirachyReferences("nyuedd.json", "requirementpath");
      expandHirachyReferences("requirements.json", "requirementpath");
      expandHirachyReferences("screens.json", "requirementpath");
  }
  
  private static void expandHirachyReferences(String modelName, String pathProperty ) throws IOException, JSONException {
    JSONObject model = JSONUtil.loadJson(modelName);
    // process the translate items

    JSONArray a = model.getJSONArray("items");
    for (int i = 0; i < a.length(); i++) {
      JSONObject jo = a.getJSONObject(i);
      if ( jo.has(pathProperty)) {
        Object o = jo.get(pathProperty);
        if ( o instanceof JSONArray ) {
          JSONArray ja = (JSONArray) o;
          Set<String> values =  new HashSet<String>();
          for ( int j = 0; j< ja.length(); j++ ) {
            String path = ja.getString(j);
            values.add(path);
            int k = path.lastIndexOf('.');
            while(k> 0) {
              path = path.substring(0, k);
              values.add(path);
              k = path.lastIndexOf('.');
            }
          }
          JSONArray nja = new JSONArray();
          for ( String v : values) {
            nja.put(v);
          }
          jo.put(pathProperty, nja);
        } else {
          String path = jo.getString(pathProperty);
          Set<String> values =  new HashSet<String>();
          int k = path.lastIndexOf('.');
          while(k> 0) {
            path = path.substring(0, k);
            values.add(path);
            k = path.lastIndexOf('.');
          }
          JSONArray nja = new JSONArray();
          for ( String v : values) {
            nja.put(v);
          }
          jo.put(pathProperty, nja);          
        } 
      }
    }
    
    JSONUtil.saveJson(model, modelName);
    
  }

  /**
   * @param string
   * @param string2
   * @throws JSONException
   * @throws IOException
   */
  private static void convertAndCompact(String toCompact, String translate)
      throws IOException, JSONException {
    JSONObject translateModel = JSONUtil.loadJson(translate);
    // process the translate items

    JSONArray a = translateModel.getJSONArray("items");
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      translateModel.put(o.getString("id"), o);
    }
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      String to = o.getString("translateto");
      if (!translateModel.has(to)) {
        throw new IllegalArgumentException("Translate Map does not contain key [" + to
            + "] for " + o.getString("id"));
      }
    }
    JSONObject toCompactModel = JSONUtil.loadJson(toCompact);
    Map<String, JSONObject> newModelMap = new HashMap<String, JSONObject>();
    a = toCompactModel.getJSONArray("items");
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      JSONObject toJ = translateModel.getJSONObject(o.getString("id"));
      String name = toJ.getString("translateto");
      o.put("id", name);
      int j = name.lastIndexOf('.');
      if (j > 0) {
        String parent = name.substring(0, j);
        o.put("parent", parent);
      }
      JSONArray ja = new JSONArray();
      String path = name;
      j = path.lastIndexOf('.');
      while(j> 0) {
        path = path.substring(0, j);
        ja.put(path);
        j = path.lastIndexOf('.');
      }
      o.put("requirementpath",ja);
      
      
      o.put("label", name);

      newModelMap.put(name, o);
    }

    JSONObject newModel = new JSONObject();
    a = new JSONArray();
    newModel.put("items", a);
    for (Entry<String, JSONObject> e : newModelMap.entrySet()) {
      a.put(e.getValue());
    }

    JSONUtil.saveJson(newModel, toCompact+".trans");

  }

  /**
   * @param string
   * @param string2
   * @param string3
   * @param b
   * @throws JSONException
   * @throws IOException
   */
  private static void convertAndTranslate(String modelName, String translate,
      String currentReferences, String savedReferences, String newReferences)
      throws IOException, JSONException {
    JSONObject model = JSONUtil.loadJson(modelName);
    JSONObject translateModel = JSONUtil.loadJson(translate);
    // process the translate items

    JSONArray a = translateModel.getJSONArray("items");
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      translateModel.put(o.getString("id"), o);
    }
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      String to = o.getString("translateto");
      if (!translateModel.has(to)) {
        throw new IllegalArgumentException("Translate Map does not contain key [" + to
            + "] for " + o.getString("id"));
      }
    }

    // translate the model
    a = model.getJSONArray("items");
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      Set<String> refs = new HashSet<String>();
      if (o.has(currentReferences)) {
        JSONArray currentRefs = o.getJSONArray(currentReferences);
        if (savedReferences != null) {
          o.put(savedReferences, currentRefs);
        }
        for (int j = 0; j < currentRefs.length(); j++) {
          String r = currentRefs.getString(j);
          JSONObject to = translateModel.getJSONObject(r);
          refs.add(to.getString("translateto"));
        }
        JSONArray newRefs = new JSONArray();
        for (String r : refs) {
          newRefs.put(r);
        }
        o.put(newReferences, newRefs);
      }
    }

    JSONUtil.saveJson(model, modelName);
  }

  /**
   * @param string
   * @param string2
   * @throws IOException
   * @throws JSONException
   */
  private static void convert(String file, String type, boolean postprocess)
      throws IOException, JSONException {
    CSVReader reader = new CSVReader(new FileReader(file + ".csv"));
    String[] nextLine;
    String[] headers = reader.readNext();
    JSONObject jo = new JSONObject();
    JSONArray a = new JSONArray();
    jo.put("items", a);
    for (int i = 0; i < headers.length; i++) {
      headers[i] = JSONUtil.safeId(headers[i]).toLowerCase();
    }
    while ((nextLine = reader.readNext()) != null) {
      JSONObject o = new JSONObject();
      for (int i = 0; i < nextLine.length && i < headers.length; i++) {
        o.put(headers[i], nextLine[i]);
      }
      if (postprocess) {
        o.put("type", type);
        o.put("label", o.getString("id"));
        JSONUtil.toArray(o, "sakai-pov");
        JSONUtil.toArray(o, "requirementpath");
      }

      a.put(o);
    }
    File f = new File(file + ".json");
    FileWriter fw = new FileWriter(f);
    fw.append(jo.toString(4));
    fw.close();
  }

  /**
   * @param string
   * @param string2
   * @param string3
   * @param string4
   */
  private static void outputElement(String keyId, String keyValue, String headerName,
      String value, int column) {
    if (column < COLUMNS.length && COLUMNS[column] != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("<e id=\"").append(keyValue.trim()).append("-").append(column).append(
          "\"");
      sb.append(" name=\"").append(COLUMNS[column].trim()).append("\"");
      sb.append(" >");
      sb.append(value.trim());
      sb.append("</e>");
      System.out.println(sb.toString());
    }
  }

  private static void outputFile(String keyId, String keyValue, String pointOfView,
      String headerName, String value, int column) throws IOException, JSONException {
    if (column < FILES.length && FILES[column] != null && value.trim().length() != 0) {
      File f = new File("dataset/" + keyValue.trim() + "-" + FILES[column] + ".json");
      f.getParentFile().mkdirs();
      JSONObject o = new JSONObject();
      o.put("sling:resourceType", "sakai/learningCapability");
      o.put("sakai:pov", pointOfView);
      o.put("sakai:id", keyValue.trim() + "_" + column);
      o.put("sakai:name", COLUMNS[column]);
      o.put("sakai:capability", value.trim());
      FileWriter fw = new FileWriter(f);
      fw.append(o.toString(4));
      fw.close();
    }
  }

  private static void outputFile(JSONObject jo, String keyId, String keyValue,
      String pointOfView, String headerName, String value, int column)
      throws IOException, JSONException {
    if (column < FILES.length && FILES[column] != null && value.trim().length() != 0) {
      String name = keyValue.trim() + "-" + FILES[column];
      JSONObject o = new JSONObject();
      o.put("sling:resourceType", "sakai/learningCapability");
      o.put("sakai:pov", pointOfView);
      o.put("sakai:id", keyValue.trim() + "_" + column);
      o.put("sakai:name", COLUMNS[column]);
      o.put("sakai:capability", value.trim());
      jo.put(name, o);
    }
  }

}
