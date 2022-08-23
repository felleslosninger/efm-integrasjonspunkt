package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.DpePolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusRestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@Order
@RequiredArgsConstructor
public class DefaultDpePolling implements DpePolling {

    private final ServiceBusRestClient serviceBusRestClient;
    private final IntegrasjonspunktProperties properties;

    @Override
    @Timed
    public void poll() {
        if (!properties.getNextmove().getServiceBus().isBatchRead()) {
            serviceBusRestClient.getAllMessagesRest();
        }
    }

}
