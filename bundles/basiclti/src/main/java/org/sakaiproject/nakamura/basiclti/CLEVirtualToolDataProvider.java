/**
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
package org.sakaiproject.nakamura.basiclti;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants;
import org.sakaiproject.nakamura.api.basiclti.VirtualToolDataProvider;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

/**
 * Specific to CLE tools provided by LTI.
 */
@Service
@SlingServlet(methods = { "GET" }, generateService = true, paths = { "/var/basiclti/cletools" }, metatype = true)
public class CLEVirtualToolDataProvider extends SlingSafeMethodsServlet implements
    VirtualToolDataProvider {
  private static final long serialVersionUID = 1871722416187493177L;

  private static final Logger LOG = LoggerFactory
      .getLogger(CLEVirtualToolDataProvider.class);

  @Property(value = "http://localhost", name = "sakai.cle.server.url", description = "Base URL of Sakai CLE server; e.g. http://localhost.")
  protected static final String CLE_SERVER_URL = "sakai.cle.server.url";

  @Property(value = "12345", name = "sakai.cle.basiclti.key", description = "LTI key for launch.")
  protected static final String CLE_BASICLTI_KEY = "sakai.cle.basiclti.key";

  @Property(value = "secret", name = "sakai.cle.basiclti.secret", description = "LTI shared secret for launch.")
  protected static final String CLE_BASICLTI_SECRET = "sakai.cle.basiclti.secret";

  @Property(longValue = 100, name = "sakai.cle.basiclti.frame.height", description = "IFRAME height.")
  protected static final String CLE_BASICLTI_FRAME_HEIGHT = "sakai.cle.basiclti.frame.height";

  @Property(boolValue = true, name = "sakai.cle.basiclti.frame.height.lock", description = "Lock the IFRAME height.")
  protected static final String CLE_BASICLTI_FRAME_HEIGHT_LOCK = "sakai.cle.basiclti.frame.height.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.url.lock", description = "Lock the LTI URL.")
  protected static final String LTI_URL_LOCK = "sakai.cle.basiclti.url.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.key.lock", description = "Lock the LTI key.")
  protected static final String LTI_KEY_LOCK = "sakai.cle.basiclti.key.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.secret.lock", description = "Lock the LTI secret.")
  protected static final String LTI_SECRET_LOCK = "sakai.cle.basiclti.secret.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.names", description = "Will full names be released to LTI provider?")
  protected static final String LTI_RELEASE_NAMES = "sakai.cle.basiclti.release.names";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.names.lock", description = "Lock release names setting.")
  protected static final String LTI_RELEASE_NAMES_LOCK = "sakai.cle.basiclti.release.names.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.email", description = "Will email addresses be released to LTI provider?")
  protected static final String LTI_RELEASE_EMAIL = "sakai.cle.basiclti.release.email";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.email.lock", description = "Lock email address release setting.")
  protected static final String LTI_RELEASE_EMAIL_LOCK = "sakai.cle.basiclti.release.email.lock";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.principal", description = "Will the username be released to the LTI provider?")
  protected static final String LTI_RELEASE_PRINCIPAL = "sakai.cle.basiclti.release.principal";

  @Property(boolValue = true, name = "sakai.cle.basiclti.release.principal.lock", description = "Lock the username release setting.")
  protected static final String LTI_RELEASE_PRINCIPAL_LOCK = "sakai.cle.basiclti.release.principal.lock";

  @Property(boolValue = false, name = "sakai.cle.basiclti.debug", description = "Enable the LTI client debug mode.")
  protected static final String LTI_DEBUG = "sakai.cle.basiclti.debug";

  @Property(boolValue = true, name = "sakai.cle.basiclti.debug.lock", description = "Lock the debug mode setting.")
  protected static final String LTI_DEBUG_LOCK = "sakai.cle.basiclti.debug.lock";

  /**
   * please keep these in sync if you change them. sync is needed to work around The value
   * for annotation attribute Property.value must be an array initializer; i.e.: <a href=
   * "http://stackoverflow.com/questions/2065937/how-to-supply-value-to-an-annotation-from-a-constant-java"
   * >stackoverflow.com article</a>
   */
  protected static final String[] DEFAULT_TOOL_LIST = new String[] {
      "sakai.gradebook.gwt.rpc", "sakai.assignment.grades", "sakai.samigo",
      "sakai.schedule", "sakai.announcements", "sakai.postem", "sakai.profile2",
      "sakai.profile", "sakai.chat", "sakai.resources", "sakai.rwiki", "sakai.forums",
      "sakai.gradebook.tool", "sakai.mailbox", "sakai.singleuser", "sakai.messages",
      "sakai.site.roster", "sakai.news", "sakai.summary.calendar", "sakai.poll",
      "sakai.syllabus", "sakai.blogwow", "sakai.sitestats", "sakai.sections" };
  @Property(value = { "sakai.gradebook.gwt.rpc", "sakai.assignment.grades",
      "sakai.samigo", "sakai.schedule", "sakai.announcements", "sakai.postem",
      "sakai.profile2", "sakai.profile", "sakai.chat", "sakai.resources", "sakai.rwiki",
      "sakai.forums", "sakai.gradebook.tool", "sakai.mailbox", "sakai.singleuser",
      "sakai.messages", "sakai.site.roster", "sakai.news", "sakai.summary.calendar",
      "sakai.poll", "sakai.syllabus", "sakai.blogwow", "sakai.sitestats",
      "sakai.sections" }, name = "sakai.cle.basiclti.tool.list", description = "")
  protected static final String TOOL_LIST = "sakai.cle.basiclti.tool.list";

  protected String cleUrl = "http://localhost";
  protected String ltiKey = "12345";
  protected String ltiSecret = "secret";
  protected Long frameHeight = 100L;
  protected boolean frameHeightLock = true;
  protected boolean urlLock = true;
  protected boolean keyLock = true;
  protected boolean secretLock = true;
  protected boolean releaseNames = true;
  protected boolean releaseNamesLock = true;
  protected boolean releaseEmail = true;
  protected boolean releaseEmailLock = true;
  protected boolean releasePrincipal = true;
  protected boolean releasePrincipalLock = true;
  protected boolean debug = false;
  protected boolean debugLock = true;
  protected List<String> toolList = Arrays.asList(DEFAULT_TOOL_LIST);

  @Activate
  protected void activate(ComponentContext componentContext) {
    LOG.debug("activate(ComponentContext componentContext)");
    final Dictionary<?, ?> properties = componentContext.getProperties();
    cleUrl = PropertiesUtil.toString(properties.get(CLE_SERVER_URL), cleUrl);
    ltiKey = PropertiesUtil.toString(properties.get(CLE_BASICLTI_KEY), ltiKey);
    ltiSecret = PropertiesUtil.toString(properties.get(CLE_BASICLTI_SECRET), ltiSecret);
    frameHeight = PropertiesUtil.toLong(properties.get(CLE_BASICLTI_FRAME_HEIGHT),
        frameHeight);
    frameHeightLock = PropertiesUtil.toBoolean(
        properties.get(CLE_BASICLTI_FRAME_HEIGHT_LOCK), frameHeightLock);
    urlLock = PropertiesUtil.toBoolean(properties.get(LTI_URL_LOCK), urlLock);
    keyLock = PropertiesUtil.toBoolean(properties.get(LTI_KEY_LOCK), keyLock);
    secretLock = PropertiesUtil.toBoolean(properties.get(LTI_SECRET_LOCK), secretLock);
    releaseNames = PropertiesUtil.toBoolean(properties.get(LTI_RELEASE_NAMES),
        releaseNames);
    releaseNamesLock = PropertiesUtil.toBoolean(properties.get(LTI_RELEASE_NAMES_LOCK),
        releaseNamesLock);
    releaseEmail = PropertiesUtil.toBoolean(properties.get(LTI_RELEASE_EMAIL),
        releaseEmail);
    releaseEmailLock = PropertiesUtil.toBoolean(properties.get(LTI_RELEASE_EMAIL_LOCK),
        releaseEmailLock);
    releasePrincipal = PropertiesUtil.toBoolean(properties.get(LTI_RELEASE_PRINCIPAL),
        releasePrincipal);
    releasePrincipalLock = PropertiesUtil.toBoolean(
        properties.get(LTI_RELEASE_PRINCIPAL_LOCK), releasePrincipalLock);
    debug = PropertiesUtil.toBoolean(properties.get(LTI_DEBUG), debug);
    debugLock = PropertiesUtil.toBoolean(properties.get(LTI_DEBUG_LOCK), debugLock);
    toolList = new ArrayList<String>(Arrays.asList(PropertiesUtil.toStringArray(
        properties.get(TOOL_LIST), DEFAULT_TOOL_LIST)));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.basiclti.VirtualToolDataProvider#getLaunchValues(java.lang.String)
   */
  public Map<String, Object> getLaunchValues(String virtualToolId) {
    LOG.debug("getLaunchValues(String {})", virtualToolId);
    if (virtualToolId == null || "".equals(virtualToolId)) {
      throw new IllegalArgumentException("Illegal String virtualToolId");
    }
    Map<String, Object> launchValues = null;
    if (toolList.contains(virtualToolId)) {
      launchValues = new HashMap<String, Object>();
      launchValues.put(BasicLTIAppConstants.LTI_URL, cleUrl + "/imsblti/provider/"
          + virtualToolId);
      launchValues.put(BasicLTIAppConstants.FRAME_HEIGHT, frameHeight);
      launchValues.put(BasicLTIAppConstants.FRAME_HEIGHT_LOCK, frameHeightLock);
      launchValues.put(BasicLTIAppConstants.LTI_URL_LOCK, urlLock);
      launchValues.put(BasicLTIAppConstants.LTI_KEY_LOCK, keyLock);
      launchValues.put(BasicLTIAppConstants.LTI_SECRET_LOCK, secretLock);
      launchValues.put(BasicLTIAppConstants.RELEASE_NAMES, releaseNames);
      launchValues.put(BasicLTIAppConstants.RELEASE_NAMES_LOCK, releaseNamesLock);
      launchValues.put(BasicLTIAppConstants.RELEASE_EMAIL, releaseEmail);
      launchValues.put(BasicLTIAppConstants.RELEASE_EMAIL_LOCK, releaseEmailLock);
      launchValues.put(BasicLTIAppConstants.RELEASE_PRINCIPAL_NAME, releasePrincipal);
      launchValues.put(BasicLTIAppConstants.RELEASE_PRINCIPAL_NAME_LOCK,
          releasePrincipalLock);
      launchValues.put(BasicLTIAppConstants.DEBUG, debug);
      launchValues.put(BasicLTIAppConstants.DEBUG_LOCK, debugLock);
    }
    return launchValues;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.basiclti.VirtualToolDataProvider#getKeySecret(java.lang.String)
   */
  public Map<String, Object> getKeySecret(String virtualToolId) {
    LOG.debug("getKeySecret(String {})", virtualToolId);
    if (virtualToolId == null || "".equals(virtualToolId)) {
      throw new IllegalArgumentException("Illegal String virtualToolId");
    }
    Map<String, Object> adminValues = null;
    if (toolList.contains(virtualToolId)) {
      adminValues = new HashMap<String, Object>(2);
      adminValues.put(BasicLTIAppConstants.LTI_KEY, ltiKey);
      adminValues.put(BasicLTIAppConstants.LTI_SECRET, ltiSecret);
    }
    return adminValues;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.basiclti.VirtualToolDataProvider#getSupportedVirtualToolIds()
   */
  public List<String> getSupportedVirtualToolIds() {
    LOG.debug("getSupportedVirtualToolIds()");
    if (toolList == null) {
      return Collections.emptyList();
    } else {
      return toolList;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cleUrl == null) ? 0 : cleUrl.hashCode());
    result = prime * result + ((ltiKey == null) ? 0 : ltiKey.hashCode());
    result = prime * result + ((ltiSecret == null) ? 0 : ltiSecret.hashCode());
    result = prime * result + ((toolList == null) ? 0 : toolList.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof CLEVirtualToolDataProvider)) {
      return false;
    }
    CLEVirtualToolDataProvider other = (CLEVirtualToolDataProvider) obj;
    if (cleUrl == null) {
      if (other.cleUrl != null) {
        return false;
      }
    } else if (!cleUrl.equals(other.cleUrl)) {
      return false;
    }
    if (ltiKey == null) {
      if (other.ltiKey != null) {
        return false;
      }
    } else if (!ltiKey.equals(other.ltiKey)) {
      return false;
    }
    if (ltiSecret == null) {
      if (other.ltiSecret != null) {
        return false;
      }
    } else if (!ltiSecret.equals(other.ltiSecret)) {
      return false;
    }
    if (toolList == null) {
      if (other.toolList != null) {
        return false;
      }
    } else if (!toolList.equals(other.toolList)) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "CLEVirtualToolDataProvider [toolList=" + toolList + ", cleUrl=" + cleUrl
        + "]";
  }

  /**
   * Provides a very simple REST endpoint that returns the Sakai CLE tool ids that are
   * available for use by the Sakai CLE Tools widget.
   * 
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(final SlingHttpServletRequest request,
      final SlingHttpServletResponse response) throws ServletException, IOException {
    LOG.debug("doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)");
    final ExtendedJSONWriter ejw = new ExtendedJSONWriter(response.getWriter());
    try {
      ejw.object();
      ejw.key("toolList").value(getSupportedVirtualToolIds().toArray());
      ejw.endObject();
    } catch (JSONException e) {
      LOG.error(e.getLocalizedMessage(), e);
      response.sendError(500, e.getLocalizedMessage());
    }
  }

}
