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
package com.lightstreamer.jms.demo.portfolio_service.config;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

  private static Logger log = LoggerFactory.getLogger(Configuration.class);

  public final String initialContextFactory;

  public final String jmsUrl;

  public final String connectionFactoryName;

  public final String topicName;

  public final String queueName;

  public final int portfolioNum;

  public final String username;

  public final String password;

  private Configuration(Builder builder) {
    this.initialContextFactory = builder.initialContextFactory;
    this.jmsUrl = builder.jmsUrl;
    this.connectionFactoryName = builder.connectionFactoryName;
    this.topicName = builder.topicName;
    this.queueName = builder.queueName;
    this.portfolioNum = builder.portfolioNum;
    this.username = builder.username;
    this.password = builder.password;
  }

  public InitialContext newInitialContext() {
    try {
      // Prepare a Properties object to be passed to the InitialContext
      // constructor giving the InitialContextFactory name and the JMS server url
      Properties properties = new Properties();
      properties.put(Context.INITIAL_CONTEXT_FACTORY, initialContextFactory);
      properties.put(Context.PROVIDER_URL, jmsUrl);

      InitialContext jndiContext = new InitialContext(properties);
      log.info("JNDI Context[{}]...", jndiContext.getEnvironment());
      return jndiContext;
    } catch (NamingException e) {
      log.error("Error while preparing the JMS Session");
      throw new RuntimeException(e);
    }
  }

  public static class Builder {

    private String initialContextFactory;

    private String jmsUrl;

    private String connectionFactoryName;

    private String topicName;

    private String queueName;

    private int portfolioNum;

    private String username;

    private String password;

    public Builder withJmsURL(String jmsUrl) {
      Objects.requireNonNull(jmsUrl, "Please provide the <jmsUrl> entry");
      this.jmsUrl = jmsUrl;
      return this;
    }

    public Builder withInitialContextFactory(String initialContextFactory) {
      Objects.requireNonNull(initialContextFactory,
          "Please provide the <initialContextFactory> entry");
      this.initialContextFactory = initialContextFactory;
      return this;
    }

    public Builder withConnectionFactoryName(String connectionFactoryName) {
      Objects.requireNonNull(connectionFactoryName,
          "Please provide the <connectionFactoryName> entry");
      this.connectionFactoryName = connectionFactoryName;
      return this;
    }

    public Builder withTopicName(String topicName) {
      Objects.requireNonNull(topicName, "Please provide the <topicName> entry");
      this.topicName = topicName;
      return this;
    }

    public Builder withQueueName(String queueName) {
      Objects.requireNonNull(queueName, "Please provide the <queueName> entry");
      this.queueName = queueName;
      return this;
    }

    public Builder withPortfolioNum(String portfolioNum) {
      this.portfolioNum = Optional.ofNullable(portfolioNum)
        .map(Integer::parseInt)
        .orElse(1);
      return this;
    }

    public Builder withCredentials(String username, String password) {
      this.username = username;
      this.password = password;
      return this;
    }

    public Configuration build() {
      return new Configuration(this);
    }

  }

}
