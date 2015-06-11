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
package com.seagate.kinetic.tools.management.cli.impl;

import com.seagate.kinetic.tools.management.common.KineticToolsException;

public class DefaultCommandInvoker implements Invoker {

    @Override
    public Report execute(Command command) {
        try {
            command.init();
            command.execute();
            command.done();
        } catch (KineticToolsException e) {
            System.err.println("Exec command failed.");
            e.printStackTrace();
            return null;
        }

        return command.getReport();
    }
}