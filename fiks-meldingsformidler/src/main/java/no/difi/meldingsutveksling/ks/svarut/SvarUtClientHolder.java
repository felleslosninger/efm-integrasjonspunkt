package no.difi.meldingsutveksling.ks.svarut;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import jakarta.annotation.PostConstruct;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Slf4j
public class SvarUtClientHolder {

    private final IntegrasjonspunktProperties properties;
    private final SvarUtFaultInterceptor svarUtFaultInterceptor;

    @Getter
    private final Map<String, SvarUtWebServiceClientImpl> clients = Maps.newHashMap();

    @PostConstruct
    public void init() {
        if (!isNullOrEmpty(properties.getFiks().getUt().getUsername())) {
            clients.put(properties.getOrg().getNumber(),
                    svarUtClient(properties.getFiks().getUt().getUsername(),
                            properties.getFiks().getUt().getPassword()));
        }

        properties.getFiks().getUt().getPaaVegneAv().forEach((k, v) ->
                clients.put(k, svarUtClient(v.getUsername(), v.getPassword())));
    }

    public SvarUtWebServiceClientImpl getClient(String orgnr) {
        if (!clients.containsKey(orgnr)) {
            throw new IllegalArgumentException("No SvarUt client configured for orgnr: " + orgnr);
        }
        return clients.get(orgnr);
    }

    private SvarUtWebServiceClientImpl svarUtClient(String username, String password) {
        SvarUtWebServiceClientImpl client = new SvarUtWebServiceClientImpl();
        client.setDefaultUri("http://localhost:8080");
        client.setMessageFactory(getFactory());

        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setMtomEnabled(true);
        marshaller.setContextPath(Forsendelse.class.getPackage().getName());
        marshaller.setValidationEventHandler(event -> {
            log.error(event.getMessage(), event.getLinkedException());
            return false;
        });
        client.setMarshaller(marshaller);
        client.setUnmarshaller(marshaller);

        PreauthMessageSender preauthMessageSender = new PreauthMessageSender(username, password);
        client.setMessageSender(preauthMessageSender);

        final ClientInterceptor[] interceptors = new ClientInterceptor[1];
        interceptors[0] = svarUtFaultInterceptor;
        client.setInterceptors(interceptors);

        return client;
    }

    @SneakyThrows
    private static SaajSoapMessageFactory getFactory() {
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return new SaajSoapMessageFactory(messageFactory);
    }

}
