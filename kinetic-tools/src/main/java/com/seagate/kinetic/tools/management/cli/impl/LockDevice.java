package com.seagate.kinetic.tools.management.cli.impl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kinetic.admin.AdminClientConfiguration;
import kinetic.admin.KineticAdminClient;
import kinetic.admin.KineticAdminClientFactory;
import kinetic.client.KineticException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

public class LockDevice extends DefaultExecuter {
    private byte[] lockPin;

    public LockDevice(String driveInputFile, String lockPinInString,
            boolean useSsl, long clusterVersion, long identity, String key,
            long requestTimeout) throws IOException {
        this.lockPin = null;
        parsePin(lockPinInString);
        loadDevices(driveInputFile);
        initBasicSettings(useSsl, clusterVersion, identity, key, requestTimeout);
    }

    private void parsePin(String lockPinInString) {
        if (null != lockPinInString) {
            this.lockPin = lockPinInString.getBytes(Charset.forName("UTF-8"));
        }
    }

    public void lockDevice() throws InterruptedException, KineticException,
            JsonGenerationException, JsonMappingException, IOException {
        CountDownLatch latch = new CountDownLatch(devices.size());
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("Start lock device...");

        for (KineticDevice device : devices) {
            pool.execute(new LockDeviceThread(device, latch, lockPin, useSsl,
                    clusterVersion, identity, key, requestTimeout));
        }

        latch.await();
        pool.shutdown();

        int totalDevices = devices.size();
        int succeedDevices = succeed.size();
        int failedDevices = failed.size();

        assert (succeedDevices + failedDevices == totalDevices);

        TimeUnit.SECONDS.sleep(2);
        System.out.flush();

        System.out.println("\n(Succeed/Failed): " + totalDevices + "("
                + succeedDevices + "/" + failedDevices + ")");

        if (succeedDevices > 0) {
            System.out
                    .println("The following devices were locked successfully");
            for (KineticDevice device : succeed.keySet()) {
                System.out.println(KineticDevice.toJson(device));
            }
        }

        if (failedDevices > 0) {
            System.out.println("The following devices were locked failed");
            for (KineticDevice device : failed.keySet()) {
                System.out.println(KineticDevice.toJson(device));
            }
        }
    }

    class LockDeviceThread implements Runnable {
        private KineticDevice device = null;
        private CountDownLatch latch = null;
        private byte[] lockPin = null;
        private AdminClientConfiguration adminClientConfig = null;
        private KineticAdminClient adminClient = null;

        public LockDeviceThread(KineticDevice device, CountDownLatch latch,
                byte[] lockPin, boolean useSsl, long clusterVersion,
                long identity, String key, long requestTimeout)
                throws KineticException {
            this.device = device;
            this.latch = latch;
            this.lockPin = lockPin;
            adminClientConfig = new AdminClientConfiguration();
            adminClientConfig.setHost(device.getInet4().get(0));
            adminClientConfig.setUseSsl(useSsl);
            if (useSsl) {
                adminClientConfig.setPort(device.getTlsPort());
            } else {
                adminClientConfig.setPort(device.getPort());
            }
            adminClientConfig.setClusterVersion(clusterVersion);
            adminClientConfig.setUserId(identity);
            adminClientConfig.setKey(key);
            adminClientConfig.setClusterVersion(clusterVersion);

            adminClient = KineticAdminClientFactory
                    .createInstance(adminClientConfig);
        }

        @Override
        public void run() {
            try {
                adminClient.lockDevice(lockPin);
                synchronized (this) {
                    succeed.put(device, "");
                }

                latch.countDown();

                System.out.println("[Succeed]" + KineticDevice.toJson(device));
            } catch (KineticException e) {
                synchronized (this) {
                    failed.put(device, "");
                }

                latch.countDown();

                try {
                    System.out.println("[Failed]"
                            + KineticDevice.toJson(device) + "\n"
                            + e.getMessage());
                } catch (IOException e1) {
                    System.out.println(e1.getMessage());
                }

            } catch (JsonGenerationException e) {
                System.out.println(e.getMessage());
            } catch (JsonMappingException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    adminClient.close();
                } catch (KineticException e) {
                    System.out.println(e.getMessage());
                }
            }

        }

    }
}