/*
 * Copyright (c) Lightstreamer Srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package jms_demo_services.portfolio;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.naming.NamingException;

import jms_demo_services.JmsWrapper;

import org.apache.log4j.Logger;

public class PortfolioService implements MessageListener {
    
    private static final String PORTFOLIO_STATUS_REQUEST = "GET_PORTFOLIO_STATUS";
    private static final String BUY_REQUEST = "BUY";
    private static final String SELL_REQUEST = "SELL";
   
    private Logger _log;

    /**
     * The feed simulator.
     */
    private PortfolioFeedSimulator _feed;

    /**
     * This object handles comunications with JMS queue for
     * portfolio operations and status requests.
     */
    private JmsWrapper _portfolioQueueWrapper;
    
    /**
     * This object handles comunications with JMS topic for
     * portfolio publishing.
     */
    private JmsWrapper _portfolioTopicWrapper;

    /**
     * This object handles comunications with specific JMS clients
     * answering direct requests
     */
    private JmsWrapper _portfolioResponseWrapper;
    
    public PortfolioService(Logger log, String providerURL, String initialContextFactory, String connectionFactory, String topic, String queue, int portfolios) throws NamingException, JMSException{
        _log= log;
        
        // "Bind" to the feed simulator
        _feed = new PortfolioFeedSimulator(log);

        // Initialize JMSWrapper shared components
        JmsWrapper.init(_log, initialContextFactory, providerURL, connectionFactory);
        
        // Instantiate a JMSWrapper for queue
        _portfolioQueueWrapper = new JmsWrapper(queue, false);
        
        // This service will be the JMS listener
        _portfolioQueueWrapper.setMessageListener(this);

        // Instantiate a JMSWrapper for topic
        _portfolioTopicWrapper = new JmsWrapper(topic, true);
        
        // Instantiate a JMSWrapper for responses on temp queues
        _portfolioResponseWrapper = new JmsWrapper();

        // Start JMS
        _portfolioQueueWrapper.initConsumer();
        _portfolioTopicWrapper.initProducer();
        _portfolioResponseWrapper.initResponder();
        
        
        // Create the portfolio and attach a listener to them
        for (int i = 1; i<=portfolios; i++) {
            String portfolioId = "portfolio"+i;
            MyPortfolioListener listener = new MyPortfolioListener(portfolioId);
            Portfolio portfolio = _feed.getPortfolio(portfolioId);
            portfolio.setListener(listener);
        }
        
        _log.debug("Portfolio service ready");
    }
    
    
    ///////////////////////////////////////////////////////////////////////////
    // Message listener

    /**
     * Receive messages from JMSWrapper.
     */
    public void onMessage(Message message) {
        _log.debug("Portfolio: message received: processing...");
        
        
        if (message instanceof MapMessage) {
            String opMsg = null;
            
            final MapMessage mapMessage = (MapMessage) message;
            final String portfolioId;
            try {
                opMsg = mapMessage.getString("request");
                portfolioId = mapMessage.getString("portfolio");
            } catch (JMSException e) {
                _log.error("Portfolio: JMSException: " + e.getMessage());
                return;
            }
            
            _log.debug("Portfolio: message: request received: " + opMsg + " for " + portfolioId);
            Portfolio requestedPortfolio = _feed.getPortfolio(portfolioId);
            
            if (opMsg.equals(PORTFOLIO_STATUS_REQUEST)) {
                
                requestedPortfolio.flushToListener(new PortfolioListener() {
                    public void update(String stock, int qty) {
                        PortfolioMessage toSend = new PortfolioMessage(portfolioId, stock, String.valueOf(qty));
                        try {
                            _portfolioResponseWrapper.sendObjectResponse(toSend,mapMessage);
                        } catch (JMSException e) {
                            _log.error("Portfolio: unable to send message - JMSException:" + e.getMessage());
                        }
                    }
                });
                
            } else if (opMsg.equals(BUY_REQUEST) || opMsg.equals(SELL_REQUEST) ) {
                
                String stock;
                int qty;
                try {
                    stock = mapMessage.getString("stock");
                    qty = (int) mapMessage.getLong("quantity");
                } catch (JMSException e) {
                    _log.error("Portfolio: JMSException: " + e.getMessage());
                    return;
                }
                
                try {
                    if (opMsg.equals(BUY_REQUEST)) {
                        requestedPortfolio.buy(stock, qty);
                    } else {
                        requestedPortfolio.sell(stock, qty);
                    }
                } catch(IllegalArgumentException iae) {
                    _log.error("Portfolio: IllegalArgumentException during buy/sell: " + iae.getMessage());
                }
            }
            
        }
        
        
        
    }
    
    
    ///////////////////////////////////////////////////////////////////////////
    // Operations
    

    private void onUpdate(String portfolioId, String key, int qty) {
        
        // An update was received from the feed
        // Check for late calls
            
        // Prepare the object to send through JMS
        PortfolioMessage toSend = new PortfolioMessage(portfolioId, key, String.valueOf(qty));
        try {

            // Publish the update to JMS
            _portfolioTopicWrapper.sendObjectMessage(toSend);
            
        } catch (JMSException je) {
            _log.error("Portfolio: unable to send message - JMSException:" + je.getMessage());
        }
    
    }
    
    ///////////////////////////////////////////////////////////////////////////
    // Portfolio listener
    
    /**
     * Inner class that listens to a single Portfolio.
     */
    private class MyPortfolioListener implements PortfolioListener {

        // Id of the portfolio
        private String _portfolioId;

        public MyPortfolioListener(String portfolioId) {
            _portfolioId = portfolioId;
        }

        public void update(String stock, int qty) {
            // update quantity
            onUpdate(_portfolioId, stock, qty);
            
            _log.debug("Portfolio: " + _portfolioId + ": updated " + stock);
        }
    }
}
