/**
 * Copyright (C) 2014 Seagate Technology.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.seagate.kinetic.tools.management.rest.service.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.seagate.kinetic.tools.management.rest.service.ServiceHandler;

/**
 * 
 * @author chiaming
 */
public class HandlerMap {

    private static final Logger logger = Logger.getLogger(HandlerMap.class
            .getName());

    // ping request
    public static final String PING = "/ping";

    // error handler
    public static final String ERROR = "/error";

    // discover request
    public static final String DISCOVER = "/discover";

    // handler map
    private static ConcurrentHashMap<String, ServiceHandler> hmap = new ConcurrentHashMap<String, ServiceHandler>();
    
    static {
        hmap.put(PING, new PingHandler());
        hmap.put(ERROR, new ErrorHandler());
        hmap.put(DISCOVER, new DiscoverHandler());
    }

    /**
     * find handler based on the request URI
     * 
     * @param path
     *            request URI
     * @return the matched service handler., or ErrorHandler if not found.
     */
    public static ServiceHandler findHandler(String path) {

        logger.info("*** path=" + path);

        ServiceHandler handler = null;

        handler = hmap.get(path);

        if (handler == null) {
            handler = hmap.get(ERROR);
        }

        return handler;
    }
    
}
