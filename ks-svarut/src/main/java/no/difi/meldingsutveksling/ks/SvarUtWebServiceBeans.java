package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
public class SvarUtWebServiceBeans {
    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(Forsendelse.class.getPackage().getName());
        return marshaller;
    }

    @Bean
    public SaajSoapMessageFactory svarUtMessageFactory() {
        final SaajSoapMessageFactory saajSoapMessageFactory = new SaajSoapMessageFactory();
        saajSoapMessageFactory.setSoapVersion(SoapVersion.SOAP_12);
        return saajSoapMessageFactory;
    }

    @Bean
    public SvarUtWebServiceClientImpl svarUtClient(Jaxb2Marshaller marshaller, IntegrasjonspunktProperties properties, SaajSoapMessageFactory svarUtMessageFactory) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("http://localhost:8080");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        client.setMessageFactory(svarUtMessageFactory);

        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementUsername(properties.getDps().getUsername());
        securityInterceptor.setSecurementPassword(properties.getDps().getPassword());
        securityInterceptor.setSecurementActions("UsernameToken");
        securityInterceptor.setSecurementPasswordType("PasswordText");
        securityInterceptor.setValidateResponse(false);
        securityInterceptor.setValidationActions("UsernameToken");
        ClientInterceptor[] interceptors = {securityInterceptor};
        client.setInterceptors(interceptors);
        return client;
    }
}
