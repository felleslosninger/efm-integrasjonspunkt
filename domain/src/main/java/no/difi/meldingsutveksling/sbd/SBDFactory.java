package no.difi.meldingsutveksling.sbd;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessageAsAttachment;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringType;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.meldingsutveksling.nextmove.StatusMessageAsAttachment;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.meldingsutveksling.status.Conversation;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static no.difi.meldingsutveksling.sbd.ScopeFactory.fromConversationId;

@Component
@RequiredArgsConstructor
public class SBDFactory {
    private static final String HEADER_VERSION = "1.0";
    private static final String TYPE_VERSION_2 = "2.0";

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final Clock clock;
    private final IntegrasjonspunktProperties props;
    private final UUIDGenerator uuidGenerator;

    public StandardBusinessDocument createNextMoveSBD(PartnerIdentifier avsender,
                                                      PartnerIdentifier mottaker,
                                                      String conversationId,
                                                      String messageId,
                                                      String process,
                                                      String documentType,
                                                      Object any) {
        Optional<MessageType> type = MessageType.valueOfDocumentType(documentType);
        if (type.isEmpty()) {
            try {
                ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(mottaker.getOrganizationIdentifier())
                        .process(process).conversationId(conversationId).build(), documentType);
                if (serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPFIO) {
                    type = Optional.of(MessageType.FIKSIO);
                }
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException("Error looking up service record for %s".formatted(mottaker), e);
            }
        }
        MessageType messageType = type.orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No valid messageType for documentType: " + documentType));
        return createNextMoveSBD(avsender, mottaker, conversationId, messageId, process, documentType, messageType, any);
    }

    public StandardBusinessDocument createNextMoveSBD(PartnerIdentifier avsender,
                                                      PartnerIdentifier mottaker,
                                                      String conversationId,
                                                      String messageId,
                                                      String process,
                                                      String documentType,
                                                      MessageType messageType,
                                                      Object any) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setHeaderVersion(HEADER_VERSION)
                        .addSender(createPartner(avsender))
                        .addReceiver(createPartner(mottaker))
                        .setDocumentIdentification(createDocumentIdentification(documentType, messageType, messageId))
                        .setBusinessScope(createBusinessScope(fromConversationId(conversationId, process, OffsetDateTime.now(clock).plusHours(props.getNextmove().getDefaultTtlHours()))))
                ).setAny(any);
    }


    public StandardBusinessDocument createStatusFrom(StandardBusinessDocument sbd,
                                                     ReceiptStatus status) {
        StandardBusinessDocument statusSbd = createNextMoveSBD(
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                sbd.getConversationId(),
                sbd.getMessageId(),
                createProcess(sbd),
                props.getNextmove().getStatusDocumentType(),
                new StatusMessageAsAttachment(status));

        SBDUtil.getOptionalMessageChannel(sbd).ifPresent(statusSbd::addScope);
        return statusSbd;
    }

    public StandardBusinessDocument createArkivmeldingReceiptFrom(NextMoveMessage message, ArkivmeldingKvitteringType type) {
        ArkivmeldingKvitteringMessageAsAttachment receipt = new ArkivmeldingKvitteringMessageAsAttachment(type.name(), message.getMessageId(), Sets.newHashSet());
        return createNextMoveSBD(message.getReceiver(),
                message.getSender(),
                message.getConversationId(),
                uuidGenerator.generate(),
                props.getArkivmelding().getReceiptProcess(),
                props.getArkivmelding().getReceiptDocumentType(),
                receipt);
    }

    public StandardBusinessDocument createArkivmeldingReceiptFrom(Conversation conversation, ArkivmeldingKvitteringType type) {
        ArkivmeldingKvitteringMessageAsAttachment receipt = new ArkivmeldingKvitteringMessageAsAttachment(type.name(), conversation.getMessageId(), Sets.newHashSet());
        return createNextMoveSBD(PartnerIdentifier.parse(conversation.getReceiver()),
                PartnerIdentifier.parse(conversation.getSender()),
                conversation.getConversationId(),
                uuidGenerator.generate(),
                props.getArkivmelding().getReceiptProcess(),
                props.getArkivmelding().getReceiptDocumentType(),
                receipt);
    }

    private String createProcess(StandardBusinessDocument sbd) {
        if (SBDUtil.isArkivmelding(sbd)) {
            return props.getArkivmelding().getReceiptProcess();
        }

        if (SBDUtil.isEinnsyn(sbd)) {
            return props.getEinnsyn().getReceiptProcess();
        }

        if (SBDUtil.isAvtalt(sbd)) {
            return props.getAvtalt().getReceiptProcess();
        }

        return null;
    }

    private Partner createPartner(PartnerIdentifier partnerIdentifier) {
        Partner sender = new Partner();
        fillPartner(sender, partnerIdentifier);
        return sender;
    }

    private void fillPartner(Partner partner, PartnerIdentifier partnerIdentifier) {
        partner.setIdentifier(new PartnerIdentification()
                .setValue(partnerIdentifier.getIdentifier())
                .setAuthority(partnerIdentifier.getAuthority()));
    }

    private DocumentIdentification createDocumentIdentification(String documentType,
                                                                MessageType messageType,
                                                                String messageId) {
        return new DocumentIdentification()
                // Need to subtract a couple of seconds due to SBD validation as @Past
                .setCreationDateAndTime(OffsetDateTime.now(clock).minusSeconds(5))
                .setStandard(documentType)
                .setType(messageType.getType())
                .setTypeVersion(TYPE_VERSION_2)
                .setInstanceIdentifier(messageId);
    }

    private BusinessScope createBusinessScope(Scope... scopes) {
        return new BusinessScope().addScopes(scopes);
    }
}
