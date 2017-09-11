package no.difi.meldingsutveksling.core;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.mxa.MessageMarker;
import no.difi.meldingsutveksling.mxa.schema.domain.Message;
import no.difi.meldingsutveksling.noarkexchange.PutMessageMarker;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.PutMessageResponseFactory;
import no.difi.meldingsutveksling.noarkexchange.receive.InternalQueue;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationRepository;
import no.difi.meldingsutveksling.receipt.GenericReceiptStatus;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
@Component
public class EDUCoreService {

    private final IntegrasjonspunktProperties properties;
    private final EDUCoreSender coreSender;
    private final ServiceRegistryLookup serviceRegistryLookup;
    private final InternalQueue queue;
    private final ConversationRepository conversationRepository;

    @Autowired
    public EDUCoreService(
            IntegrasjonspunktProperties properties,
            EDUCoreSender coreSender,
            ServiceRegistryLookup serviceRegistryLookup,
            InternalQueue queue,
            ConversationRepository conversationRepository) {
        this.properties = properties;
        this.coreSender = coreSender;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.queue = queue;
        this.conversationRepository = conversationRepository;
    }

    public PutMessageResponseType queueMessage(Message msg) {
        if (isNullOrEmpty(properties.getOrg().getNumber())) {
            Audit.error("Senders orgnr missing", MessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing senders orgnumber. Please configure orgnumber= in the integrasjonspunkt-local.properties");
        }

        if (isNullOrEmpty(msg.getParticipantId())) {
            Audit.error("Receiver identifier missing", MessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing receiver identifier.");
        }

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        EDUCore message = eduCoreFactory.create(msg, properties.getOrg().getNumber());
        return queueMessage(message);
    }

    public PutMessageResponseType queueMessage(PutMessageRequestWrapper msg) {

        // Validate sender identifier
        if (!msg.hasSenderPartyNumber()) {
            Audit.error("Senders orgnr missing", PutMessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing senders orgnumber. Please configure orgnumber= in the integrasjonspunkt-local.properties");
        }
        // Validate receiver identifier
        if (!msg.hasRecieverPartyNumber()) {
            Audit.error("Receiver orgnr missing", PutMessageMarker.markerFrom(msg));
            throw new MeldingsUtvekslingRuntimeException("Missing receivers orgnumber.");
        }

        EDUCoreFactory eduCoreFactory = new EDUCoreFactory(serviceRegistryLookup);
        EDUCore message = eduCoreFactory.create(msg.getRequest(), msg.getSenderPartynumber());
        return queueMessage(message);
    }

    private PutMessageResponseType queueMessage(EDUCore message) {
        createConversation(message);
        if (properties.getFeature().isEnableQueue()) {
            queue.enqueueExternal(message);
            Audit.info("Message enqueued", EDUCoreMarker.markerFrom(message));
            return PutMessageResponseFactory.createOkResponse();
        } else {
            Audit.info("Queue is disabled", EDUCoreMarker.markerFrom(message));
            return coreSender.sendMessage(message);
        }
    }

    private void createConversation(EDUCore message) {
        MessageStatus ms = MessageStatus.of(GenericReceiptStatus.OPPRETTET.toString(), LocalDateTime.now());
        if (message.getMessageType() == EDUCore.MessageType.APPRECEIPT) {
            ms.setDescription("AppReceipt");
        }
        Conversation conversation = Conversation.of(message, ms);
        conversationRepository.save(conversation);
    }

}
