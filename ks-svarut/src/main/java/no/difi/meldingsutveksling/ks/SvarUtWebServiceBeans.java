package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtWebServiceBeans {
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setMtomEnabled(true);
        marshaller.setContextPath(Forsendelse.class.getPackage().getName());
        return marshaller;
    }

    @Bean
    public AxiomSoapMessageFactory svarUtMessageFactory() {
        final AxiomSoapMessageFactory axiomSoapMessageFactory = new AxiomSoapMessageFactory();
        axiomSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12);
        return axiomSoapMessageFactory;
    }

    @Bean
    public HttpComponentsMessageSender svarUtMessageSender(IntegrasjonspunktProperties properties) {
        HttpComponentsMessageSender httpMessageSender = new HttpComponentsMessageSender();
        httpMessageSender.setCredentials(new UsernamePasswordCredentials(properties.getDps().getUt().getUsername(), properties.getDps().getUt().getPassword()));
        return httpMessageSender;
    }

    @Bean
    public SvarUtWebServiceClientImpl svarUtClient(Jaxb2Marshaller marshaller, IntegrasjonspunktProperties properties, AxiomSoapMessageFactory svarUtMessageFactory, HttpComponentsMessageSender svarUtMessageSender) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("http://localhost:8080");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        client.setMessageFactory(svarUtMessageFactory);
        client.setMessageSender(svarUtMessageSender);

        return client;
    }
}
