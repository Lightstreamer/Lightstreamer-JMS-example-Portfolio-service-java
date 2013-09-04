/*
 *
 * Copyright 2013 Weswit s.r.l.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package jms_demo_services.portfolio;

import java.util.Map;

/**
 * Used to receive data from the simulated portfolio feed in an
 * asynchronous way.
 * Upon listener submission, a single call to onActualStatus is issued
 * in short time, then multiple calls to "update" can be issued.
 */
public interface PortfolioListener {

    /**
     * Called at first to send the actual portfolio contents.
     * The map associates stock ids with quantities.
     * Only stocks with positive quantities are included.
     */
    public void onActualStatus(Map<String, Integer> currentStatus);

    /**
     * Called on each new update on the state of the portfolio.
     * If oldQty is 0 means that the stock wasn't on the portfolio before;
     * if qty is 0 means that the stock was completely sold from the portfolio.
     */
    public void update(String stock, int qty, int oldQty);

}