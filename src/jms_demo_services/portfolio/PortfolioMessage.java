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

import java.io.Serializable;

/**
 * A message published by Portfolio service and received from client.
 */
public class PortfolioMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // The portfolio ID
    public String portfolioId;
    
    // The updated stock key
    public String key;
    
    // The update command
    public String command;
    
    // The quantity
    public String qty;
    
    public PortfolioMessage() {}
    
    public PortfolioMessage(String portfolioId, String key, String command, String qty) {
    	this.portfolioId= portfolioId;
    	this.key= key;
    	this.command= command;
    	this.qty= qty;
    }
}
