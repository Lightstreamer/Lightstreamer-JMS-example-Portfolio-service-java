/*
 * Copyright 2014 Weswit Srl
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import jms_demo_services.JmsWrapper;

import org.apache.log4j.Logger;

public class PortfolioService implements MessageListener {
    private static final String ERROR_MSG_NOT_COMPATIBLE = "message received was not compatible with this process. Maybe someone else sending messages? ";

    private Logger _log;

    /**
     * A map containing every active subscriptions;
     * It associates each item name with the item handle to be used
     * to identify the item towards Lightstreamer Kernel.
     */
    private final Set<String> _subscriptions = new ConcurrentSkipListSet<String>();

    /**
     * The feed simulator.
     */
    private PortfolioFeedSimulator _feed;

    /**
     * This object handles comunications with JMS queue for
     * portfolio operations.
     */
    private JmsWrapper _portfolioQueueWrapper;
    
    /**
     * This object handles comunications with JMS topic for
     * portfolio publishing.
     */
    private JmsWrapper _portfolioTopicWrapper;
	
	public PortfolioService(Logger log, String providerURL, String initialContextFactory, String connectionFactory, String topic, String queue) throws NamingException, JMSException{
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

        // Start JMS
        _portfolioQueueWrapper.initConsumer();
        _portfolioTopicWrapper.initProducer();

        _log.debug("Portfolio service ready");
	}
	
	
    ///////////////////////////////////////////////////////////////////////////
    // Message listener

    /**
     * Receive messages from JMSWrapper.
     */
    public void onMessage(Message message) {
        String opMsg = null;
        
        _log.debug("Portfolio: message received: processing...");
        
        try {

        	// Pull out text from the Message object
            TextMessage textMessage = (TextMessage) message;
            opMsg = textMessage.getText();
            _log.debug("Portfolio: message: TextMessage received: " + opMsg);
        
        } catch (ClassCastException cce) {

        	// If message isn't a TextMessage then this update is not "correct"
            _log.warn("Portfolio: " + ERROR_MSG_NOT_COMPATIBLE + "(ClassCastException)");
            return;
        
        } catch (JMSException jmse) {
            _log.error("Portfolio: JMSException: " + jmse.getMessage());
            return;
        }
        
        String[] operation = opMsg.split("\\|");
        
        switch (operation.length) {
        	case 2: {
                try {
                    if (operation[0].equals("SUBSCRIBE")) {
                    	
                        // Subscribe the specified portfolio
                    	subscribe(operation[1]);
                        
                    } else if (operation[0].equals("UNSUBSCRIBE")) {
                        
                        // Unsubscribe the specified portfolio
                    	unsubscribe(operation[1]);
                    }
                    
                } catch (Exception e) {
                	_log.error("Portfolio: exception: " + e.getMessage(), e);
                }
        		
        		
        		break;
        	}
        		
        	case 4: {
                int qty= 0;
                try {
                	
                    // Parse the received quantity to be an integer
                    qty = Integer.parseInt(operation[3]);
                    
                } catch (NumberFormatException e) {
                	_log.warn("Portfolio: wrong message received (quantity must be an integer number): " + opMsg);
                }

                if (qty <= 0) {
                    
                	// Quantity can't be a negative number or 0; just ignore
                	_log.warn("Portfolio: wrong message received (quantity must be greater than 0): " + opMsg);
                    return;
                }

                // Get the needed portfolio
                Portfolio portfolio = _feed.getPortfolio(operation[1]);
                if (portfolio == null) {
                	
                    // Since the feed creates a new portfolio if no one is available for
                    // an id, this will never occur
                	_log.error("Portfolio: no such portfolio: " + operation[1]);
                }
                
                try {
                    if (operation[0].equals("BUY")) {
                    	
                        // Call the buy operation on the selected portfolio
                        portfolio.buy(operation[2], qty);
                        
                    } else if (operation[0].equals("SELL")) {
                        
                    	// Call the sell operation on the selected portfolio
                        portfolio.sell(operation[2], qty);
                    }
                    
                } catch (Exception e) {
                	_log.error("Portfolio: exception: " + e.getMessage(), e);
                }
        		break;
        	}
        	
        	default:
        		_log.warn("Portfolio: wrong message received: " + opMsg);
        		break;
        }
    }
    
    
    ///////////////////////////////////////////////////////////////////////////
    // Operations
    
    public void subscribe(String portfolioId) {
        Portfolio portfolio = _feed.getPortfolio(portfolioId);
        if (portfolio == null)
        	_log.error("No such portfolio: " + portfolioId);

        // Add the new item to the list of subscribed items
        _subscriptions.add(portfolioId);

        // Create a new listener for the portfolio
        MyPortfolioListener listener = new MyPortfolioListener(portfolioId);

        // Set the listener on the feed
        portfolio.setListener(listener);

        _log.info("Portfolio: " + portfolioId + " subscribed");
    }

    public void unsubscribe(String portfolioId) {
        Portfolio portfolio = _feed.getPortfolio(portfolioId);
        if (portfolio != null) {
            
        	// Remove the listener from the feed to not receive new
            // updates
            portfolio.removeListener();
        }
        
        // Remove the handle from the list of subscribed items
        _subscriptions.remove(portfolioId);

        _log.info("Portfolio: " + portfolioId + " unsubscribed");
    }
    
    private final boolean isSubscribed(String portfolioId) {
    	
        // Just check if a given handle is in the map of subscribed items
        return _subscriptions.contains(portfolioId);
    }

    private void onUpdate(String portfolioId, String key, int qty) {
    	
        // An update was received from the feed
        // Check for late calls
        if (isSubscribed(portfolioId)) {
        	
            // Prepare the object to send through JMS
            PortfolioMessage toSend = new PortfolioMessage(portfolioId, key, "UPDATE", String.valueOf(qty));
            try {

            	// Publish the update to JMS
                _portfolioTopicWrapper.sendObjectMessage(toSend);
                
            } catch (JMSException je) {
                _log.error("Portfolio: unable to send message - JMSException:" + je.getMessage());
            }
        }
    }

    private void onDelete(String portfolioId, String key) {
    	
        // An update was received from the feed
        // Check for late calls
        if (isSubscribed(portfolioId)) {
            
            // Prepare the object to send through JMS
            PortfolioMessage toSend = new PortfolioMessage(portfolioId, key, "DELETE", null);
            try {

            	// Publish the update to JMS
                _portfolioTopicWrapper.sendObjectMessage(toSend);
                
            } catch (JMSException je) {
                _log.error("Portfolio: unable to send message - JMSException:" + je.getMessage());
            }
        }
    }

    private void onAdd(String portfolioId, String key, int qty, boolean snapshot) {
    	
        // An update for a new stock was received from the feed or the snapshot was read
        // Check for late calls
        if (isSubscribed(portfolioId)) {
            
            // Prepare the object to send through JMS
            PortfolioMessage toSend = new PortfolioMessage(portfolioId, key, "ADD", String.valueOf(qty));
            try {

            	// Publish the update to JMS
                _portfolioTopicWrapper.sendObjectMessage(toSend);
                
            } catch (JMSException je) {
                _log.error("Portfolio: unable to send message - JMSException:" + je.getMessage());
            }
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

        public void update(String stock, int qty, int oldQty) {
        	
            // An update was received from the feed
            if (qty <= 0) {
                
            	// If qty is 0 or less we have to delete the "row"
                onDelete(_portfolioId, stock);
                
                _log.debug("Portfolio: " + _portfolioId + ": deleted " + stock);

            } else if (oldQty == 0) {
                
            	// If oldQty value is 0 then this is a new stock
                // in the portfolio so that we have to add a "row"
                onAdd(_portfolioId, stock, qty, false);
                
                _log.debug("Portfolio: " + _portfolioId + ": added " + stock);

            } else {
                
            	// A simple update
                onUpdate(_portfolioId, stock, qty);
                
                _log.debug("Portfolio: " + _portfolioId + ": updated " + stock);
            }
        }

        public void onActualStatus(Map<String, Integer> currentStatus) {
            Set<String> keys = currentStatus.keySet();
            
            // Iterates through the Hash representing the actual status to send
            // the snapshot to the client
            for (String key : keys)
                onAdd(_portfolioId, key, currentStatus.get(key).intValue(), true);
            
            _log.info("Portfolio: " + _portfolioId + ": snapshot sent");
        }
    }
}
