package no.difi.meldingsutveksling.noarkexchange;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.MessageInformable;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.ConversationService;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnFieldValidator;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnPutMessageBuilder;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.nextmove.ConversationDirection;
import no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.status.Conversation;
import no.difi.move.common.io.pipe.PromiseMaker;
import no.difi.move.common.io.pipe.Reject;
import org.slf4j.MDC;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.receipt.ReceiptStatus.*;

@RequiredArgsConstructor
public class SvarInnPutMessageForwarder implements Consumer<Forsendelse> {

    private final IntegrasjonspunktProperties properties;
    private final ConversationService conversationService;
    private final SvarInnService svarInnService;
    private final NoarkClient localNoark;
    private final NoarkClient fiksMailClient;
    private final PutMessageRequestFactory putMessageRequestFactory;
    private final Clock clock;
    private final PromiseMaker promiseMaker;

    @Override
    public void accept(Forsendelse forsendelse) {
        promiseMaker.promise(reject -> {
            forward(forsendelse, reject);
            return null;
        }).await();
    }

    private void forward(Forsendelse forsendelse, Reject reject) {
        MDC.put(NextMoveConsts.CORRELATION_ID, forsendelse.getId());
        SvarInnPutMessageBuilder builder = new SvarInnPutMessageBuilder(forsendelse, clock, putMessageRequestFactory);
        svarInnService.getAttachments(forsendelse, reject).forEach(builder::streamedFile);
        if (!Strings.isNullOrEmpty(properties.getFiks().getInn().getFallbackSenderOrgNr())) {
            builder.setFallbackSenderOrgNr(properties.getFiks().getInn().getFallbackSenderOrgNr());
        }
        PutMessageRequestType putMessage = builder.build();

        if (builder.getDokumentTypeList().isEmpty()) {
            Audit.error("Zipfile is empty: skipping message", Markers.append("fiks-id", forsendelse.getId()));
            return;
        }

        Conversation c = conversationService.registerConversation(new MessageInformable() {
            @Override
            public String getConversationId() {
                return forsendelse.getId();
            }

            @Override
            public String getMessageId() {
                return forsendelse.getId();
            }

            @Override
            public PartnerIdentifier getSender() {
                return Iso6523.of(ICD.NO_ORG, putMessage.getEnvelope().getSender().getOrgnr());
            }

            @Override
            public PartnerIdentifier getReceiver() {
                return Iso6523.of(ICD.NO_ORG, putMessage.getEnvelope().getReceiver().getOrgnr());
            }

            @Override
            public String getProcessIdentifier() {
                return properties.getArkivmelding().getDefaultProcess();
            }

            @Override
            public String getDocumentIdentifier() {
                return properties.getArkivmelding().getDefaultDocumentType();
            }

            @Override
            public ConversationDirection getDirection() {
                return ConversationDirection.INCOMING;
            }

            @Override
            public ServiceIdentifier getServiceIdentifier() {
                return ServiceIdentifier.DPF;
            }

            @Override
            public OffsetDateTime getExpiry() {
                return OffsetDateTime.now(clock).plusHours(properties.getNextmove().getDefaultTtlHours());
            }
        });
        conversationService.registerStatus(c.getMessageId(), INNKOMMENDE_MOTTATT);

        String missingFields = getMissingFields(forsendelse, putMessage, builder.getDokumentTypeList());
        if (!missingFields.isEmpty()) {
            handleError(putMessage, forsendelse, "Validation failed - missing fields: " + missingFields);
            return;
        }

        final PutMessageResponseType response = localNoark.sendEduMelding(putMessage);
        if ("OK".equals(response.getResult().getType())) {
            Audit.info("Message successfully forwarded");
            conversationService.registerStatus(c.getMessageId(), INNKOMMENDE_LEVERT);
            svarInnService.confirmMessage(forsendelse);
        } else if ("WARNING".equals(response.getResult().getType())) {
            Audit.info(format("Archive system responded with warning for message with fiks-id %s",
                    forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            conversationService.registerStatus(c.getMessageId(), INNKOMMENDE_LEVERT);
            svarInnService.confirmMessage(forsendelse);
        } else {
            Audit.error(format("Message with fiks-id %s failed", forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
            handleError(putMessage, forsendelse, "Archive system responded with error: " + response.getResult().getMessage().get(0).getText());
        }
    }

    private void handleError(PutMessageRequestType message, Forsendelse forsendelse, String errorMsg) {
        if (properties.getFiks().getInn().isMailOnError()) {
            Audit.info(format("Sending message with id=%s by mail", forsendelse.getId()));
            fiksMailClient.sendEduMelding(message);
            conversationService.registerStatus(message.getEnvelope().getConversationId(), INNKOMMENDE_LEVERT);
            svarInnService.confirmMessage(forsendelse);
        } else {
            svarInnService.setErrorStateForMessage(forsendelse, errorMsg);
            conversationService.registerStatus(message.getEnvelope().getConversationId(), FEIL, errorMsg);
        }
    }

    private String getMissingFields(Forsendelse forsendelse, PutMessageRequestType putMessage, List<DokumentType> files) {
        SvarInnFieldValidator validator = SvarInnFieldValidator.validator()
                .addField(forsendelse.getMottaker().getOrgnr(), "receiver: orgnr")
                .addField(putMessage.getEnvelope().getSender().getOrgnr(), "sender: orgnr")
                .addField(forsendelse.getSvarSendesTil().getNavn(), "sender: name");

        files.forEach(f ->
                validator.addField(f.getVeDokformat(), "veDokformat") // veDokformat
                        .addField(f.getDbTittel(), "dbTittel") // dbTittel
        );

        if (!validator.getMissing().isEmpty()) {
            String missingFields = String.join(", ", validator.getMissing());
            Audit.error(format("Message with id=%s has the following missing field(s): %s",
                    forsendelse.getId(), missingFields));
            return missingFields;
        }

        return "";
    }
}
