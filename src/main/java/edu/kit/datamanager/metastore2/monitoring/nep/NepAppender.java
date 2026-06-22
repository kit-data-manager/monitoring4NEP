/*
 * Copyright 2021 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.metastore2.monitoring.nep;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.clients.SimpleServiceClient;
import edu.kit.datamanager.metastore.monitoring.nep.json.Payload;
import edu.kit.datamanager.metastore.monitoring.nep.json.VirtualAccessCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Appender logging access to NEP service.
 *
 */
public class NepAppender extends AppenderBase<ILoggingEvent> {

  /**
   * Create logger for class.
   */
  private static final Logger logger = LoggerFactory.getLogger(NepAppender.class);

  /**
   * URL for accessing monitoring service.
   */
  private String nepServiceUrl;
  /**
   * Service ID of the service sending the log.
   */
  private String virtualServiceId;

  private static final ObjectMapper objectMapper = new ObjectMapper();
  // Creating Object of ObjectMapper define in Jakson Api

  @Override
  protected void append(ILoggingEvent e) {
    // Test configuration
    if (nepServiceUrl == null || nepServiceUrl.trim().isEmpty()) {
      addError("nepServiceUrl is not set for NepAppender.");
      return;
    }

    if (virtualServiceId == null || virtualServiceId.trim().isEmpty()) {
      addError("virtualServiceId is not set for NepAppender.");
      return;
    }

    // Set input for REST endpoint of monitoring service.
    VirtualAccessCreate vac = new VirtualAccessCreate();
    vac.setVirtualServiceId(virtualServiceId);
    // Remove following line if payload is optional
    vac.setPayload(new Payload());

    String virtualAccessCreateAsString = null;
    try {
      virtualAccessCreateAsString = NepAppender.objectMapper.writeValueAsString(vac);
    } catch (JsonProcessingException ex) {
      logger.error("Error serializing VirtualAccessCreate!", ex);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("NEP Appender: " + nepServiceUrl);
      logger.trace("VirtualServiceCreate: " + virtualAccessCreateAsString);
      logger.trace("Message: " + e.getMessage());
      logger.trace("Event: " + e);
      for (Object arg : e.getArgumentArray()) {
        logger.trace("Other: " + arg);
      }
    }
    try {
      // Set bearer token if available
      String bearerToken = (e.getArgumentArray()[3] != null) ? e.getArgumentArray()[3].toString() : null;

      // Post input to nepServiceUrl
      SimpleServiceClient ssc = SimpleServiceClient.create(nepServiceUrl);
      if (bearerToken != null) {
        ssc.withBearerToken(bearerToken);
      }
      VirtualAccessCreate postResource = ssc.postResource(vac, VirtualAccessCreate.class);
      if (logger.isTraceEnabled()) {
        virtualAccessCreateAsString = NepAppender.objectMapper.writeValueAsString(postResource);
        logger.trace("Return value: '{}'", virtualAccessCreateAsString);
      }
    } catch (Throwable tw) {
      logger.error("Error posting monitoring information!", tw);
    }
  }

  /**
   * Gets the service URL for NEP.
   *
   * @return the nepServiceUrl
   */
  public String getNepServiceUrl() {
    return nepServiceUrl;
  }

  /**
   * Sets the service URL for NEP.
   *
   * @param nepServiceUrl the nepServiceUrl to set
   */
  public void setNepServiceUrl(String nepServiceUrl) {
    this.nepServiceUrl = nepServiceUrl;
  }

  /**
   * Gets the virtual servide ID.
   *
   * @return the virtualServiceId
   */
  public String getVirtualServiceId() {
    return virtualServiceId;
  }

  /**
   * Sets the virtual service ID.
   *
   * @param virtualServiceId the virtualServiceId to set
   */
  public void setVirtualServiceId(String virtualServiceId) {
    this.virtualServiceId = virtualServiceId;
  }

}
