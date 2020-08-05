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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lightstreamer.jms.demo.portfolio_service.config.Configuration;
import com.lightstreamer.jms.demo.portfolio_service.message.PortfolioMessage;

/**
 * @author Gialuca Finocchiaro
 *
 */
public class PortfolioService implements MessageListener {

  private static final String PORTFOLIO_STATUS_REQUEST = "GET_PORTFOLIO_STATUS";

  private static final String BUY_REQUEST = "BUY";

  private static final String SELL_REQUEST = "SELL";

  private static Logger log = LoggerFactory.getLogger(PortfolioService.class);

  /**
   * The feed simulator.
   */
  private final PortfolioFeedSimulator feed;

  /**
   * This object handles communications with JMS topic for portfolio publishing.
   */
  private final TopicSender portfolioTopicSender;

  /**
   * This object handles communications with specific JMS clients answering direct requests
   */
  private final Responder portfolioCurrentStatus;

  /**
   * The number of portfolios to handle.
   */
  private final int portfolioNum;

  public PortfolioService(Configuration config) {
    this.portfolioNum = config.portfolioNum;

    // "Bind" to the feed simulator
    feed = new PortfolioFeedSimulator();

    InitialContext jndiContext = config.newInitialContext();
    Session consumingSession = newSession(jndiContext, config);

    // Create Queue consumer for handling with JMS queue for portfolio operations and status
    // requests.
    newConsumer(jndiContext, consumingSession, config.queueName, this);

    // Here we create a new Session, as some broker (e.g, IBM_MQ), does not
    // support the use of synchronous operations on a session which has already
    // been used for asynchronous operations
    Session publishlingSession = newSession(jndiContext, config);

    // Instantiate the Topic sender
    portfolioTopicSender = new TopicSender(jndiContext, publishlingSession, config.topicName);

    // Instantiate the Responder for responses on temp queues
    portfolioCurrentStatus = new Responder(publishlingSession);
  }


  public void start() {
    // Create the portfolios and attach a listener to them
    for (int i = 1; i <= portfolioNum; i++) {
      String portfolioId = String.format("portfolio%d", i);
      MyPortfolioListener listener = new MyPortfolioListener(portfolioId);
      Portfolio portfolio = feed.getPortfolio(portfolioId);
      portfolio.setListener(listener);
    }
    log.debug("Portfolio service ready");
  }

  /**
   * Receive messages from the Queue consumer.
   */
  @Override
  public void onMessage(Message message) {
    log.debug("Portfolio: message received: processing...");

    if (!(message instanceof MapMessage)) {
      log.warn("Portfolio: not a MapMessage");
      return;
    }

    try {
      MapMessage mapMessage = (MapMessage) message;
      String opMsg = mapMessage.getString("request");
      String portfolioId = mapMessage.getString("portfolio");
      log.debug("Portfolio: message: request received: {} for {} ", opMsg, portfolioId);

      Portfolio requestedPortfolio = feed.getPortfolio(portfolioId);

      switch (opMsg) {
        case PORTFOLIO_STATUS_REQUEST:
          requestedPortfolio.flushToListener((String stock, long qty) -> {
            PortfolioMessage toSend = new PortfolioMessage(portfolioId, stock, qty);
            portfolioCurrentStatus.sendObjectResponse(toSend, mapMessage);
          });
          break;

        case BUY_REQUEST:
        case SELL_REQUEST:
          String stock = mapMessage.getString("stock");
          long qty = mapMessage.getLong("quantity");
          try {
            if (opMsg.equals(BUY_REQUEST)) {
              requestedPortfolio.buy(stock, qty);
            } else {
              requestedPortfolio.sell(stock, qty);
            }
          } catch (IllegalArgumentException iae) {
            log.warn("Portfolio: IllegalArgumentException during buy/sell:", iae);
          }

        default:
          break;
      }
    } catch (JMSException e) {
      log.warn("Portfolio: JMSException", e);
    }
  }


  /**
   * Manages update received from the feed.
   */
  private void onUpdate(String portfolioId, String key, long qty) {
    // Prepare the object to send through JMS
    PortfolioMessage toSend = new PortfolioMessage(portfolioId, key, qty);

    // Publish the update to JMS
    portfolioTopicSender.sendObjectMessage(toSend);
  }

  /**
   * Inner class that listens to a single Portfolio.
   */
  private class MyPortfolioListener implements PortfolioListener {

    // Id of the portfolio
    private String portfolioId;

    public MyPortfolioListener(String portfolioId) {
      this.portfolioId = portfolioId;
    }

    @Override
    public void update(String stock, long qty) {
      // Update the quantity
      onUpdate(portfolioId, stock, qty);
      log.debug("Portfolio: " + portfolioId + ": updated " + stock);
    }
  }

  /**
   * Creates a new JMS Session
   */
  private static Session newSession(InitialContext jndiContext, Configuration config) {
    try {
      log.info("Looking up queue connection factory [{}]...", config.connectionFactoryName);
      ConnectionFactory connectionFactory =
          (ConnectionFactory) jndiContext.lookup(config.connectionFactoryName);

      // Get the Connection from our ConnectionFactory
      Connection connection = null;
      if (config.username != null && config.password != null) {
        connection = connectionFactory.createConnection(config.username, config.password);
      } else {
        connection = connectionFactory.createConnection();
      }
      log.debug("Connection created");

      // Start listening to JMS
      connection.start();
      log.debug("Connection started");

      // Get the Session from our Connection
      Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      log.debug("Session created");

      return session;
    } catch (Exception e) {
      log.error("Error while preparing the JMS Session");
      throw new RuntimeException(e);
    }
  }


  /**
   * Creates a new JMS Queue consumer.
   */
  private static MessageConsumer newConsumer(InitialContext jndiContext, Session session,
      String queueName, MessageListener listener) {

    // Find our destination
    log.info("Looking up queue [{}]...", queueName);
    try {
      Queue destination;
      try {
        destination = (Queue) jndiContext.lookup(queueName);
      } catch (NamingException e) {
        // In case of dynamic destinations
        destination = session.createQueue(queueName);
      }

      // Get the MessageConsumer from our Session and set the listener
      MessageConsumer consumer = session.createConsumer(destination);
      consumer.setMessageListener(listener);

      log.debug("QueueConsumer created");
      return consumer;
    } catch (Exception e) {
      log.error("Error while creating the QueueConsumer");
      throw new RuntimeException(e);
    }
  }
}
