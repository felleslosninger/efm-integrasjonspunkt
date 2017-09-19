package no.difi.meldingsutveksling.core;

import net.logstash.logback.marker.LogstashMarker;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
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
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.services.Adresseregister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPE_INNSYN;
import static no.difi.meldingsutveksling.ServiceIdentifier.DPV;

@Component
public class EDUCoreSender {
    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final StrategyFactory strategyFactory;
    private final Adresseregister adresseRegister;
    private final NoarkClient mshClient;
    private final ConversationRepository conversationRepository;

    @Autowired
    EDUCoreSender(IntegrasjonspunktProperties properties,
                  ServiceRegistryLookup serviceRegistryLookup,
                  StrategyFactory strategyFactory,
                  Adresseregister adresseregister,
                  ConversationRepository conversationRepository,
                  @Qualifier("mshClient") ObjectProvider<NoarkClient> mshClient) {
        this.properties = properties;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.strategyFactory = strategyFactory;
        this.adresseRegister = adresseregister;
        this.conversationRepository = conversationRepository;
        this.mshClient = mshClient.getIfAvailable();
    }

    public PutMessageResponseType sendMessage(EDUCore message) {
        final ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(message.getReceiver().getIdentifier());
        PutMessageResponseType result;
        MessageStrategy strategy = null;
        final LogstashMarker marker = EDUCoreMarker.markerFrom(message);
        if (adresseRegister.hasAdresseregisterCertificate(serviceRecord)) {
            Audit.info("Receiver validated", marker);

            final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(serviceRecord);
            strategy = messageStrategyFactory.create(message.getPayload());
            result = strategy.send(message);
        } else if (!isNullOrEmpty(properties.getMsh().getEndpointURL())
                && mshClient.canRecieveMessage(message.getReceiver().getIdentifier())) {
            Audit.info("Send message to MSH", marker);
            EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);

            PutMessageRequestType putMessage = eduCoreFactory.createPutMessageFromCore(message);
            result = mshClient.sendEduMelding(putMessage);
        } else if (asList(DPV, DPE_INNSYN).contains(serviceRecord.getServiceIdentifier())) {
            Audit.info("Send message to DPV", marker);
            final MessageStrategyFactory messageStrategyFactory = this.strategyFactory.getFactory(serviceRecord);
            strategy = messageStrategyFactory.create(message.getPayload());
            result = strategy.send(message);
        } else {
            Audit.error("Unable to send message: recipient does not have IP OR MSH is not configured OR service" +
                    " identifier is not " + DPV.toString(), marker);
            result = PutMessageResponseFactory.createErrorResponse(new MessageException(StatusMessage.UNABLE_TO_FIND_RECEIVER));
        }

        auditResult(result, message, Optional.ofNullable(strategy));
        createReceiptIfValidResult(result, message);
        return result;
    }

    private void createReceiptIfValidResult(PutMessageResponseType result, EDUCore message) {
        if (properties.getFeature().isEnableReceipts() &&
                message.getServiceIdentifier() != null &&
                "OK".equals(result.getResult().getType())) {
            MessageStatus status = MessageStatus.of(GenericReceiptStatus.SENDT.toString(), LocalDateTime.now());
            if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
                status.setDescription("AppReceipt");
            }
            Optional<Conversation> conv = conversationRepository.findByConversationId(message.getId()).stream().findFirst();
            conv.ifPresent(c -> {
                c.addMessageStatus(status);
                conversationRepository.save(c);
            });
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
