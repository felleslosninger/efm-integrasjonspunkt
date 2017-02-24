package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;

public class SvarInnService {

    private SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;
    private SvarInnUnzipper unzipper;

    public SvarInnService(SvarInnClient svarInnClient, SvarInnFileDecryptor decryptor, SvarInnUnzipper unzipper) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
        this.unzipper = unzipper;
    }

    public void hentNyeMeldinger() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        forsendelses.forEach(f -> {
            final SvarInnFile svarInnFile = svarInnClient.downloadFile(f.getDownloadUrl());
            final byte[] decrypt = decryptor.decrypt(svarInnFile.getContents());
            try {
                final List<SvarInnFile> unzip = unzipper.unzip(new SvarInnFile(MediaType.ALL, decrypt));
            } catch (IOException e) {
                throw new SvarInnForsendelseException("Unable to unzip file", e);
            }
            SvarInnMessageFactory messageFactory;
            // create SvarInnMessage <- forsendelse and SvarInnFile
            // mapToEduCore (f and svarInnFile)
            // enqueueToNoark
            // sendConfirm
        });
    }
}
