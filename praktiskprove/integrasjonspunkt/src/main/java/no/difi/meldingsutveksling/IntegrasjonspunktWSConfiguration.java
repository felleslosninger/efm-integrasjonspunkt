package no.difi.meldingsutveksling;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


/**
 * @author Dervis M, 13/08/15.
 */

@Configuration
@ImportResource({"classpath*:rest-servlet.xml"})
@EnableAutoConfiguration
@ComponentScan("no.difi")
public class IntegrasjonspunktWSConfiguration {

}
