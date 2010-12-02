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
package org.sakaiproject.nakamura.image;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.util.PathUtils;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet will crop and cut images.
 */
@SlingServlet(paths = "/var/image/cropit", methods = { "POST" })
@Properties(value = { @Property(name = "service.description", value = "Crops an image."),
    @Property(name = "service.vendor", value = "The Sakai Foundation") })
@ServiceDocumentation(name = "CropItServlet", shortDescription = "Crop an image.", description = "Use this servlet to cut out a part of an image and resize it to different sizes.", bindings = @ServiceBinding(type = BindingType.PATH, bindings = "/var/image/cropit"), methods = @ServiceMethod(name = "POST", description = "Cut out part of an image and save it in different sizes. <br />"
    + "Example: curl -d\"img=/dev/_images/gateway.png\" -d\"save=/test\" -d\"x=0\" -d\"y=0\" -d\"width=100\" -d\"height=100\" -d\"dimensions=16x16;32x32\" http://admin:admin@localhost:8080/var/image/cropit", parameters = {
    @ServiceParameter(name = "img", description = "The location where the image that needs cropping is saved."),
    @ServiceParameter(name = "save", description = "The location where you want to save the resized/cropped images."),
    @ServiceParameter(name = "x", description = "Start cropping from this point on the x-axis. Must be an integer-value."),
    @ServiceParameter(name = "y", description = "Start cropping from this point on the y-axis. Must be an integer-value."),
    @ServiceParameter(name = "width", description = "How many pixels to crop out starting from the x point. Must be an integer-value."),
    @ServiceParameter(name = "height", description = "How many pixels to crop out starting from the y point. Must be an integer-value."),
    @ServiceParameter(name = "dimensions", description = "A list of dimensions you want the cropped out image to be resized in. Example: 32x32;256x256;128x128.") }, response = {
    @ServiceResponse(code = 200, description = "Everything is OK, a JSON response is also provided with an array of all the created url's.<br />"
        + "Example: {\"files\":[\"/test/16x16_gateway.png\",\"/test/32x32_gateway.png\"]}"),
    @ServiceResponse(code = 400, description = "There is a missing (or invalid) parameter."),
    @ServiceResponse(code = 406, description = "The provided image is not a valid imagetype."),
    @ServiceResponse(code = 500, description = "Failure, explanation is in the HTML.") }))
public class CropItServlet extends SlingAllMethodsServlet {

  private static final Logger logger = LoggerFactory.getLogger(CropItServlet.class);
  private static final long serialVersionUID = 7893384805719426200L;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // Check if the current user is logged in.
    if (request.getRemoteUser().equals("anonymous")) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Anonymous user cannot crop images.");
      return;
    }

    RequestParameter imgParam = request.getRequestParameter("img");
    RequestParameter saveParam = request.getRequestParameter("save");
    RequestParameter xParam = request.getRequestParameter("x");
    RequestParameter yParam = request.getRequestParameter("y");
    RequestParameter widthParam = request.getRequestParameter("width");
    RequestParameter heightParam = request.getRequestParameter("height");
    RequestParameter dimensionsParam = request.getRequestParameter("dimensions");

    if (imgParam == null || saveParam == null || xParam == null || yParam == null
        || widthParam == null || heightParam == null || dimensionsParam == null) {
      response
          .sendError(HttpServletResponse.SC_BAD_REQUEST,
              "The following parameters are required: img, save, x, y, width, height, dimensions");
      return;
    }

    try {
      // Grab the session
      ResourceResolver resourceResolver = request.getResourceResolver();
      Session session = resourceResolver.adaptTo(Session.class);

      String requestImg =  imgParam.getString();
      String requestSave = saveParam.getString();
      String img = expandAuthorizable(session, requestImg);
      String save = expandAuthorizable(session, requestSave);
      int x = Integer.parseInt(xParam.getString());
      int y = Integer.parseInt(yParam.getString());
      int width = Integer.parseInt(widthParam.getString());
      int height = Integer.parseInt(heightParam.getString());
      String[] dimensionsList = StringUtils.split(dimensionsParam.getString(), ';');
      List<Dimension> dimensions = new ArrayList<Dimension>();
      for (String s : dimensionsList) {
        Dimension d = new Dimension();
        String[] size = StringUtils.split(s, 'x');
        int diWidth = Integer.parseInt(size[0]);
        int diHeight = Integer.parseInt(size[1]);

        diWidth = checkIntBiggerThanZero(diWidth, 0);
        diHeight = checkIntBiggerThanZero(diHeight, 0);

        d.setSize(diWidth, diHeight);
        dimensions.add(d);
      }


      x = checkIntBiggerThanZero(x, 0);
      y = checkIntBiggerThanZero(y, 0);
      width = checkIntBiggerThanZero(width, 0);
      height = checkIntBiggerThanZero(height, 0);

      // Make sure the save path is correct.
      save = PathUtils.normalizePath(save) + "/";
      requestSave = PathUtils.normalizePath(requestSave) + "/";

      String[] crop = CropItProcessor.crop(session, x, y, width, height, dimensions, img,
          save);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");


      JSONWriter output = new JSONWriter(response.getWriter());
      output.object();
      output.key("files");
      output.array();
      for (String url : crop) {
        if ( url.startsWith(img) ) {
          url = requestImg + url.substring(img.length());
        }
        if ( url.startsWith(save) ) {
          logger.debug("Chomping [{}][{}][{}]",new Object[] {url ,save ,requestSave});
          url = requestSave + url.substring(save.length());
        }
        output.value(url);
      }
      output.endArray();
      output.endObject();

    } catch (ArrayIndexOutOfBoundsException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "The dimensions have to be specified in a widthxheight;widthxheight fashion.");
      return;
    } catch (NumberFormatException e) {
      response
          .sendError(
              HttpServletResponse.SC_BAD_REQUEST,
              "The following parameters have to be integers: x, y, width, height. (Dimensions has to be of the form widthxheight;widthxheight");
      return;
    } catch (ImageException e) {
      // Something went wrong..
      logger.warn("ImageException e: " + e.getMessage());
      response.sendError(e.getCode(), e.getMessage());
    } catch (JSONException e) {
      response.sendError(500, "Unable to output JSON.");
    } catch (RepositoryException e) {
      logger.warn(e.getMessage(),e);
      response.sendError(500, "Unable to locate user.");
    }
  }

  private String expandAuthorizable(Session session, String path) throws  RepositoryException {
    int start = 0;
    if ( path.startsWith("/~") ) {
      start = "/~".length();
    } else if ( path.startsWith("/user/" )) {
      start = "/user/".length();
    } else if ( path.startsWith("/group/" )) {
      start = "/group/".length();
    }
    if ( start > 0 ) {
      int nextSlash = path.indexOf("/",start);
      if ( nextSlash > 0 ) {
        String id = path.substring(start, nextSlash);
        UserManager um = AccessControlUtil.getUserManager(session);
        Authorizable a = um.getAuthorizable(id);
        ItemBasedPrincipal p = (ItemBasedPrincipal) a.getPrincipal();
        if ( a.isGroup() ) {
          path = "/_group"+p.getPath().substring("/rep:security/rep:authorizables/rep:groups".length())+path.substring(nextSlash);
        } else {
          logger.debug("Processing Path as [{}][{}][{}] ", new Object[]{path, p.getPath(), path.substring(nextSlash)});
          path = "/_user"+p.getPath().substring("/rep:security/rep:authorizables/rep:users".length())+path.substring(nextSlash);     
          logger.debug("Done Processing Path as [{}]", path);
        }
      }
    }
    return path;
  }
  

  protected int checkIntBiggerThanZero(int val, int defaultVal) {
    if (val < 0) {
      return defaultVal;
    }
    return val;
  }
}
