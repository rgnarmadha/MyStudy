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

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.util.IOUtils;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

public class CropItProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CropItProcessor.class);

  /**
   *
   * @param session
   *          The JCR session
   * @param x
   *          Where to start cutting on the x-axis.
   * @param y
   *          Where to start cutting on the y-axis.
   * @param width
   *          The width of the image to cut out. If <=0 then the entire image width will
   *          be used.
   * @param height
   *          The height of the image to cut out. If <=0 then the entire image width will
   *          be used.
   * @param dimensions
   *          A List of Dimension who contain the width and height for each dimension the
   *          image should be scaled into.
   * @param img
   *          The location of the image to scale.
   * @param save
   *          The location where to save all the scaled instances.
   * @return returns an array with all the location of the scaled instances.
   * @throws ImageException
   */
  public static String[] crop(Session session, int x, int y, int width, int height,
      List<Dimension> dimensions, String img, String save) throws ImageException {

    InputStream in = null;

    // The array that will contain all the cropped and resized images.
    String[] arrFiles = new String[dimensions.size()];

    try {

      Node imgNode = (Node) session.getItem(img);

      if (imgNode != null) {

        String imgName = imgNode.getName();
        String imgPath = imgNode.getPath();
        // nt:file
        if (imgNode.isNodeType(JCRConstants.NT_FILE)) {
          imgNode = imgNode.getNode(JCRConstants.JCR_CONTENT);
        } else if (imgNode.isNodeType(JCRConstants.NT_RESOURCE)) {
          // This is an nt:resource file.
          // We check if it has an nt:file to use the name from, otherwise we just use
          // this name.
          Node parentNode = imgNode.getParent();
          if (parentNode.isNodeType(JCRConstants.NT_FILE)) {
            imgName = parentNode.getName();
            imgPath = parentNode.getPath();
          }
        } else {
          throw new ImageException(500, "Invalid image");
        }

        String mimeType = "unknown";
        if (imgNode.hasProperty(JCRConstants.JCR_MIMETYPE) ) {
          mimeType = imgNode.getProperty(JCRConstants.JCR_MIMETYPE).getString();
        }

        // Read the image
        Binary content = imgNode.getProperty(JCRConstants.JCR_DATA).getBinary();
        if ( content.getSize() > 100L*1024L*1024L ) {
          throw new ImageException(406, "Image "+imgPath+" too large to crop > 100MB Si "+content.getSize());

        }
        in = content.getStream();
        try {

          // NOTE: I'd prefer to use the InputStream, but I don't see a way to get the
          // ImageInfo _and_ the BufferedImage.
          // I've tried using a BufferedInputStream which allows you to reset, but for BMP
          // this doesn't help.
          byte[] bytes = IOUtils.getInputStreamBytes(in);
          ImageInfo info = Sanselan.getImageInfo(bytes);
          BufferedImage imgBuf = getBufferedImage(bytes, info);

          // Set the correct width & height.
          width = (width <= 0) ? info.getWidth() : width;
          height = (height <= 0) ? info.getHeight() : height;

          if (x + width > info.getWidth()) {
            width = info.getWidth() - x;
          }
          if (y + height > info.getHeight()) {
            height = info.getHeight() - y;
          }

          // Cut the desired piece out of the image.
          BufferedImage subImage = imgBuf.getSubimage(x, y, width, height);

          // Loop the dimensions and create and save an image for each
          // one.
          for (int i = 0; i < dimensions.size(); i++) {

            Dimension d = dimensions.get(i);

            // get dimension size
            int iWidth = d.width;
            int iHeight = d.height;

            iWidth = (iWidth <= 0) ? info.getWidth() : iWidth;
            iHeight = (iHeight <= 0) ? info.getHeight() : iHeight;

            // Create the image.
            byte[] image = scaleAndWriteToByteArray(iWidth, iHeight, subImage, imgName, info);

            if ( image != null ) {

              String sPath = save + iWidth + "x" + iHeight + "_" + imgName;
              // Save new image to JCR.
              saveImageToJCR(sPath, info.getMimeType(), image, imgNode, session);

              arrFiles[i] = sPath;
            } else {
              LOGGER.warn("Failed to scale image "+img+" to "+iWidth+" by "+iHeight+" defaulting to full size");
              arrFiles[i] = img;
            }
          }
        } catch (ImageReadException e) {
          // This is not a valid image.
          LOGGER.error("Can't parse this format. Image {}, mime Type {} :{}", new Object[]{imgPath, mimeType, e.getMessage()});
          LOGGER.debug("Cause: ", e);
          throw new ImageException(406, "Can't parse this format.  Image "+imgPath+", mime Type "+mimeType);
        } catch (ImageWriteException e) {
          LOGGER.error("Can't crop this Image {}, mime Type {}  :{} ", new Object[]{imgPath, mimeType, e.getMessage()});
          LOGGER.debug("Cause: ", e);
          throw new ImageException(406, "Can't crop this Image "+imgPath+", mime Type "+mimeType);
        }
      } else {
        throw new ImageException(400, "No image file found.");
      }

    } catch (PathNotFoundException e) {
      throw new ImageException(400, "Could not find image.");
    } catch (RepositoryException e) {
      LOGGER.error("Unable to crop image.", e);
      throw new ImageException(500, "Unable to crop image.");
    } catch (IOException e) {
      LOGGER.error("Unable to read image in order to crop it.", e);
      throw new ImageException(500, "Unable to read image in order to crop it.");
    } finally {
      // close the streams
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOGGER.debug("Exception closing inputstream.");
        }
      }
    }
    return arrFiles;
  }

  /**
   * @param bytes
   * @param info
   * @return
   * @throws IOException
   * @throws ImageReadException
   * @throws ImageException
   */
  protected static BufferedImage getBufferedImage(byte[] bytes, ImageInfo info)
      throws ImageReadException, IOException, ImageException {
    BufferedImage imgBuf;
    // Guess the format and check if it is a valid one.
    if (info.getFormat() == ImageFormat.IMAGE_FORMAT_UNKNOWN) {
      // This is not a valid image.
      LOGGER.error("Can't parse this format.");
      throw new ImageException(406, "Can't parse this format.");
    } else if (info.getFormat() == ImageFormat.IMAGE_FORMAT_JPEG) {
      imgBuf = ImageIO.read(new ByteArrayInputStream(bytes));
    } else {
      imgBuf = Sanselan.getBufferedImage(bytes);

      // KERN-1113 Sanselan doesn't read the image type correctly when working with some
      // PNG's.  Alpha layer is tricky.
      if (imgBuf.getType() == 0) {
        imgBuf = ImageIO.read(new ByteArrayInputStream(bytes));
      }
    }

    return imgBuf;
  }

  /**
   * Will save a stream of an image to the JCR.
   *
   * @param path
   *          The JCR path to save the image in.
   * @param mimetype
   *          The Mime type of the node that will be saved.
   * @param out
   *          The stream you wish to save.
   * @throws ImageException
   */
  protected static void saveImageToJCR(String path, String mimetype,
      byte[] image, Node baseNode, Session session) throws ImageException {

    // Save image into the jcr
    ByteArrayInputStream bais = null;
    try {
      path = PathUtils.normalizePath(path);
      Node node = JcrUtils.deepGetOrCreateNode(session, path, "nt:file");

      // convert stream to inputstream
      bais = new ByteArrayInputStream(image);
      Node contentNode = null;
      if (node.hasNode(JCRConstants.JCR_CONTENT)) {
        contentNode = node.getNode(JCRConstants.JCR_CONTENT);
      } else {
        contentNode = node.addNode(JCRConstants.JCR_CONTENT, JCRConstants.NT_RESOURCE);
      }
      ValueFactory vf = session.getValueFactory();
      contentNode.setProperty(JCRConstants.JCR_DATA, vf.createBinary(bais));
      contentNode.setProperty(JCRConstants.JCR_MIMETYPE, mimetype);
      contentNode.setProperty(JCRConstants.JCR_LASTMODIFIED, Calendar.getInstance());

      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Repository exception: " + e.getMessage());
      throw new ImageException(500, "Unable to save image to JCR.");
    } finally {
      if (bais != null) {
        try {
          bais.close();
        } catch (IOException e) {
          LOGGER.warn("Unable to close inputstream.");
        }
      }
    }
  }

  /**
   * This method will scale an image to a desired width and height and shall output the
   * stream of that scaled image.
   *
   * @param width
   *          The desired width of the scaled image.
   * @param height
   *          The desired height of the scaled image.
   * @param img
   *          The image that you want to scale
   * @param imgName
   *          Filename of the image
   * @param info
   *          The {@link ImageInfo info} for this image.
   * @return Returns an {@link ByteArrayOutputStream ByteArrayOutputStream} of the scaled
   *         image.
   * @throws IOException
   * @throws ImageWriteException
   *           Failed to write the cropped image to the stream.
   */
  protected static byte[] scaleAndWriteToByteArray(int width, int height,
      BufferedImage img, String imgName, ImageInfo info) throws IOException,
      ImageWriteException {
    try {
      // Get a scaled image.
      BufferedImage imgScaled = getScaledInstance(img, width, height);

      // Convert image to a stream
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      // Write to stream.
      if (info.getFormat() == ImageFormat.IMAGE_FORMAT_JPEG) {
        ImageIO.write(imgScaled, "jpg", out);
      } else if (info.getFormat() == ImageFormat.IMAGE_FORMAT_PNG) {
        ImageIO.write(imgScaled, "png", out);
      } else {
        Sanselan.writeImage(imgScaled, out, info.getFormat(), null);
      }
      out.close();
      return out.toByteArray();
    } catch (Exception e) {
      LOGGER.error("Failed to process image "+e.getMessage(), e);
    }
    return null;
  }


  /**
   * Image scaling routine as prescribed by
   * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html.
   * Image.getScaledInstance() is not very efficient or fast and this leverages the
   * graphics classes directly for a better and faster image scaling algorithm.
   *
   * @param img
   * @param targetWidth
   * @param targetHeight
   * @return
   */
  protected static BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
      int targetHeight) {
    BufferedImage ret = img;

    // Use multi-step technique: start with original size, then
    // scale down in multiple passes with drawImage()
    // until the target size is reached
    int w = img.getWidth();
    int h = img.getHeight();

    while (w > targetWidth || h > targetHeight) {
      // Bit shifting by one is faster than dividing by 2.
      w >>= 1;
      if (w < targetWidth) {
        w = targetWidth;
      }

      // Bit shifting by one is faster than dividing by 2.
      h >>= 1;
      if (h < targetHeight) {
        h = targetHeight;
      }

      BufferedImage tmp = new BufferedImage(w, h, img.getType());
      Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_QUALITY);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();

      ret = tmp;
    }

    return ret;
  }
}
