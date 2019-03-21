package no.difi.meldingsutveksling.dokumentpakking.service;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;

import java.time.ZonedDateTime;
import java.util.UUID;

import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromConversationId;
import static no.difi.meldingsutveksling.dokumentpakking.service.ScopeFactory.fromJournalPostId;

public class CreateSBD {
    private static final String STANDARD = "urn:no:difi:meldingsutveksling:1.0";
    private static final String HEADER_VERSION = "1.0";
    private static final String TYPE_VERSION_1 = "1.0";
    private static final String TYPE_VERSION_2 = "2.0";
    private static final String DPO_MELDING_DOCTYPE = "urn:no:difi:eFormidling:xsd::Melding##urn:www.difi.no:eFormidling:melding:2.0 ";

    public StandardBusinessDocument createSBD(Organisasjonsnummer avsender, Organisasjonsnummer mottaker, Object payload, String conversationId, String type, String journalPostId) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(createHeader(avsender, mottaker, conversationId, type, journalPostId))
                .setAny(payload);
    }

    public StandardBusinessDocument createNextMoveSBD(Organisasjonsnummer avsender,
                                                      Organisasjonsnummer mottaker,
                                                      String conversationId,
                                                      ServiceIdentifier serviceIdentifier,
                                                      Object any) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setHeaderVersion(HEADER_VERSION)
                        .addSender(createSender(avsender))
                        .addReceiver(createReceiver(mottaker))
                        .setDocumentIdentification(createDocumentIdentification(serviceIdentifier, DPO_MELDING_DOCTYPE))
                        .setBusinessScope(createBusinessScope(fromConversationId(conversationId)))
                ).setAny(any);
    }

    private StandardBusinessDocumentHeader createHeader(Organisasjonsnummer avsender, Organisasjonsnummer mottaker,
                                                        String conversationId, String documentType, String
                                                                journalPostId) {
        return new StandardBusinessDocumentHeader()
                .setHeaderVersion(HEADER_VERSION)
                .addSender(createSender(avsender))
                .addReceiver(createReceiver(mottaker))
                .setDocumentIdentification(createDocumentIdentification(documentType))
                .setBusinessScope(createBusinessScope(fromConversationId(conversationId), fromJournalPostId(journalPostId)));
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
                .setAuthority(orgNummer.asIso6523()));
    }

    private DocumentIdentification createDocumentIdentification(ServiceIdentifier serviceIdentifier, String docType) {
        return new DocumentIdentification()
                .setCreationDateAndTime(ZonedDateTime.now())
                .setStandard(docType)
                .setType(serviceIdentifier.getFullname())
                .setTypeVersion(TYPE_VERSION_2)
                .setInstanceIdentifier(UUID.randomUUID().toString());
    }

    private DocumentIdentification createDocumentIdentification(String type) {
        return new DocumentIdentification()
                .setCreationDateAndTime(ZonedDateTime.now())
                .setStandard(STANDARD)
                .setType(type)
                .setTypeVersion(TYPE_VERSION_1)
                .setInstanceIdentifier(UUID.randomUUID().toString());
    }

    private BusinessScope createBusinessScope(Scope... scopes) {
        return new BusinessScope().addScopes(scopes);
    }
}
