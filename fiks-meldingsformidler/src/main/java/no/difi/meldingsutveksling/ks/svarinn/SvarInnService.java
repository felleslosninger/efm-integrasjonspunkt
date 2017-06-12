package no.difi.meldingsutveksling.ks.svarinn;

import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.MessageDownloaderModule;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreFactory;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.meldingsutveksling.noarkexchange.NoarkClient;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageResponseType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SvarInnService implements MessageDownloaderModule {

    private SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;
    private SvarInnUnzipper unzipper;
    private NoarkClient noarkClient;
    private SvarInnFileFactory svarInnFileFactory;

    public SvarInnService(SvarInnClient svarInnClient, SvarInnFileDecryptor decryptor, SvarInnUnzipper unzipper, NoarkClient noarkClient) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
        this.unzipper = unzipper;
        this.noarkClient = noarkClient;
        svarInnFileFactory = new SvarInnFileFactory();

    }

    @Override
    public void downloadFiles() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        if (!forsendelses.isEmpty()) {
            Audit.info(String.format("%d new messages in FIKS", forsendelses.size()));
        }
        for(Forsendelse forsendelse : forsendelses) {

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

            if (!validateRequiredFields(forsendelse, files)) {
                continue;
            }

            final SvarInnMessage message = new SvarInnMessage(forsendelse, files);

            final EDUCore eduCore = message.toEduCore();
            PutMessageRequestType putMessage = EDUCoreFactory.createPutMessageFromCore(eduCore);

            final PutMessageResponseType putMessageResponseType = noarkClient.sendEduMelding(putMessage);

            if ("OK".equals(putMessageResponseType.getResult().getType())) {
                Audit.info("Message successfully forwarded");
                svarInnClient.confirmMessage(forsendelse.getId());
            } else if ("WARNING".equals(putMessageResponseType.getResult().getType())) {
                Audit.info("Archive system responded with warning");
            } else {
                Audit.error("New message failed");
            }
        }
    }

    private boolean validateRequiredFields(Forsendelse forsendelse, List<SvarInnFile> files) {
        Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();

        SvarInnFieldValidator validator = SvarInnFieldValidator.validator()
                .addField(metadata.getTittel(), "jpInnhold")
                .addField(forsendelse.getMottaker().getOrgnr(), "receiver: orgnr")
                .addField(forsendelse.getSvarSendesTil().getOrgnr(), "sender: orgnr")
                .addField(forsendelse.getSvarSendesTil().getNavn(), "sender: name");
        files.stream().forEach(f ->
            validator.addField(f.getMediaType().toString(), "veDokformat") // veDokformat
            .addField(f.getFilnavn(), "dbTittel") // dbTittel
        );

        if (!validator.getMissing().isEmpty()) {
            String missingFields = validator.getMissing().stream().reduce((a, b) -> a + ", " + b).get();
            Audit.error(String.format("Message with id=%s has the following missing field(s): %s",
                    forsendelse.getId(), missingFields));
            return false;
        }

        return true;
    }

}
