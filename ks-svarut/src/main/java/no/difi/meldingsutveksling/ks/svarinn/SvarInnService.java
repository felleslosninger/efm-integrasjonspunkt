package no.difi.meldingsutveksling.ks.svarinn;

import java.util.List;

public class SvarInnService {

    SvarInnClient svarInnClient;
    private SvarInnFileDecryptor decryptor;

    public SvarInnService(SvarInnClient svarInnClient, SvarInnFileDecryptor decryptor) {
        this.svarInnClient = svarInnClient;
        this.decryptor = decryptor;
    }

    public void hentNyeMeldinger() {
        final List<Forsendelse> forsendelses = svarInnClient.checkForNewMessages();
        forsendelses.forEach(f -> {
            final SvarInnFile svarInnFile = svarInnClient.downloadFile(f.getDownloadUrl());
            final byte[] decrypt = decryptor.decrypt(svarInnFile.getContents());
            // mapToEduCore (f and svarInnFile)
            // enqueueToNoark
            // sendConfirm
        });
    }
}
