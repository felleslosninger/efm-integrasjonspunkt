package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor;

@Configuration
@EnableConfigurationProperties({IntegrasjonspunktProperties.class})
@Import(value=SvarUtConfiguration.class)
public class SvarUtWebServiceBeans {
    @Bean
    public SvarUtWebServiceClientImpl svarUtClient(Jaxb2Marshaller marshaller, IntegrasjonspunktProperties properties) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("http://localhost:8080");
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        Wss4jSecurityInterceptor securityInterceptor = new Wss4jSecurityInterceptor();
        securityInterceptor.setSecurementUsername(properties.getDps().getUsername());
        securityInterceptor.setSecurementPassword(properties.getDps().getPassword());
        securityInterceptor.setSecurementActions("UsernameToken");
        securityInterceptor.setSecurementPasswordType("PasswordText");
        securityInterceptor.setValidateResponse(false);
        securityInterceptor.setValidationActions("UsernameToken");
        ClientInterceptor interceptors[] = {securityInterceptor};
        client.setInterceptors(interceptors);
        return client;
    }

    @Bean
    public SvarUtService svarUtService(EDUCoreConverter converter, SvarUtWebServiceClient svarUtClient) {
        return new SvarUtService(converter, svarUtClient);
    }


}
