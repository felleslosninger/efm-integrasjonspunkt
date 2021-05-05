package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.BusinessMessageFiller;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class NextMoveOutMessageFactory {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRecordProvider serviceRecordProvider;
    private final UUIDGenerator uuidGenerator;
    private final Clock clock;
    private final ObjectProvider<BusinessMessageFiller<?>> fillers;

    NextMoveOutMessage getNextMoveOutMessage(StandardBusinessDocument sbd) {
        ServiceRecord serviceRecord = serviceRecordProvider.getServiceRecord(sbd);

        setDefaults(sbd, serviceRecord);

        return new NextMoveOutMessage(
                sbd.getConversationId(),
                sbd.getDocumentId(),
                sbd.getProcess(),
                sbd.getReceiverIdentifier(),
                sbd.getSenderIdentifier(),
                serviceRecord.getServiceIdentifier(),
                sbd);
    }

    private void setDefaults(StandardBusinessDocument sbd, ServiceRecord serviceRecord) {
        sbd.getScopes()
                .stream()
                .filter(p -> !StringUtils.hasText(p.getInstanceIdentifier()))
                .forEach(p -> p.setInstanceIdentifier(uuidGenerator.generate()));

        if (sbd.getSenderIdentifier() == null) {
            Organisasjonsnummer org = Organisasjonsnummer.from(properties.getOrg().getNumber());
            sbd.getStandardBusinessDocumentHeader().addSender(
                    new Sender()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(org.asIso6523())
                                    .setAuthority(org.authority()))
            );
        }

        DocumentIdentification documentIdentification = sbd.getStandardBusinessDocumentHeader().getDocumentIdentification();

        if (documentIdentification.getInstanceIdentifier() == null) {
            documentIdentification.setInstanceIdentifier(uuidGenerator.generate());
        }

        if (documentIdentification.getCreationDateAndTime() == null) {
            documentIdentification.setCreationDateAndTime(OffsetDateTime.now(clock));
        }

        if (!sbd.getExpectedResponseDateTime().isPresent()) {
            OffsetDateTime ttl = OffsetDateTime.now(clock).plusHours(properties.getNextmove().getDefaultTtlHours());
            if (sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().isEmpty()) {
                CorrelationInformation ci = new CorrelationInformation().setExpectedResponseDateTime(ttl);
                sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().add(ci);
            } else {
                sbd.getScope(ScopeType.CONVERSATION_ID).getScopeInformation().stream()
                        .findFirst()
                        .ifPresent(ci -> ci.setExpectedResponseDateTime(ttl));
            }
        }

        // set defaults for business message if filler is provided
        fillers.orderedStream().filter(f -> f.getType() == sbd.getBusinessMessage().getClass())
            .findFirst()
            .ifPresent(f -> f.setDefaults(sbd.getBusinessMessage(), serviceRecord));

    }

}
