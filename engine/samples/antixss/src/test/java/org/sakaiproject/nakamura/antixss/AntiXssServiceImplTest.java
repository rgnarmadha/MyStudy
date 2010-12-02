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
package org.sakaiproject.nakamura.antixss;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentContext;
import org.owasp.validator.html.PolicyException;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 *
 */
public class AntiXssServiceImplTest {

  private static final String CLEAN = "The client requests all content for dom injection with a selector that activates the filter "
      + "eg <a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.forht\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.forht</a>... "
      + " where  &quot;forhtml&quot; activates the filter and the default url with no filtering is "
      + " <a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.json\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.json</a> "
      + " and the URI for the resource is "
      + "<a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent</a> "
      + " With comments for refinement: "
      + "then a clearly articulated policy for when and when not to"
      + " perform filtering needs to be agreed on, enforced and QA cycles "
      + "reserved.  Further, I am not sure if there is more than one filtering "
      + "level needed depending on content purpose. "
      + "[Was tempted to set priority as Blocker] "
      + "An example filtering  library is antSamy: <a href=\"http://www.owasp.org/index.php/Category:OWASP_AntiSamy_Project\">http://www.owasp.org/index.php/Category:OWASP_AntiSamy_Project</a>"
      + " ";

  private static final String CLEANRESULT = "\nThe client requests all content for dom injection with a selector that activates the filter eg <a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.forht\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.forht</a>\n"
      + "\n"
      + "...  where  &quot;forhtml&quot; activates the filter and the default url with no filtering is  <a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.json\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent.json</a>\n"
      + "  and the URI for the resource is <a href=\"http://sakai3.alexandria.edu/site/physics101/page24/pagecontent\">http://sakai3.alexandria.edu/site/physics101/page24/pagecontent</a>\n"
      + "\n"
      + "  With comments for refinement: then a clearly articulated policy for when and when not to perform filtering needs to be agreed on, enforced and QA cycles reserved.  Further, I am not sure if there is more than one filtering level needed depending on content purpose. [Was tempted to set priority as Blocker] An example filtering  library is antSamy: <a href=\"http://www.owasp.org/index.php/Category:OWASP_AntiSamy_Project\">http://www.owasp.org/index.php/Category:OWASP_AntiSamy_Project</a>\n"
      + " ";

  private static final String[][] TESTS = new String[][] {
      new String[] { "<script>alert(\"gotya\");</script>", "" },
      new String[] { CLEAN, CLEANRESULT } };

  @Mock
  private ComponentContext componentContext;

  public AntiXssServiceImplTest() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCleanHtml() throws IOException, PolicyException {
    AntiXssServiceImpl antiXssServiceImpl = new AntiXssServiceImpl();

    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    properties.put(AntiXssServiceImpl.POLICY_FILE_LOCATION,
        "res://org/sakaiproject/nakamura/antixss/defaultpolicy.xml");
    Mockito.when(componentContext.getProperties()).thenReturn(properties);
    antiXssServiceImpl.activate(componentContext);
    for (String[] test : TESTS) {
      long start = System.currentTimeMillis();
      for (int i = 0; i < 1000; i++) {
        Assert.assertEquals(test[1], antiXssServiceImpl.cleanHtml(test[0]));
      }
      long end = System.currentTimeMillis();
      System.err.println("Test [" + test[1] + "] took " + (end - start) + " ns");
    }
  }
}
