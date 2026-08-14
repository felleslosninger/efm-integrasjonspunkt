package no.difi.meldingsutveksling.ks.svarut.ws;

import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPConstants;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarut.PreauthMessageSender;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.status.MessageStatusFactory;
import no.difi.move.common.dokumentpakking.CreateCMSDocument;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.google.common.base.Strings.isNullOrEmpty;


@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fiks.ut.type", havingValue = "ws", matchIfMissing = true)
@ConditionalOnBooleanProperty(name = "difi.move.feature.enableDPF")
public class SvarUtWebServiceConfiguration {

    private final IntegrasjonspunktProperties properties;

    @Bean
    FiksWebServiceMapper fiksWebServiceMapper(ServiceRegistryLookup serviceRegistry,
                                              OptionalCryptoMessagePersister optionalCryptoMessagePersister,
                                              CreateCMSDocument createCMSDocument,
                                              ArkivmeldingUtil arkivmeldingUtil,
                                              Supplier<AlgorithmIdentifier> algorithmIdentifierSupplier) {
        return new FiksWebServiceMapper(properties, serviceRegistry, optionalCryptoMessagePersister, createCMSDocument, arkivmeldingUtil, algorithmIdentifierSupplier);
    }

    @Bean
    SvarUtInternalWebServiceClientHolder svarUtInternalWebServiceClientHolder(List<SvarUtWebServiceInterceptor> interceptors) {
        Map<String, SvarUtInternalWebServiceClient> clients = new LinkedHashMap<>();
        if (!isNullOrEmpty(properties.getFiks().getUt().getUsername())) {
            clients.put(properties.getOrg().getNumber(),
                svarUtInternalWebServiceClient(properties.getFiks().getUt().getUsername(),
                    properties.getFiks().getUt().getPassword(), interceptors));
        }

        properties.getFiks().getUt().getPaaVegneAv().forEach((k, v) ->
            clients.put(k, svarUtInternalWebServiceClient(v.getUsername(), v.getPassword(), interceptors)));


        return new SvarUtInternalWebServiceClientHolder(clients);
    }

    @Bean
    SvarUtFaultInterceptor svarUtFaultInterceptor() {
        return new SvarUtFaultInterceptor();
    }

    @Bean
    FiksWebServiceStatusMapper fiksWebServiceStatusMapper(MessageStatusFactory messageStatusFactory) {
        return new FiksWebServiceStatusMapper(messageStatusFactory);
    }

    @Bean
    SvarUtWebServiceClient svarUtWebServiceClient(FiksWebServiceMapper fiksMapper,
                                                  FiksWebServiceStatusMapper fiksStatusMapper,
                                                  SvarUtInternalWebServiceClientHolder svarUtClientHolder) {
        return new SvarUtWebServiceClient(fiksMapper, fiksStatusMapper, svarUtClientHolder);
    }

    private SvarUtInternalWebServiceClient svarUtInternalWebServiceClient(String username, String password, List<SvarUtWebServiceInterceptor> interceptors) {
        SvarUtInternalWebServiceClient client = new SvarUtInternalWebServiceClient();
        client.setDefaultUri(properties.getFiks().getUt().getWs().getEndpointUrl().toExternalForm());
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

        client.setInterceptors(interceptors.toArray(new ClientInterceptor[0]));

        return client;
    }

    @SneakyThrows
    private static SaajSoapMessageFactory getFactory() {
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        return new SaajSoapMessageFactory(messageFactory);
    }
}
