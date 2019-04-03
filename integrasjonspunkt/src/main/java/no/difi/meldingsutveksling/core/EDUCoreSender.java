package no.difi.meldingsutveksling.core;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategy;
import no.difi.meldingsutveksling.noarkexchange.putmessage.MessageStrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.putmessage.StrategyFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.*;

@Component
public class EDUCoreSender {
    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final StrategyFactory strategyFactory;
    private final NoarkClient mshClient;
    private final ConversationService conversationService;

    @Autowired
    EDUCoreSender(IntegrasjonspunktProperties properties,
                  ServiceRegistryLookup serviceRegistryLookup,
                  StrategyFactory strategyFactory,
                  ConversationService conversationService,
                  @Qualifier("mshClient") ObjectProvider<NoarkClient> mshClient) {
        this.properties = properties;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.strategyFactory = strategyFactory;
        this.conversationService = conversationService;
        this.mshClient = mshClient.getIfAvailable();
    }

    public PutMessageResponseType sendMessage(EDUCore message) {
        Optional<ServiceRecord> serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier(), message.getServiceIdentifier());
        if (!serviceRecord.isPresent()) {
            throw new MeldingsUtvekslingRuntimeException(String.format("ServiceRecord of type %s not found for receiver %s",
                    message.getServiceIdentifier(), message.getReceiver().getIdentifier()));
        }
        PutMessageResponseType result;
        MessageStrategy strategy = null;
        final LogstashMarker marker = EDUCoreMarker.markerFrom(message);
        if (asList(DPO, DPF).contains(message.getServiceIdentifier()) &&
                this.strategyFactory.hasFactory(message.getServiceIdentifier())) {
            final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(message.getServiceIdentifier());
            strategy = messageStrategyFactory.create(message.getPayload());
            Audit.info(String.format("Send message to %s", message.getServiceIdentifier()), marker);
            result = strategy.send(message);
        } else if (!isNullOrEmpty(properties.getMsh().getEndpointURL())
                && mshClient.canRecieveMessage(message.getReceiver().getIdentifier())) {
            Audit.info("Send message to MSH", marker);

            PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(message);
            result = mshClient.sendEduMelding(putMessage);
        } else {
            if (!this.strategyFactory.hasFactory(DPV)) {
                Audit.error("Unable to send message: recipient does not have IP OR MSH is not configured OR service" +
                        " identifier is not " + DPV.toString(), marker);
                result = PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.UNABLE_TO_FIND_RECEIVER));
            } else {
                final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(DPV);
                strategy = messageStrategyFactory.create(message.getPayload());
                result = strategy.send(message);
            }
        }

        auditResult(result, message, Optional.ofNullable(strategy));
        createReceiptIfValidResult(result, message);
        return result;
    }

    private void createReceiptIfValidResult(PutMessageResponseType result, EDUCore message) {
        if (properties.getFeature().isEnableReceipts() &&
                message.getServiceIdentifier() != null &&
                "OK".equals(result.getResult().getType())) {
            MessageStatus ms = MessageStatus.of(GenericReceiptStatus.SENDT);
            if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
                ms.setDescription("AppReceipt");
            }
            conversationService.registerStatus(message.getId(), ms);
        }
    }

    private void auditResult(PutMessageResponseType result, EDUCore message, Optional<MessageStrategy> strategy) {
        String type = strategy.map(MessageStrategy::serviceName).orElse("MSH");
        if ("OK".equals(result.getResult().getType())) {
            Audit.info(String.format("%s message sent", type), EDUCoreMarker.markerFrom(message));
        } else {
            Audit.error(String.format("%s message sending failed", type), EDUCoreMarker.markerFrom(message));
        }
    }

}
