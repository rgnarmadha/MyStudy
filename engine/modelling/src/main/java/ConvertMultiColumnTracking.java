import au.com.bytecode.opencsv.CSVReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
public class ConvertMultiColumnTracking {

  /**
   * @param args
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   * @throws JSONException
   */
  public static void main(String[] args) throws SAXException, IOException,
      ParserConfigurationException, JSONException {
    CSVReader reader = new CSVReader(new FileReader("test.csv"));
    String[] nextLine;
    String[] headers = reader.readNext();
    for (int i = 0; i < headers.length; i++) {
      headers[i] = JSONUtil.safeId(headers[i]);
    }
    JSONObject jo = new JSONObject();
    JSONArray a = new JSONArray();
    jo.put("items", a);
    while ((nextLine = reader.readNext()) != null) {
      JSONObject o = new JSONObject();
      for (int j = 0; j < headers.length && j < nextLine.length; j++) {
        System.err.println(headers[j] + ":" + nextLine[j]);
        o.put(headers[j].toLowerCase(), nextLine[j]);
      }
      o.put("id", JSONUtil.safeId(o.getString("screen-name")));
      o.put("label", o.get("screen-name"));
      o.put("type", "Screens");
      JSONArray references = new JSONArray();
      loadReferences("Authorizables", o, references);
      loadReferences("Content", o, references);
      loadReferences("Sites", o, references);
      loadReferences("Networking", o, references);
      o.put("requirementPath", references);
      a.put(o);
    }
    JSONUtil.saveJson(jo, "test.json");
  }

  /**
   * @param string
   * @param o
   * @param references
   * @throws JSONException
   */
  private static void loadReferences(String name, JSONObject o, JSONArray references)
      throws JSONException {
    if (o.has(name.toLowerCase())) {
      String ref = o.getString(name.toLowerCase());
      if (ref.trim().length() > 0) {
        if (name.toLowerCase().equals(ref.toLowerCase())) {
          references.put(name);
        } else {
          references.put(name + "." + ref);
        }
      }
      o.remove(name.toLowerCase());
    }
  }

}
