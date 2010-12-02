import au.com.bytecode.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
public class JsonToCsv {


  /**
   * @param args
   * @throws IOException
   * @throws JSONException 
   */
  public static void main(String[] args) throws IOException, JSONException {
    convertToCSV("johnusermap","items");
    convertToCSV("requirements","items");
    convertToCSV("model","items");
    convertToCSV("screens","items");
    convertToCSV("types","items");
    convertToCSV("eddmaster1","items");
    convertToCSV("nyuedd","items");
  }
    
    
    /**
   * @param string
   * @param string2
     * @throws JSONException 
     * @throws IOException 
   */
  private static void convertToCSV(String file, String items) throws JSONException, IOException {
    JSONObject jsonObject = JSONUtil.loadJson(file+".json");
    JSONArray a = jsonObject.getJSONArray(items);
    File f = new File(file+".csv");
    Set<String> nameSet = new HashSet<String>();
    for ( int i = 0; i < a.length(); i++ ) {
      JSONObject o = a.getJSONObject(i);
      for ( String name : JSONObject.getNames(o)) {
        nameSet.add(name);
      }
    }
    nameSet.remove("id");
    String[] names = nameSet.toArray(new String[nameSet.size()]);
    String[] allNames = new String[names.length+1];
    System.arraycopy(names, 0, allNames, 1, names.length);
    allNames[0] = "id";
    List<String[]> sheet = new ArrayList<String[]>();
    sheet.add(allNames);
    for ( int i = 0; i < a.length(); i++ ) {
      JSONObject o = a.getJSONObject(i);
      String[] row = new String[allNames.length];
      for (int j = 0; j < allNames.length; j++) {
        if ( o.has(allNames[j])) {
        row[j] = o.getString(allNames[j]);
        } else {
          row[j] = "";
        }
      }
      sheet.add(row);
    }
    FileWriter fw = new FileWriter(f);
    CSVWriter writer = new CSVWriter(fw);
    writer.writeAll(sheet);
    writer.close();
    fw.close();
  }


}
