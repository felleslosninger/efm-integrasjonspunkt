package no.difi.meldingsutveksling.ks.svarinn;

import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.MessageDownloaderModule;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.logging.PutMessageResponseMarkers;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;
import no.difi.meldingsutveksling.receipt.Conversation;
import no.difi.meldingsutveksling.receipt.ConversationService;
import no.difi.meldingsutveksling.receipt.MessageStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.receipt.GenericReceiptStatus.INNKOMMENDE_LEVERT;
import static no.difi.meldingsutveksling.receipt.GenericReceiptStatus.INNKOMMENDE_MOTTATT;

public class SvarInnService implements MessageDownloaderModule {

    private SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;
    private SvarInnUnzipper unzipper;
    private NoarkClient noarkClient;
    private NoarkClient mailClient;
    private SvarInnFileFactory svarInnFileFactory;
    private IntegrasjonspunktProperties properties;
    private ConversationService conversationService;

    public SvarInnService(SvarInnClient svarInnClient,
                          SvarInnFileDecryptor decryptor,
                          SvarInnUnzipper unzipper,
                          NoarkClient noarkClient,
                          NoarkClient mailClient,
                          ConversationService conversationService,
                          IntegrasjonspunktProperties properties) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
        this.unzipper = unzipper;
        this.noarkClient = noarkClient;
        this.mailClient = mailClient;
        this.conversationService = conversationService;
        this.properties = properties;
        svarInnFileFactory = new SvarInnFileFactory();
    }

    @Override
    public void downloadFiles() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        if (!forsendelses.isEmpty()) {
            Audit.info(format("%d new messages in FIKS", forsendelses.size()));
        }
        for(Forsendelse forsendelse : forsendelses) {
            Audit.info(format("Downloading message with fiks-id %s", forsendelse.getId()), Markers.append("fiks-id", forsendelse.getId()));
            final SvarInnFile svarInnFile = svarInnClient.downloadFile(forsendelse.getDownloadUrl());
            final byte[] decrypt = decryptor.decrypt(svarInnFile.getContents());
            final Map<String, byte[]> unzippedFile;
            try {
                unzippedFile = unzipper.unzip(decrypt);
            } catch (IOException e) {
                throw new SvarInnForsendelseException("Unable to unzip file", e);
            }
            if (unzippedFile.values().isEmpty()) {
                Audit.error("Zipfile is empty: skipping message", Markers.append("fiks-id", forsendelse.getId()));
                continue;
            }
            // create SvarInnFile with unzipped file and correct mimetype
            final List<SvarInnFile> files = svarInnFileFactory.createFiles(forsendelse.getFilmetadata(), unzippedFile);

            final SvarInnMessage message = new SvarInnMessage(forsendelse, files, properties);
            final EDUCore eduCore = message.toEduCore();

            Conversation c = conversationService.registerConversation(eduCore);
            c = conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_MOTTATT));

            PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);
            if (!validateRequiredFields(forsendelse, eduCore, files)) {
                checkAndSendMail(putMessage, forsendelse.getId());
                continue;
            }

            final PutMessageResponseType response = noarkClient.sendEduMelding(putMessage);
            if ("OK".equals(response.getResult().getType())) {
                Audit.info("Message successfully forwarded");
                conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
                svarInnClient.confirmMessage(forsendelse.getId());
            } else if ("WARNING".equals(response.getResult().getType())) {
                Audit.info(format("Archive system responded with warning for message with fiks-id %s",
                        forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
                conversationService.registerStatus(c, MessageStatus.of(INNKOMMENDE_LEVERT));
                svarInnClient.confirmMessage(forsendelse.getId());
            } else {
                Audit.error(format("Message with fiks-id %s failed", forsendelse.getId()), PutMessageResponseMarkers.markerFrom(response));
                checkAndSendMail(putMessage, forsendelse.getId());
            }
        }
    }

    private void checkAndSendMail(PutMessageRequestType message, String fiksId) {
        if (properties.getFiks().getInn().isMailOnError()) {
            Audit.info(format("Sending message with id=%s by mail", fiksId));
            mailClient.sendEduMelding(message);
            svarInnClient.confirmMessage(fiksId);
        }
    }

    private boolean validateRequiredFields(Forsendelse forsendelse, EDUCore eduCore, List<SvarInnFile> files) {
        SvarInnFieldValidator validator = SvarInnFieldValidator.validator()
                .addField(forsendelse.getMottaker().getOrgnr(), "receiver: orgnr")
                .addField(eduCore.getSender().getIdentifier(), "sender: orgnr")
                .addField(forsendelse.getSvarSendesTil().getNavn(), "sender: name");
        files.forEach(f ->
            validator.addField(f.getMediaType().toString(), "veDokformat") // veDokformat
            .addField(f.getFilnavn(), "dbTittel") // dbTittel
        );

        if (!validator.getMissing().isEmpty()) {
            String missingFields = validator.getMissing().stream().reduce((a, b) -> a + ", " + b).get();
            Audit.error(format("Message with id=%s has the following missing field(s): %s",
                    forsendelse.getId(), missingFields));
            return false;
        }

        return true;
    }

}
