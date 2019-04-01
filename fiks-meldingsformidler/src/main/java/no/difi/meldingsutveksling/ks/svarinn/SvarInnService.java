package no.difi.meldingsutveksling.ks.svarinn;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.logging.Audit;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.String.format;

@RequiredArgsConstructor
public class SvarInnService {

    private final SvarInnClient svarInnClient;
    private final SvarInnFileDecryptor decryptor;

    public Stream<SvarInnStreamedFile> getAttachments(Forsendelse forsendelse) {
        InputStream encrypted = svarInnClient.downloadZipFile(forsendelse);
        InputStream decrypted = decryptor.decryptCMSStreamed(encrypted);
        return unzip(forsendelse, decrypted);
    }

    private Stream<SvarInnStreamedFile> unzip(Forsendelse forsendelse, InputStream decrypted) {
        Map<String, String> mimeTypeMap = forsendelse.getFilmetadata()
                .stream()
                .collect(Collectors.toMap(p -> p.get("filnavn"), p -> p.get("mimetype")));

        Iterator<SvarInnStreamedFile> sourceIterator = new Iterator<SvarInnStreamedFile>() {

            private final ZipInputStream stream = new ZipInputStream(decrypted);
            private ZipEntry entry;

            @Override
            @SneakyThrows
            public boolean hasNext() {
                if (entry == null) {
                    entry = stream.getNextEntry();
                }

                if (entry == null) {
                    stream.close();
                    return false;
                }

                return true;
            }

            @Override
            public SvarInnStreamedFile next() {
                if (hasNext()) {
                    SvarInnStreamedFile file = SvarInnStreamedFile.of(
                            entry.getName(),
                            stream,
                            mimeTypeMap.get(entry.getName()));
                    entry = null;
                    return file;
                }

                throw new NoSuchElementException();
            }
        };

        Iterable<SvarInnStreamedFile> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public List<Forsendelse> getForsendelser() {
        final List<Forsendelse> forsendelser = svarInnClient.checkForNewMessages();

        if (!forsendelser.isEmpty()) {
            Audit.info(format("%d new messages in FIKS", forsendelser.size()));
        }
        return forsendelser;
    }

    public void confirmMessage(String forsendelsesId) {
        svarInnClient.confirmMessage(forsendelsesId);
    }
}
