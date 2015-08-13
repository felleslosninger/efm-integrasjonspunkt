package no.difi.meldingsutveksling;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * @author Dervis M, 13/08/15.
 */

@Configuration
@ImportResource({"classpath*:spring-rest.xml"})
@EnableAutoConfiguration
public class IntegrasjonspunktConfiguration {

}
