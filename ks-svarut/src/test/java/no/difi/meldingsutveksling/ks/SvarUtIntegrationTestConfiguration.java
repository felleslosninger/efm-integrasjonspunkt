package no.difi.meldingsutveksling.ks;

import org.springframework.context.annotation.Bean;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class SvarUtIntegrationTestConfiguration {

    @Bean
    public SvarUtService svarUtService(EDUCoreConverter converter, SvarUtWebServiceClient svarUtClient) {
        return new SvarUtService(converter, svarUtClient);
    }


    @Bean
    public SvarUtWebServiceClientImpl svarUtClient(Jaxb2Marshaller marshaller) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("localhost");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);
        return client;
    }
}
