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
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Responder {

  private static Logger log = LoggerFactory.getLogger(Responder.class);

  private final Session session;

  private final MessageProducer replyProducer;

  public Responder(Session session) {
    this.session = session;
    try {
      // We use a producer not bound to a destination: the request will carry
      // the information about the destination
      replyProducer = session.createProducer(null);
      log.debug("Responder created");
    } catch (Exception e) {
      log.error("Error while creating the Responder");
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends an object message.
   */
  public synchronized void sendObjectResponse(Serializable obj, Message request) {
    try {
      // Get a message
      ObjectMessage objMessage = session.createObjectMessage();

      // Fill it with obj (our message to be sent)
      objMessage.setObject(obj);

      // Correlate response with the request
      objMessage.setJMSCorrelationID(request.getJMSCorrelationID());

      log.debug("Sending response object " + obj);

      // Send to JMS
      replyProducer.send(request.getJMSReplyTo(), objMessage);
    } catch (JMSException e) {
      log.warn("Portfolio: unable to send message", e);
    }
  }


}
