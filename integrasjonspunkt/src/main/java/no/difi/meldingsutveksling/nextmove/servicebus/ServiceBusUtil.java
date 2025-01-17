package no.difi.meldingsutveksling.nextmove.servicebus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.nextmove.EinnsynKvitteringMessage;
import no.difi.meldingsutveksling.nextmove.EinnsynType;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.NextMoveRuntimeException;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;

import static no.difi.meldingsutveksling.NextMoveConsts.NEXTMOVE_QUEUE_PREFIX;
import static no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusQueueMode.*;
import static no.difi.meldingsutveksling.nextmove.servicebus.ServiceBusQueueMode.INNSYN;

@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPE", havingValue = "true")
@RequiredArgsConstructor
public class ServiceBusUtil {

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final IntegrasjonspunktProperties properties;

    @Getter
    private String localQueuePath;

    @PostConstruct
    private void init() {
        this.localQueuePath = NEXTMOVE_QUEUE_PREFIX +
                properties.getOrg().getNumber() +
                properties.getNextmove().getServiceBus().getMode();
    }

    public String getSasKey() {
        if (properties.getOidc().isEnable()) {
            return serviceRegistryLookup.getSasKey();
        } else {
            return properties.getNextmove().getServiceBus().getSasToken();
        }
    }

    public String getReceiverQueue(NextMoveOutMessage message) {
        String prefix = NextMoveConsts.NEXTMOVE_QUEUE_PREFIX + message.getReceiverIdentifier();

        if (SBDUtil.isStatus(message.getSbd())) {
            return prefix + statusSuffix();
        }
        if (message.getBusinessMessage() instanceof EinnsynKvitteringMessage) {
            return prefix + receiptSuffix((EinnsynKvitteringMessage) message.getBusinessMessage());
        }

        try {
            ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(
                    SRParameter.builder(message.getReceiverIdentifier())
                            .process(message.getSbd().getProcess())
                            .conversationId(message.getConversationId()).build(),
                    message.getSbd().getDocumentType());

            if (!StringUtils.hasText(serviceRecord.getService().getEndpointUrl())) {
                throw new NextMoveRuntimeException("No endpointUrl defined for process %s".formatted(serviceRecord.getProcess()));
            }
            return prefix + serviceRecord.getService().getEndpointUrl();
        } catch (ServiceRegistryLookupException e) {
            throw new NextMoveRuntimeException(e);
        }
    }

    private String receiptSuffix(EinnsynKvitteringMessage k) {
        if (StringUtils.hasText(properties.getNextmove().getServiceBus().getReceiptQueue())) {
            return properties.getNextmove().getServiceBus().getReceiptQueue();
        }
        if (k.getReferanseType() == EinnsynType.INNSYNSKRAV) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }

    private String statusSuffix() {
        if (StringUtils.hasText(properties.getNextmove().getServiceBus().getReceiptQueue())) {
            return properties.getNextmove().getServiceBus().getReceiptQueue();
        }
        if (MEETING.fullname().equals(properties.getNextmove().getServiceBus().getMode())) {
            return INNSYN.fullname();
        }
        if (INNSYN.fullname().equals(properties.getNextmove().getServiceBus().getMode())) {
            return DATA.fullname();
        }
        return INNSYN.fullname();
    }

}
