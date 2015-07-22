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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.seagate.kinetic.tools.management.rest.message.RestRequest;
import com.seagate.kinetic.tools.management.rest.message.discover.DiscoverRequest;
import com.seagate.kinetic.tools.management.rest.service.ServiceHandler;

/**
 * Discover service handler.
 * 
 * @author chiaming
 *
 */
public class DiscoverHandler extends GenericServiceHandler implements
        ServiceHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public Class getRequestMessageClass() {
        return DiscoverRequest.class;
    }

    @Override
    protected void transformRequestParams(HttpServletRequest httpRequest,
            RestRequest req) {
        if (httpRequest.getContentLength() <= 0) {

            DiscoverRequest request = (DiscoverRequest) req;

            Map<String, String[]> params = httpRequest.getParameterMap();

            String[] subnet = params.get("subnet");
            if (subnet != null) {
                request.setSubnet(subnet[0]);
            }

            String[] clversion = params.get("clversion");
            if (clversion != null) {
                request.setClversion(Integer.parseInt(clversion[0]));
            }

            String[] timeout = params.get("timeout");
            if (timeout != null) {
                request.setTimeout(Integer.parseInt(timeout[0]));
            }

            String[] scopeStr = params.get("scoped");

            boolean scoped = false;
            if (scopeStr != null) {
                scoped = Boolean.parseBoolean(scopeStr[0]);
                request.setScoped(scoped);
            }

            if (scoped) {
                // start ip
                String[] startIp = params.get("startIp");
                if (startIp != null) {
                    request.setStartIp(startIp[0]);
                }

                // end ip
                String[] endIp = params.get("endIp");
                if (startIp != null) {
                    request.setEndIp(endIp[0]);
                }

            }

        }
    }

}
