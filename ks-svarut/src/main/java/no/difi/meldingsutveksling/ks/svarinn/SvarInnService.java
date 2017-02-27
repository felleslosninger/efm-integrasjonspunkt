package no.difi.meldingsutveksling.ks.svarinn;

import no.difi.meldingsutveksling.core.EDUCore;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SvarInnService {

    private SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;
    private SvarInnUnzipper unzipper;
    private SvarInnFileFactory svarInnFileFactory;

    public SvarInnService(SvarInnClient svarInnClient, SvarInnFileDecryptor decryptor, SvarInnUnzipper unzipper) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
        this.unzipper = unzipper;
        svarInnFileFactory = new SvarInnFileFactory();

    }

    public void hentNyeMeldinger() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        forsendelses.forEach(forsendelse -> {
            final SvarInnFile svarInnFile = svarInnClient.downloadFile(forsendelse.getDownloadUrl());
            final byte[] decrypt = decryptor.decrypt(svarInnFile.getContents());
            final Map<String, byte[]> unzippedFile;
            try {
                unzippedFile = unzipper.unzip(decrypt);
            } catch (IOException e) {
                throw new SvarInnForsendelseException("Unable to unzip file", e);
            }
            // create SvarInnFile with unzipped file and correct mimetype
            final List<SvarInnFile> files = svarInnFileFactory.createFiles(forsendelse.getFilmetadata(), unzippedFile);

            SvarInnMessageFactory messageFactory = new SvarInnMessageFactory();
            final SvarInnMessage message = new SvarInnMessage(forsendelse, files);

            final EDUCore eduCore = message.toEduCore();
            // create SvarInnMessage <- forsendelse and SvarInnFile
            // mapToEduCore (forsendelse and svarInnFile)
            // enqueueToNoark
            // sendConfirm
        });
    }
}
