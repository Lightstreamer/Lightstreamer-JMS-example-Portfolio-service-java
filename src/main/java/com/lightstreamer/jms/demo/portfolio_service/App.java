/*
 * Copyright (c) Lightstreamer Srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.lightstreamer.jms.demo.portfolio_service;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lightstreamer.jms.demo.portfolio_service.config.Configuration;

public class App {

  private static Logger log = LoggerFactory.getLogger(App.class);

  private static final String DEFAULT_BROKER = "ActiveMQ";

  private static final String broker = System.getProperty("broker", DEFAULT_BROKER);

  public static void main(String[] args) {
    try (InputStream is = App.class.getResourceAsStream("/service.conf")) {
      log.info("Portfolio Demo service starting. Loading configuration...");

      Properties props = new Properties();
      props.load(is);

      // Read parameters
      Configuration config = new Configuration.Builder().withJmsURL(getProperty(props, "jmsUrl"))
        .withInitialContextFactory(getProperty(props, "initialContextFactory"))
        .withConnectionFactoryName(props.getProperty("connectionFactoryName"))
        .withTopicName(props.getProperty("topicName"))
        .withQueueName(props.getProperty("queueName"))
        .withPortfolioNum(props.getProperty("portfolioNum"))
        .withCredentials(getProperty(props, "user"), getProperty(props, "password"))
        .build();

      // Create and start our service passing the supplied configuration
      new PortfolioService(config).start();

      log.info("Portfolio Demo service ready.");
    } catch (Exception e) {
      if (log != null) {
        log.error("Exception caught while starting Portfolio Demo service: " + e.getMessage(), e);
      }
    }
  }

  private static String getProperty(Properties props, String key) {
    return props.getProperty(broker + "." + key);
  }
}
