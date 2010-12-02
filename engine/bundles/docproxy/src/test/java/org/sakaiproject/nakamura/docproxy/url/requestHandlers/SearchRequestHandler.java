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
package org.sakaiproject.nakamura.docproxy.url.requestHandlers;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.UrlDocumentResult;

import java.io.IOException;
import java.util.Map.Entry;

/**
 *
 */
public class SearchRequestHandler implements HttpRequestHandler {
  private static final String START_ELEMENT_PATTERN = "<document contentLength=\"%s\" contentType=\"%s\" uri=\"%s\">\n";
  private static final String DOCUMENT_ELEMENT_PATTERN = "<document contentLength=\"%s\" contentType=\"%s\" uri=\"%s\" />\n";

  private UrlDocumentResult[] docs;

  public SearchRequestHandler(UrlDocumentResult... docs) {
    this.docs = docs;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.http.protocol.HttpRequestHandler#handle(org.apache.http.HttpRequest,
   *      org.apache.http.HttpResponse, org.apache.http.protocol.HttpContext)
   */
  public void handle(HttpRequest request, HttpResponse response, HttpContext context)
      throws HttpException, IOException {
    response.setStatusCode(200);
    response.setHeader("Content-type", "text/xml");

    StringBuilder output = new StringBuilder("<search>\n");
    output.append("<documents>\n");
    for (UrlDocumentResult doc : docs) {
      if (doc.getProperties() == null || doc.getProperties().size() == 0) {
        // add the starting element
        output.append(String.format(DOCUMENT_ELEMENT_PATTERN, doc.getContentLength(), doc
            .getContentType(), doc.getUri()));
      } else {
        // add the starting element
        output.append(String.format(START_ELEMENT_PATTERN, doc.getContentLength(), doc
            .getContentType(), doc.getUri()));

        // add property elements
        output.append("<properties>\n");
        for (Entry<String, Object> entry : doc.getProperties().entrySet()) {
          output.append("<" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey()
              + ">\n");
        }
        output.append("</properties>\n");

        // add the ending element
        output.append("</document>\n");
      }
    }
    output.append("</documents>\n");
    output.append("</search>\n");

    StringEntity entity = new StringEntity(output.toString());
    response.setEntity(entity);
  }

}
