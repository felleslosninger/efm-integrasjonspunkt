package no.difi.meldingsutveksling;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;


/**
 * @author Dervis M, 13/08/15.
 */

@Configuration
@ComponentScan("no.difi")
@ImportResource({"classpath*:rest-servlet.xml"})
public class IntegrasjonspunktWSConfiguration{

}
