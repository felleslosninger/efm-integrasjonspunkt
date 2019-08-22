package no.difi.meldingsutveksling;

import com.sun.xml.ws.developer.JAXWSProperties;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.BrokerServiceExternalBasicStreamedSF;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

@Component
@RequiredArgsConstructor
public class AltinnWsClientFactory {

    private final ApplicationContextHolder applicationContextHolder;
    private final AltinnWsConfigurationFactory altinnWsConfigurationFactory;
    private final TaskExecutor taskExecutor;
    private final Plumber plumber;
    private final IntegrasjonspunktProperties properties;

    public AltinnWsClient getAltinnWsClient(ServiceRecord serviceRecord) {
        AltinnWsConfiguration configuration = altinnWsConfigurationFactory.fromServiceRecord(serviceRecord);
        return new AltinnWsClient(
                getBrokerServiceExternalBasicSF(configuration),
                getBrokerServiceExternalBasicStreamedSF(configuration),
                configuration,
                applicationContextHolder.getApplicationContext(),
                taskExecutor,
                plumber);
    }

    private IBrokerServiceExternalBasic getBrokerServiceExternalBasicSF(AltinnWsConfiguration configuration) {
        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, properties.getDpo().getRequestTimeout());
        bp.getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, properties.getDpo().getConnectTimeout());
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());
        return service;
    }

    private IBrokerServiceExternalBasicStreamed getBrokerServiceExternalBasicStreamedSF(AltinnWsConfiguration configuration) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;
        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());
        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed(new MTOMFeature(true));

        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());
        bp.getRequestContext().put(JAXWSProperties.REQUEST_TIMEOUT, properties.getDpo().getRequestTimeout());
        bp.getRequestContext().put(JAXWSProperties.CONNECT_TIMEOUT, properties.getDpo().getConnectTimeout());
        bp.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);
        return streamingService;
    }

}
