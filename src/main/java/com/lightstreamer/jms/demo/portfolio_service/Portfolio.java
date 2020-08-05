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

import java.util.HashMap;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the contents for a single portfolio. The contents can be changed through "buy" and "sell"
 * methods and can be inquired through a listener; upon setting of a new listener, the current
 * contents are notified, followed by the notifications of subsequent content changes. To make it
 * simple, a single listener is allowed at each time. All methods are synchronized, but none can be
 * blocking. The calls to the listener are enqueued and send from a local thread; they may occur
 * just after "removeListener" has been issued.
 */
public class Portfolio {

  private Logger log = LoggerFactory.getLogger(Portfolio.class);

  /**
   * Single listener for the contents.
   */
  private PortfolioListener listener;

  private final String id;

  /**
   * Used to enqueue the calls to the listener.
   */
  private final ExecutorService executor;

  /**
   * The portfolio contents; associates stock ids with quantities; only stocks with positive
   * quantities are included.
   */
  private final HashMap<String, Long> quantities = new HashMap<>();

  public Portfolio(String id) {
    this.id = id;

    // Create the executor for this instance. The SingleThreadExecutor ensures a FIFO behaviour
    executor = Executors.newSingleThreadExecutor();
  }

  public synchronized void buy(String stock, long qty) {
    if (qty <= 0) {

      // We can't buy 0 or less...
      log.warn("Cannot buy " + qty + " " + stock + " for " + id + " use an integer greater than 0");
      throw new IllegalArgumentException(
          "Cannot buy " + qty + " " + stock + " for " + id + " use an integer greater than 0");
    }

    log.debug("Buying " + qty + " " + stock + " for " + id);

    // Pass the quantity to add to the changeQty method
    changeQty(stock, qty);
  }

  public synchronized void sell(String stock, long qty) {
    if (qty <= 0) {

      // We can't sell 0 or less...
      log.warn(
          "Cannot sell " + qty + " " + stock + " for " + id + " use an integer greater than 0");
      throw new IllegalArgumentException(
          "Cannot sell " + qty + " " + stock + " for " + id + " use an integer greater than 0");
    }

    log.debug("Selling " + qty + " " + stock + " for " + id);

    // Change the quantity sing and pass it to the changeQty method
    changeQty(stock, -qty);
  }

  private synchronized void changeQty(String stock, long qty) {
    if (!isValidStock(stock)) {
      log.warn(stock + " does not exist");
      throw new IllegalArgumentException(stock + " does not exist");
    }

    // Get the old quantity for the stock
    Long oldQty = quantities.get(stock);
    long newQty;
    if (oldQty == null) {
      // If oldQty is null it means that we have not that stock on our portfolio
      if (qty <= 0) {
        // We can't sell something we don't have, warn and return.
        log.warn(id + "|No stock to sell: " + stock);
        return;
      }

      // The new quantity is equal to the bought value
      newQty = qty;
    } else {
      assert (oldQty > 0);

      // The new quantity will be the value of the old quantity plus the qty value.
      // If qty is a negative number than we are selling, in the other case we're buying
      newQty = oldQty + qty;

      // Overflow check; just in case
      if (qty > 0 && newQty <= qty) {
        newQty = oldQty;
        log.warn(id + "|Quantity overflow; order ignored: " + stock);
        return;
      }
    }

    if (newQty < 0) {
      // We sold more than we had
      log.warn(id + "|Not enough stock to sell: " + stock);

      // We interpret this as "sell everything"
      newQty = 0;
    }

    if (newQty == 0) {
      // If we sold everything we remove the stock from the internal structure
      quantities.remove(stock);
    } else {
      // Save the actual quantity in internal structure
      quantities.put(stock, newQty);
    }

    if (listener != null) {
      // As we need an "effective final" variable, copy the value to be used in
      // the lambda passed to the executor
      long newVal = newQty;

      // Call the update on the listener; in case the listener has just been
      // detached, the listener should detect the case.
      executor.execute(() -> listener.update(stock, newVal));
    }
  }


  private boolean isValidStock(String stock) {
    if (!stock.startsWith("item")) {
      return false;
    }

    String stockNumString = stock.substring(4);

    int stockNum;
    try {
      stockNum = Integer.parseInt(stockNumString);
    } catch (NumberFormatException nfe) {
      return false;
    }

    return stockNum > 0 && stockNum <= 30;
  }

  public synchronized void flushToListener(PortfolioListener listener) {
    // Clone the actual status of the portfolio.
    final HashMap<String, Long> currentStatus = new HashMap<>(quantities);

    // Create a new Runnable to be used as a task to pass the actual status to the listener
    // We add the task on the executor to pass to the listener the actual status
    executor.execute(() -> {
      Set<Entry<String, Long>> entries = currentStatus.entrySet();

      // Iterates through the HashMap representing the current status to send
      // the snapshot to the client
      for (Entry<String, Long> entry : entries) {
        listener.update(entry.getKey(), entry.getValue().intValue());
      }
    });
  }

  public void setListener(PortfolioListener newListener) {
    Objects.requireNonNull(newListener, "Please provide a valid listener");

    // Set the listener
    listener = newListener;
    log.debug("Listener set on {}", id);

    // Send the current status to the listener
    flushToListener(newListener);
  }
}
