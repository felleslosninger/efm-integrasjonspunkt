package no.difi.meldingsutveksling.ks.svarinn;

import com.google.common.collect.Sets;
import net.logstash.logback.marker.Markers;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.logging.Audit;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class SvarInnService {

    private SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;
    private SvarInnUnzipper unzipper;
    private SvarInnFileFactory svarInnFileFactory;
    private IntegrasjonspunktProperties properties;

    public SvarInnService(SvarInnClient svarInnClient,
                          SvarInnFileDecryptor decryptor,
                          SvarInnUnzipper unzipper,
                          IntegrasjonspunktProperties properties) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
        this.unzipper = unzipper;
        this.properties = properties;
        svarInnFileFactory = new SvarInnFileFactory();
    }

    public Set<SvarInnMessage> downloadFiles() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        if (!forsendelses.isEmpty()) {
            Audit.info(format("%d new messages in FIKS", forsendelses.size()));
        }
        Set<SvarInnMessage> messages = Sets.newHashSet();
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
            messages.add(message);
        }

        return messages;
    }

    public void confirmMessage(String forsendelsesId) {
        svarInnClient.confirmMessage(forsendelsesId);
    }

}
