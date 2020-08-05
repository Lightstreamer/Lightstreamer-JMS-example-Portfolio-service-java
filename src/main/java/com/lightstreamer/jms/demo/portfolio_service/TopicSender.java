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

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicSender {

  private static Logger log = LoggerFactory.getLogger(TopicSender.class);

  private final Session session;

  private final MessageProducer producer;

  public TopicSender(InitialContext jndiContext, Session session, String topicName) {
    this.session = session;

    // Find our destination
    log.info("Looking up topic [{}]...", topicName);
    try {
      Topic destination;
      try {
        destination = (Topic) jndiContext.lookup(topicName);
      } catch (NamingException ne) {
        // In case of dynamic destinations
        destination = session.createTopic(topicName);
      }

      // Get the MessageProducer from our Session
      producer = session.createProducer(destination);
      log.debug("TopicSender created");
    } catch (Exception e) {
      log.error("Error while creating the TopicSender");
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends an object message.
   */
  public synchronized void sendObjectMessage(Serializable obj) {
    try {
      // Get a message
      ObjectMessage objMessage = session.createObjectMessage();

      // Fill it with obj (our message to be sent)
      objMessage.setObject(obj);

      log.debug("Sending message object {}", obj);

      // Send to JMS
      producer.send(objMessage);
    } catch (JMSException e) {
      log.warn("Portfolio: unable to send message", e);
    }
  }

}
