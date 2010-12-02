import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

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
public class ConvertJohnsMMap {

  /**
   * @param args
   * @throws IOException 
   * @throws SAXException 
   * @throws ParserConfigurationException 
   * @throws JSONException 
   */
  public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, JSONException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
    File f = new File("mmap.xml");
    Document doc = db.parse(f);
    NodeList nl = doc.getChildNodes();
    JSONObject jo = new JSONObject();
    JSONArray ja = new JSONArray();
    jo.put("items", ja);
    buildStructure(ja, "",nl);
    
    JSONUtil.saveJson(jo, "mmap.json");
  }
  
  // <node COLOR="#CCCCCC"  BACKGROUND_COLOR="#" CREATED="1269431911000" ID="Freemind_Link_45603059" MODIFIED="1269598791000" TEXT="Export data">
  // <node COLOR="#000000"  BACKGROUND_COLOR="#" CREATED="1269448889000" ID="Freemind_Link_45638511" MODIFIED="1269597987000" TEXT="View by personal map/desktop layout">
  // <font SIZE="12" ITALIC="true"/></node>
  // </node>


  /**
   * @param string
   * @param nl
   * @throws JSONException 
   */
  private static void buildStructure(JSONArray ja, String path, NodeList nl) throws JSONException {
    for ( int i = 0; i < nl.getLength(); i++ ) {
      Node n = nl.item(i);
      String localName = n.getNodeName();
      if ( "node".equals(localName) ) {
        NamedNodeMap nn = n.getAttributes();
        String nodeName = path+JSONUtil.safeId(nn.getNamedItem("TEXT").getTextContent())+".";
        
        Node colorNode = nn.getNamedItem("COLOR");
        String color = "#000000";
        if ( colorNode != null ) {
          color = colorNode.getTextContent();
        }
        
        boolean bold = false;
        boolean italic = false;
        NodeList childNodes = n.getChildNodes();
        for ( int j = 0; j < childNodes.getLength(); j++ ) {
          Node childNode = childNodes.item(j);
          if ( "font".equals(childNode.getNodeName()) ) {
            NamedNodeMap childNodeMap = childNode.getAttributes();
            Node italicNode = childNodeMap.getNamedItem("ITALIC");
            italic = ( italicNode != null && "true".equals(italicNode.getTextContent()));
            Node boldNode = childNodeMap.getNamedItem("BOLD");
            bold = ( boldNode != null && "true".equals(boldNode.getTextContent()));
          }
        }
        
        String implementationPhase = "phase1";
        if ( italic ) {
          implementationPhase = "phase2";
        } else if ( "#CCCCCC".equals(color) ) {
          implementationPhase = "phase3";
        }
        
        
        JSONObject jo = new JSONObject();
        String finalNodeName = nodeName.substring(0,nodeName.length()-1);
        System.err.println("Adding "+finalNodeName);
        jo.put("id", finalNodeName);
        int j = finalNodeName.lastIndexOf('.');
        if ( j > 0 ) {
          String parent = finalNodeName.substring(0,j);
          jo.put("parent", parent);
        }

        jo.put("label", finalNodeName);
        jo.put("type", "UserDesire");
        jo.put("implementationPhase", implementationPhase);
        ja.put(jo);
        buildStructure(ja, nodeName,n.getChildNodes());
      } else {
        buildStructure(ja, path,n.getChildNodes());   
      }
    }
  }


}
