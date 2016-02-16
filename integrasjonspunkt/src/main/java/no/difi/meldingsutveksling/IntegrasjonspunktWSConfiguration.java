package no.difi.meldingsutveksling;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ComponentScan("no.difi")
@ImportResource({"classpath*:rest-servlet.xml"})
public class IntegrasjonspunktWSConfiguration{


}
