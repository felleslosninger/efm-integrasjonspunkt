package no.difi.meldingsutveksling.config;

import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author nikko
 */
@Profile("docker")
@Configuration
@EnableEurekaClient
public class EurekaClientConfiguration {

}
