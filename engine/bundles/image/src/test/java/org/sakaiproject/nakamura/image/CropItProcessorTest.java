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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.jcr.JCRConstants;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class CropItProcessorTest extends AbstractEasyMockTest {

  private Session session;
  private String img = "/foo/people.png";
  private int x = 0;
  private int y = 0;
  private int width = 100;
  private int height = 100;
  private List<Dimension> dimensions;
  private String save = "/save/in/here/";
  private Node node;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    session = createMock(Session.class);
    node = createMock(Node.class);
    dimensions = new ArrayList<Dimension>();
    Dimension d = new Dimension();
    d.setSize(50, 50);
    dimensions.add(d);
    expect(session.getItem(img)).andReturn(node);
  }

  /**
   * @param node
   * @param string
   * @throws RepositoryException
   * @throws ValueFormatException
   */
  private void createMimeType(Node node, String mimeTypeValue)
      throws ValueFormatException, RepositoryException {
    Property mimeType = createMock(Property.class);
    expect(mimeType.getString()).andReturn(mimeTypeValue);
    expect(node.hasProperty(JCRConstants.JCR_MIMETYPE)).andReturn(true);
    expect(node.getProperty(JCRConstants.JCR_MIMETYPE)).andReturn(mimeType);
  }

  @Test
  public void testGetScaledInstance() throws IOException, ImageReadException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage bufImg = Sanselan.getBufferedImage(is);
    BufferedImage croppedImg = CropItProcessor.getScaledInstance(bufImg, 50, 50);
    assertEquals(50, croppedImg.getWidth());
    assertEquals(50, croppedImg.getHeight());
  }

  @Test
  public void testInvalidImage() throws RepositoryException {
    expect(node.getName()).andReturn("foo.bar").anyTimes();
    expect(node.getPath()).andReturn("/path/to/the/file/foo.bar");
    expect(node.isNodeType("nt:file")).andReturn(false);
    expect(node.isNodeType("nt:resource")).andReturn(false);
    expect(node.hasNode(JCRConstants.JCR_CONTENT)).andReturn(false);
    expect(node.hasProperty(JCRConstants.JCR_DATA)).andReturn(false);
    replay();
    try {
      CropItProcessor.crop(session, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(500, e.getCode());
    }
  }

  @Test
  public void testInvalidImageMimeType() throws RepositoryException {
    expect(node.getName()).andReturn("foo.bar").anyTimes();
    expect(node.getPath()).andReturn("/path/to/foo.bar");
    expect(node.isNodeType("nt:file")).andReturn(true);
    expect(node.hasNode(JCRConstants.JCR_CONTENT)).andReturn(true);

    InputStream in = getClass().getResourceAsStream("not.an.image");
    Node contentNode = createMock(Node.class);
    expect(contentNode.isNodeType("nt:resource")).andReturn(true);
    Property streamProp = createMock(Property.class);
    Binary bin = createMock(Binary.class);
    expect(streamProp.getBinary()).andReturn(bin);
    expect(bin.getSize()).andReturn(100L); // this is not the correct length but here its ok.
    expect(bin.getStream()).andReturn(in);
    expect(contentNode.getProperty(JCRConstants.JCR_DATA)).andReturn(streamProp);
    createMimeType(contentNode, "image/foo");
    expect(node.getNode(JCRConstants.JCR_CONTENT)).andReturn(contentNode);

    replay();
    try {
      CropItProcessor.crop(session, x, y, width, height, dimensions, img, save);
      fail("The processor should not handle non-images.");
    } catch (ImageException e) {
      assertEquals(406, e.getCode());
    }
  }

  @Test
  public void testscaleAndWriteToStream() throws IOException, ImageWriteException,
      ImageReadException {
    InputStream is = getClass().getResourceAsStream("people.png");
    BufferedImage imgBuf = Sanselan.getBufferedImage(is);
    BufferedImage subImage = imgBuf.getSubimage(0, 0, 100, 100);
    ImageInfo info = new ImageInfo("PNG", 8, null, ImageFormat.IMAGE_FORMAT_PNG, "PNG",
        256, "image/png", 1, 76, 76, 76, 76, 256, true, true, false, 2, "ZIP");
    byte[] image = CropItProcessor.scaleAndWriteToByteArray(50, 50, subImage,
        "people.png", info);
    InputStream scaledIs = new ByteArrayInputStream(image);
    BufferedImage scaledImage = ImageIO.read(scaledIs);
    assertEquals(scaledImage.getWidth(), 50);
    assertEquals(scaledImage.getHeight(), 50);
  }

}
