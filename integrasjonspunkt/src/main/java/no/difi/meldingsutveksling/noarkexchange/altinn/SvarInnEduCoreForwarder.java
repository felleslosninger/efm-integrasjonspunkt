package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnEduCoreBuilder;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnFieldValidator;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.INNKOMMENDE_LEVERT;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.INNKOMMENDE_MOTTATT;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@Component
public class SvarInnEduCoreForwarder implements Consumer<Forsendelse> {

    private final IntegrasjonspunktProperties properties;
    private final ConversationService conversationService;
    private final SvarInnService svarInnService;
    private final NoarkClient localNoark;
    private final NoarkClient fiksMailClient;

    @Override
    public void accept(Forsendelse forsendelse) {
        SvarInnEduCoreBuilder builder = new SvarInnEduCoreBuilder(forsendelse);
        svarInnService.getAttachments(forsendelse).forEach(builder::streamedFile);
        final EDUCore eduCore = builder.build();

        if (builder.getDokumentTypeList().isEmpty()) {
            Audit.error("Zipfile is empty: skipping message", Markers.append("fiks-id", forsendelse.getId()));
            return;
        }

        Conversation c = conversationService.registerConversation(eduCore);
        c = conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_MOTTATT));

        PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
        if (!validateRequiredFields(forsendelse, eduCore, builder.getDokumentTypeList())) {
            checkAndSendMail(putMessage, forsendelse.getId());
            return;
        }

        final PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if ("OK".equals(response.getResult().getType())) {
            Audit.info("Message successfully forwarded");
            conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
            svarInnService.confirmMessage(forsendelse.getId());
        } else if ("WARNING".equals(response.getResult().getType())) {
            Audit.info(format("Archive system responded with warning for message with fiks-id %s",
                    forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
            svarInnService.confirmMessage(forsendelse.getId());
        } else {
            Audit.error(format("Message with fiks-id %s failed", forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            checkAndSendMail(putMessage, forsendelse.getId());
        }
    }

    private void checkAndSendMail(PutMessageRequestType message, String fiksId) {
        if (properties.getFiks().getInn().isMailOnError()) {
            Audit.info(format("Sending message with id=%s by mail", fiksId));
            fiksMailClient.sendEduMelding(message);
            svarInnService.confirmMessage(fiksId);
        }
    }

    private boolean validateRequiredFields(Forsendelse forsendelse, EDUCore eduCore, List<DokumentType> files) {
        SvarInnFieldValidator validator = SvarInnFieldValidator.validator()
                .addField(forsendelse.getMottaker().getOrgnr(), "receiver: orgnr")
                .addField(eduCore.getSender().getIdentifier(), "sender: orgnr")
                .addField(forsendelse.getSvarSendesTil().getNavn(), "sender: name");

        files.forEach(f ->
                validator.addField(f.getVeDokformat(), "veDokformat") // veDokformat
                        .addField(f.getDbTittel(), "dbTittel") // dbTittel
        );

        if (!validator.getMissing().isEmpty()) {
            String missingFields = String.join(", ", validator.getMissing());
            Audit.error(format("Message with id=%s has the following missing field(s): %s",
                    forsendelse.getId(), missingFields));
            return false;
        }

        return true;
    }
}
