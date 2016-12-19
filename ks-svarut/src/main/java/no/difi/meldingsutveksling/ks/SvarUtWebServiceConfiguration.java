package no.difi.meldingsutveksling.ks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

@Configuration
public class SvarUtWebServiceConfiguration {
    @Bean
    public SvarUtWebServiceClientImpl svarUtClient(Jaxb2Marshaller marshaller) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("http://localhost:8080");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }

}
