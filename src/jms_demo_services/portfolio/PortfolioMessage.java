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

import java.io.Serializable;

/**
 * A message published by Portfolio service and received from client.
 */
public class PortfolioMessage implements Serializable {

    private static final long serialVersionUID = 3674505151072844205L;

    // The portfolio ID
    public String portfolioId;
    
    // The updated stock key
    public String key;
    
    // The quantity
    public String qty;
    
    public PortfolioMessage() {}
    
    public PortfolioMessage(String portfolioId, String key, String qty) {
        this.portfolioId= portfolioId;
        this.key= key;
        this.qty= qty;
    }
}
