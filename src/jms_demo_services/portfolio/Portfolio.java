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

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

/**
 * Manages the contents for a single portfolio.
 * The contents can be changed through "buy" and "sell" methods
 * and can be inquired through a listener; upon setting of a new listener,
 * the current contents are notified, followed by the notifications
 * of subsequent content changes.
 * To make it simple, a single listener is allowed at each time.
 * All methods are synchronized, but none can be blocking. The calls
 * to the listener are enqueued and send from a local thread; they may
 * occur just after "removeListener" has been issued.
 */
public class Portfolio {

    /**
     * Private logger; we lean on a creator supplied logger.
     */
    private Logger _log;

    /**
     * Single listener for the contents.
     */
    private PortfolioListener _listener;

    private final String _id;

    /**
     * Used to enqueue the calls to the listener.
     */
    private final ExecutorService _executor;

    /**
     * The portfolio contents; associates stock ids with quantities;
     * only stocks with positive quantities are included.
     */
    private final HashMap<String, Integer> _quantities = new HashMap<String, Integer>();

    public Portfolio(String id, Logger logger) {
        _id = id;
        _log = logger;
        
        // Create the executor for this instance;
        // the SingleThreadExecutor ensures a FIFO behaviour
        _executor = Executors.newSingleThreadExecutor();
    }

    public synchronized void buy(String stock, int qty) throws Exception {
        if (qty <= 0) {
        	
            // We can't buy 0 or less...
            _log.warn("Cannot buy " + qty + " " + stock + " for " + _id + " use an integer greater than 0");
            throw new Exception("Cannot buy " + qty + " " + stock + " for " + _id + " use an integer greater than 0");
        }

        _log.debug("Buying " + qty + " " + stock + " for " + _id);
        
        // Pass the quantity to add to the changeQty method
        changeQty(stock,qty);
    }

    public synchronized void sell(String stock, int qty) throws Exception {
        if (qty <= 0) {

        	// We can't sell 0 or less...
            _log.warn("Cannot sell " + qty + " " + stock + " for " + _id + " use an integer greater than 0");
            throw new Exception("Cannot sell " + qty + " " + stock + " for " + _id + " use an integer greater than 0");
        }

        _log.debug("Selling " + qty + " " + stock + " for " + _id);

        // Change the quantity sing and pass it to the changeQty method
        changeQty(stock,-qty);
    }

    private synchronized void changeQty(String stock, int qty) {
    	
        // Get the old quantity for the stock
        Integer oldQty = _quantities.get(stock);
        int newQty;
        if (oldQty == null) {
            
        	// If oldQty is null it means that we have not that stock on our portfolio
            if (qty <= 0) {
                
            	// We can't sell something we don't have, warn and return.
                _log.warn(_id+"|No stock to sell: " + stock);
                return;
            }
            
            // Set oldQty to 0 to let the listener know that we previously didn't have such stock
            oldQty = 0;
            
            // The new quantity is equal to the bought value
            newQty = qty;

        } else {
            assert(oldQty > 0);
            
            // The new quantity will be the value of the old quantity plus the qty value.
            // If qty is a negative number than we are selling, in the other case we're buying
            newQty = oldQty + qty;

            // Overflow check; just in case
            if (qty > 0 && newQty <= qty) {
                newQty = oldQty;
                _log.warn(_id+"|Quantity overflow; order ignored: " + stock);
                return;
            }
        }

        if (newQty < 0) {
        	
            // We sold more than we had
            _log.warn(_id+"|Not enough stock to sell: " + stock);
            
            // We interpret this as "sell everything"
            newQty = 0;
        }

        if (newQty == 0) {
        	
            // If we sold everything we remove the stock from the internal structure
            _quantities.remove(stock);
        
        } else {
        
        	// Save the actual quantity in internal structure
            _quantities.put(stock, newQty);
        }

        if (_listener != null) {
            
        	// Copy the actual listener to a constant that will be used inside the inner class
            final PortfolioListener localListener = _listener;
            
            // Copy the values to constant to be used inside the inner class
            final int newVal = newQty;
            final int oldVal = oldQty.intValue();
            final String stockId = stock;

            // If we have a listener create a new Runnable to be used as a task to pass the
            // new update to the listener
            Runnable updateTask = new Runnable() {
                public void run() {
                	
                    // Call the update on the listener;
                    // in case the listener has just been detached,
                    // the listener should detect the case
                    localListener.update(stockId, newVal, oldVal);
                }
            };

            // We add the task on the executor to pass to the listener the actual status
            _executor.execute(updateTask);
        }
    }

    public synchronized void setListener(PortfolioListener newListener) {
        if (newListener == null) {
            
        	// We don't accept a null parameter. to delete the actual listener
            // the removeListener method must be used
            return;
        }
        
        // Set the listener
        _listener = newListener;

        _log.debug("Listener set on " + _id);

        // Copy the actual listener to a final variable that will be used inside the inner class
        final PortfolioListener localListener = newListener;

        // Clone the actual status of the portfolio
        @SuppressWarnings("unchecked") 
        final HashMap<String, Integer> currentStatus = (HashMap<String, Integer>) _quantities.clone();

        // Create a new Runnable to be used as a task to pass the actual status to the listener
        Runnable statusTask = new Runnable() {
            public void run() {
                
            	// Call the onActualStatus on the listener;
                // in case the listener has just been detached,
                // the listener should detect the case
                localListener.onActualStatus(currentStatus);
            }
        };

        // We add the task on the executor to pass to the listener the actual status
        _executor.execute(statusTask);
    }

    public synchronized void removeListener() {
        
    	// Remove the listener
        _listener = null;
    }
}