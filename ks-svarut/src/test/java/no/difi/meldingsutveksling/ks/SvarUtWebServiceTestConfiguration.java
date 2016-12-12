package no.difi.meldingsutveksling.ks;

import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class SvarUtWebServiceTestConfiguration {

    @Bean
    public SvarUtWebServiceClient svarUtClient(Jaxb2Marshaller marshaller) {
        SvarUtWebServiceClient client = new SvarUtWebServiceClient();
        client.setDefaultUri("localhost");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
