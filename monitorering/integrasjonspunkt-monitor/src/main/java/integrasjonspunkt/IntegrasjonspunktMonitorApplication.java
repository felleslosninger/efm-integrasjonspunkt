package integrasjonspunkt;

import de.codecentric.boot.admin.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@EnableAdminServer
@EnableDiscoveryClient
public class IntegrasjonspunktMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrasjonspunktMonitorApplication.class, args);
    }
}
