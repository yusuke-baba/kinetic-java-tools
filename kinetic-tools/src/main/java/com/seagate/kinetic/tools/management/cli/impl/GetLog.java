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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kinetic.admin.Capacity;
import kinetic.admin.Configuration;
import kinetic.admin.KineticAdminClient;
import kinetic.admin.KineticLog;
import kinetic.admin.KineticLogType;
import kinetic.admin.Limits;
import kinetic.admin.Statistics;
import kinetic.admin.Temperature;
import kinetic.admin.Utilization;
import kinetic.client.KineticException;

import com.seagate.kinetic.tools.management.common.KineticToolsException;
import com.seagate.kinetic.tools.management.common.util.MessageUtil;

public class GetLog extends AbstractCommand {
    private static final String ALL = "all";
    private static final String TEMPERATURE = "temperature";
    private static final String CAPACITY = "capacity";
    private static final String UTILIZATION = "utilization";
    private static final String CONFIGURATION = "configuration";
    private static final String MESSAGES = "message";
    private static final String STATISTICS = "statistic";
    private static final String LIMITS = "limits";
    private String logOutFile;
    private String logType;

    public GetLog(String nodesLogFile, String logOutFile, String logType,
            boolean useSsl, long clusterVersion, long identity, String key,
            long requestTimeout) throws IOException {
        super(useSsl, clusterVersion, identity, key, requestTimeout,
                nodesLogFile);
        this.logType = logType;
        this.logOutFile = logOutFile;
        this.sb = new StringBuffer();
    }

    private void getAndStoreLog() throws Exception {
        if (null == devices || devices.isEmpty()) {
            throw new Exception("Drives get from input file are null or empty.");
        }

        System.out.println("Start getting and storing log......");

        sb.append("[\n");
        List<AbstractWorkThread> threads = new ArrayList<AbstractWorkThread>();
        for (KineticDevice device : devices) {
            threads.add(new GetLogThread(logType, device));
        }
        poolExecuteThreadsInGroups(threads);
        sb.append("\n]");
        persistToFile(sb.toString());
    }

    private void persistToFile(String log) throws IOException {
        FileOutputStream fos = new FileOutputStream(new File(logOutFile));
        fos.write(log.getBytes("UTF-8"));
        fos.flush();
        fos.close();
    }

    private String logToJson(KineticDevice device, KineticLog log,
            String logType) throws KineticException {
        StringBuffer sb = new StringBuffer();
        String jsonLog = "";
        sb.append("  {\n");
        sb.append("    \"device\": ");
        sb.append(MessageUtil.toJson(device));
        sb.append(",\n");
        sb.append("    \"log\": ");
        if (logType.equalsIgnoreCase(ALL)) {
            MyKineticLog myLog = new MyKineticLog(log);
            jsonLog = MessageUtil.toJson(myLog);
        } else if (logType.equalsIgnoreCase(UTILIZATION)) {
            jsonLog = MessageUtil.toJson(log.getUtilization());
        } else if (logType.equalsIgnoreCase(CAPACITY)) {
            jsonLog = MessageUtil.toJson(log.getCapacity());
        } else if (logType.equalsIgnoreCase(TEMPERATURE)) {
            jsonLog = MessageUtil.toJson(log.getTemperature());
        } else if (logType.equalsIgnoreCase(CONFIGURATION)) {
            jsonLog = MessageUtil.toJson(log.getConfiguration());
        } else if (logType.equalsIgnoreCase(MESSAGES)) {
            jsonLog = new String(log.getMessages());
        } else if (logType.equalsIgnoreCase(STATISTICS)) {
            sb.append(MessageUtil.toJson(log.getStatistics()));
        } else if (logType.equalsIgnoreCase(LIMITS)) {
            sb.append(MessageUtil.toJson(log.getLimits()));
        } else {
            throw new IllegalArgumentException(
                    "Type should be utilization, capacity, temperature, configuration, message, statistic, limits or all");
        }

        if (logType.equalsIgnoreCase(MESSAGES)) {
            sb.append("{\"messages\":");
            sb.append("\"");
            sb.append(jsonLog);
            sb.append("\"}");
        } else {
            sb.append(jsonLog);
        }
        report.setAdditionMessage(device, log);
        sb.append("\n  }");

        return sb.toString();
    }

    private void validateLogType(String logType)
            throws IllegalArgumentException {
        if (logType == null || logType.isEmpty()) {
            throw new IllegalArgumentException("Type can not be empty");
        }

        if (!logType.equalsIgnoreCase(CAPACITY)
                && !logType.equalsIgnoreCase(TEMPERATURE)
                && !logType.equalsIgnoreCase(UTILIZATION)
                && !logType.equalsIgnoreCase(CONFIGURATION)
                && !logType.equalsIgnoreCase(MESSAGES)
                && !logType.equalsIgnoreCase(STATISTICS)
                && !logType.equalsIgnoreCase(LIMITS)
                && !logType.equalsIgnoreCase(ALL)) {
            throw new IllegalArgumentException(
                    "Type should be utilization, capacity, temperature, configuration, message, statistic, limits or all");
        }
    }

    private KineticLog getLog(KineticAdminClient kineticAdminClient,
            String logType) throws KineticException {
        validateLogType(logType);

        List<KineticLogType> listOfLogType = new ArrayList<KineticLogType>();

        if (logType.equalsIgnoreCase(ALL)) {
            return kineticAdminClient.getLog();
        } else if (logType.equalsIgnoreCase(UTILIZATION)) {
            listOfLogType.add(KineticLogType.UTILIZATIONS);
        } else if (logType.equalsIgnoreCase(CAPACITY)) {
            listOfLogType.add(KineticLogType.CAPACITIES);
        } else if (logType.equalsIgnoreCase(TEMPERATURE)) {
            listOfLogType.add(KineticLogType.TEMPERATURES);
        } else if (logType.equalsIgnoreCase(CONFIGURATION)) {
            listOfLogType.add(KineticLogType.CONFIGURATION);
        } else if (logType.equalsIgnoreCase(MESSAGES)) {
            listOfLogType.add(KineticLogType.MESSAGES);
        } else if (logType.equalsIgnoreCase(STATISTICS)) {
            listOfLogType.add(KineticLogType.STATISTICS);
        } else if (logType.equalsIgnoreCase(LIMITS)) {
            listOfLogType.add(KineticLogType.LIMITS);
        } else {
            throw new IllegalArgumentException(
                    "Type should be utilization, capacity, temperature, configuration, message, statistic, limits or all");
        }

        return kineticAdminClient.getLog(listOfLogType);
    }

    class GetLogThread extends AbstractWorkThread {
        public GetLogThread(String logType, KineticDevice device)
                throws KineticException {
            super(device);
        }

        @Override
        void runTask() throws KineticToolsException {
            try {
                KineticLog log = getLog(adminClient, logType);
                String log2String = logToJson(device, log, logType);
                synchronized (sb) {
                    sb.append(log2String);
                }
                report.reportSuccess(device);
            } catch (KineticException e) {
                throw new KineticToolsException(e);
            }
        }
    }

    class MyKineticLog {
        private List<Utilization> utilization;
        private List<Temperature> temperature;
        private Capacity capacity;
        private Configuration configuration;
        private List<Statistics> statistics;
        private String messages;
        private KineticLogType[] containedLogTypes;
        private Limits limits;

        public MyKineticLog(KineticLog log) throws KineticException {
            this.utilization = log.getUtilization();
            this.temperature = log.getTemperature();
            this.capacity = log.getCapacity();
            this.configuration = log.getConfiguration();
            this.statistics = log.getStatistics();
            this.messages = new String(log.getMessages());
            this.containedLogTypes = log.getContainedLogTypes();
            this.limits = log.getLimits();
        }

        public List<Utilization> getUtilization() {
            return utilization;
        }

        public List<Temperature> getTemperature() {
            return temperature;
        }

        public Capacity getCapacity() {
            return capacity;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public List<Statistics> getStatistics() {
            return statistics;
        }

        public String getMessages() {
            return messages;
        }

        public KineticLogType[] getContainedLogTypes() {
            return containedLogTypes;
        }

        public Limits getLimits() {
            return limits;
        }
    }

    @Override
    public void execute() throws KineticToolsException {
        try {
            getAndStoreLog();
        } catch (Exception e) {
            throw new KineticToolsException(e);
        }
    }

    @Override
    public void done() throws KineticToolsException {
        super.done();
        System.out.println("Save logs to " + logOutFile + " completed.");
    }
}