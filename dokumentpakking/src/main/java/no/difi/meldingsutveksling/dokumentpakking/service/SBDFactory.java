package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.MessageType;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.StatusMessage;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromConversationId;

@Component
@RequiredArgsConstructor
public class SBDFactory {
    private static final String HEADER_VERSION = "1.0";
    private static final String TYPE_VERSION_2 = "2.0";

    private final ServiceRegistryLookup serviceRegistryLookup;
    private final SBDUtil sbdUtil;
    private final Clock clock;
    private final IntegrasjonspunktProperties props;

    public StandardBusinessDocument createNextMoveSBD(Organisasjonsnummer avsender,
                                                      Organisasjonsnummer mottaker,
                                                      String conversationId,
                                                      String messageId,
                                                      String process,
                                                      String documentType,
                                                      Object any) {
        Optional<MessageType> type = MessageType.valueOfDocumentType(documentType);
        if (!type.isPresent()) {
            try {
                ServiceRecord serviceRecord = serviceRegistryLookup.getServiceRecord(SRParameter.builder(mottaker.getOrgNummer())
                    .process(process).conversationId(conversationId).build(), documentType);
                if (serviceRecord.getServiceIdentifier() == ServiceIdentifier.DPFIO) {
                    type = Optional.of(MessageType.FIKSIO);
                }
            } catch (ServiceRegistryLookupException e) {
                throw new MeldingsUtvekslingRuntimeException(String.format("Error looking up service record for %s", mottaker.getOrgNummer()), e);
            }
        }
        String messageType = type.orElseThrow(() -> new MeldingsUtvekslingRuntimeException("No valid messageType for documentType: "+documentType)).getType();

        return new StandardBusinessDocument()
            .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setHeaderVersion(HEADER_VERSION)
                .addSender(createSender(avsender))
                .addReceiver(createReceiver(mottaker))
                .setDocumentIdentification(createDocumentIdentification(documentType, messageType, messageId))
                .setBusinessScope(createBusinessScope(fromConversationId(conversationId, process, OffsetDateTime.now(clock).plusHours(props.getNextmove().getDefaultTtlHours()))))
            ).setAny(any);
    }


    public StandardBusinessDocument createStatusFrom(StandardBusinessDocument sbd,
                                                     ReceiptStatus status) {
        String process;
        if (sbdUtil.isArkivmelding(sbd)) {
            process = props.getArkivmelding().getReceiptProcess();
        } else if (sbdUtil.isEinnsyn(sbd)) {
            process = props.getEinnsyn().getReceiptProcess();
        } else {
            return null;
        }

        StatusMessage statusMessage = new StatusMessage(status);
        return createNextMoveSBD(sbd.getReceiver(),
            sbd.getSender(),
            sbd.getConversationId(),
            sbd.getMessageId(),
            process,
            props.getNextmove().getStatusDocumentType(),
            statusMessage);
    }


    private Receiver createReceiver(Organisasjonsnummer orgNummer) {
        Receiver sender = new Receiver();
        fillPartner(sender, orgNummer);
        return sender;
    }

    private Sender createSender(Organisasjonsnummer orgNummer) {
        Sender sender = new Sender();
        fillPartner(sender, orgNummer);
        return sender;
    }

    private void fillPartner(Partner partner, Organisasjonsnummer orgNummer) {
        partner.setIdentifier(new PartnerIdentification()
            .setValue(orgNummer.asIso6523())
            .setAuthority(orgNummer.authority()));
    }

    private DocumentIdentification createDocumentIdentification(String documentType,
                                                                String messageType,
                                                                String messageId) {
        return new DocumentIdentification()
            .setCreationDateAndTime(OffsetDateTime.now(clock))
            .setStandard(documentType)
            .setType(messageType)
            .setTypeVersion(TYPE_VERSION_2)
            .setInstanceIdentifier(messageId);
    }

    private BusinessScope createBusinessScope(Scope... scopes) {
        return new BusinessScope().addScopes(scopes);
    }
}
