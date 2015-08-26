package no.difi.meldingsutveksling;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;

public class EurekaTestClient {

    public void discoverService() {
        DynamicPropertyFactory configInstance = com.netflix.config.DynamicPropertyFactory.getInstance();
        ApplicationInfoManager applicationInfoManager = ApplicationInfoManager.getInstance();

        DiscoveryManager.getInstance().initComponent(
                new MyDataCenterInstanceConfig(),
                new DefaultEurekaClientConfig());

        DiscoveryClient eurekaClient = DiscoveryManager.getInstance().getDiscoveryClient();

        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
        String vipAddress = configInstance.getStringProperty("eureka.vipAddress", "test.domene.no").get();
        InstanceInfo nextServerInfo = null;
        while (nextServerInfo == null) {
            try {
                nextServerInfo = eurekaClient.getNextServerFromEureka(vipAddress, false);
            } catch (Throwable e) {
                System.out.println("Waiting ... verifying service registration with eureka ...");

                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        System.out.format("IP: %s, Port: %s", nextServerInfo.getIPAddr(), nextServerInfo.getPort());
    }

    public static void main(String[] args) {
        new EurekaTestClient().discoverService();
    }
}
