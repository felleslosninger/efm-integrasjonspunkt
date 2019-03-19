package no.difi.meldingsutveksling;

import com.sun.xml.ws.developer.JAXWSProperties;
import lombok.Getter;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.BrokerServiceExternalBasicSF;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.IBrokerServiceExternalBasic;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.BrokerServiceExternalBasicStreamedSF;
import no.difi.meldingsutveksling.altinn.mock.brokerstreamed.IBrokerServiceExternalBasicStreamed;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

@Component
public class AltinnWsClientFactory implements ApplicationContextAware {

    @Getter
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public AltinnWsClient getAltinnWsClient(ServiceRecord serviceRecord) {
        AltinnWsConfiguration configuration = AltinnWsConfiguration.fromConfiguration(serviceRecord, applicationContext);
        return new AltinnWsClient(
                getBrokerServiceExternalBasicSF(configuration),
                getBrokerServiceExternalBasicStreamedSF(configuration),
                configuration,
                applicationContext);
    }

    private IBrokerServiceExternalBasic getBrokerServiceExternalBasicSF(AltinnWsConfiguration configuration) {
        BrokerServiceExternalBasicSF brokerServiceExternalBasicSF;
        brokerServiceExternalBasicSF = new BrokerServiceExternalBasicSF(configuration.getBrokerServiceUrl());
        IBrokerServiceExternalBasic service = brokerServiceExternalBasicSF.getBasicHttpBindingIBrokerServiceExternalBasic();
        BindingProvider bp = (BindingProvider) service;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getBrokerServiceUrl().toString());
        return service;
    }

    private IBrokerServiceExternalBasicStreamed getBrokerServiceExternalBasicStreamedSF(AltinnWsConfiguration configuration) {
        BrokerServiceExternalBasicStreamedSF brokerServiceExternalBasicStreamedSF;
        brokerServiceExternalBasicStreamedSF = new BrokerServiceExternalBasicStreamedSF(configuration.getStreamingServiceUrl());
        IBrokerServiceExternalBasicStreamed streamingService = brokerServiceExternalBasicStreamedSF.getBasicHttpBindingIBrokerServiceExternalBasicStreamed(new MTOMFeature(true));

        BindingProvider bp = (BindingProvider) streamingService;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, configuration.getStreamingServiceUrl().toString());
        bp.getRequestContext().put(JAXWSProperties.HTTP_CLIENT_STREAMING_CHUNK_SIZE, 8192);
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);
        return streamingService;
    }

}
