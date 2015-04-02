package com.seagate.kinetic.tools.management.cli.impl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kinetic.admin.AdminClientConfiguration;
import kinetic.admin.Device;
import kinetic.admin.KineticAdminClient;
import kinetic.admin.KineticAdminClientFactory;
import kinetic.client.KineticException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;

import com.seagate.kinetic.tools.management.cli.impl.util.JsonUtil;

public class VendorSpecificDeviceLogGetter extends DefaultExecuter {
    private byte[] vendorSpecificName;
    private String outputFilePath;
    private StringBuffer sb = new StringBuffer();

    public VendorSpecificDeviceLogGetter(String vendorSpecificNameInString,
            String drivesInputFiles, String outputFilePath, boolean useSsl,
            long clusterVersion, long identity, String key, long requestTimeout)
            throws IOException {
        this.outputFilePath = outputFilePath;
        initBasicSettings(useSsl, clusterVersion, identity, key, requestTimeout);
        loadDevices(drivesInputFiles);
        this.vendorSpecificName = null;
        parseVendorSpecificDeviceName(vendorSpecificNameInString);
    }

    private void parseVendorSpecificDeviceName(String vendorSpecificNameInString) {
        if (vendorSpecificNameInString != null) {
            vendorSpecificName = vendorSpecificNameInString.getBytes(Charset
                    .forName("UTF-8"));
        }
    }

    public void vendorSpecificDeviceLogGetter() throws KineticException,
            InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(devices.size());
        ExecutorService pool = Executors.newCachedThreadPool();

        for (KineticDevice device : devices) {
            pool.execute(new getVendorSpecificDeviceLogThread(device,
                    vendorSpecificName, latch, useSsl, clusterVersion,
                    identity, key, requestTimeout));
        }

        latch.await();
        pool.shutdown();

        TimeUnit.SECONDS.sleep(2);
        System.out.flush();

        int totalDevices = devices.size();
        int succeedDevices = succeed.size();
        int failedDevices = failed.size();

        assert (succeedDevices + failedDevices == totalDevices);

        System.out.println("\nTotal(Succeed/Failed): " + totalDevices + "("
                + succeedDevices + "/" + failedDevices + ")");

        if (succeedDevices > 0) {
            System.out
                    .println("The following devices get vendor specific info succeed:");
            for (KineticDevice device : succeed.keySet()) {
                System.out.println(KineticDevice.toJson(device));
            }
        }

        if (failedDevices > 0) {
            System.out
                    .println("The following device get vendor specific info failed:");
            for (KineticDevice device : failed.keySet()) {
                System.out.println(KineticDevice.toJson(device));
            }
        }

        persist2File(sb.toString());
        System.out.println("Save logs to " + outputFilePath + " completed.");
    }

    private String device2Json(KineticDevice kineticDevice, Device device)
            throws JsonGenerationException, JsonMappingException, IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(" {\n");
        sb.append("   \"device\":");
        sb.append(JsonUtil.toJson(kineticDevice));
        sb.append(",\n");

        sb.append("   \"vendorspecificname\":");
        sb.append(JsonUtil.toJson(new String(device.getName())));
        sb.append(",\n");

        sb.append("   \"vendorspecificvalue\":");
        sb.append(new String(device.getValue()));
        sb.append("\n }");

        return sb.toString();
    }

    private void persist2File(String sb) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFilePath);
        fos.write(sb.getBytes("UTF-8"));
        fos.flush();
        fos.close();
    }

    class getVendorSpecificDeviceLogThread implements Runnable {
        private byte[] vendorSpecificName;
        private CountDownLatch latch;
        private KineticDevice device;
        private AdminClientConfiguration adminClientConfig;
        private KineticAdminClient adminClient;

        public getVendorSpecificDeviceLogThread(KineticDevice device,
                byte[] vendorSpecificName, CountDownLatch latch,
                boolean useSsl, long clusterVersion, long identity, String key,
                long requestTimeout) throws KineticException {
            this.vendorSpecificName = vendorSpecificName;
            this.device = device;
            this.latch = latch;

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
            adminClientConfig.setRequestTimeoutMillis(requestTimeout);

            adminClient = KineticAdminClientFactory
                    .createInstance(adminClientConfig);
        }

        @Override
        public void run() {
            try {
                Device vendorSpecficInfo = adminClient
                        .getVendorSpecificDeviceLog(vendorSpecificName);
                String vendorSpecficInfo2Json = device2Json(device,
                        vendorSpecficInfo);

                synchronized (this) {
                    sb.append(vendorSpecficInfo2Json + "\n");
                    succeed.put(device, vendorSpecficInfo2Json);
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
            }
        }
    }
}