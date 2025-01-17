package no.difi.meldingsutveksling.ks.svarinn;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.logging.Audit;
import no.difi.move.common.io.pipe.Reject;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
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

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
@Component
public class SvarInnService {

    private final SvarInnClient svarInnClient;
    private final SvarInnFileDecryptor decryptor;
    private final IntegrasjonspunktProperties properties;

    public Stream<Document> getAttachments(Forsendelse forsendelse, Reject reject) {
        Resource encrypted = svarInnClient.downloadZipFile(forsendelse, reject);
        Resource decrypted = decryptor.decrypt(encrypted);
        return unzip(forsendelse, decrypted);
    }

    private Stream<Document> unzip(Forsendelse forsendelse, Resource decrypted) {
        try {
            Iterator<Document> sourceIterator = getDocumentIterator(forsendelse, decrypted.getInputStream());
            Iterable<Document> iterable = () -> sourceIterator;
            return StreamSupport.stream(iterable.spliterator(), false);
        } catch (IOException e) {
            throw new IllegalStateException("Could not unzip file!", e);
        }
    }

    @NotNull
    private Iterator<Document> getDocumentIterator(Forsendelse forsendelse, InputStream inputStream) {
        Map<String, String> mimeTypeMap = forsendelse.getFilmetadata()
                .stream()
                .collect(Collectors.toMap(Forsendelse.Filmetadata::getFilnavn, Forsendelse.Filmetadata::getMimetype));

        return new Iterator<Document>() {

            private final ZipInputStream stream = new ZipInputStream(inputStream);
            private ZipEntry entry;

            @Override
            @SneakyThrows
            public boolean hasNext() {
                if (entry == null) {
                    entry = stream.getNextEntry();
                }

                return entry != null;
            }

            @Override
            public Document next() {
                if (hasNext()) {
                    Document file = Document.builder()
                            .filename(entry.getName())
                            .mimeType(mimeTypeMap.get(entry.getName()))
                            .resource(getResource(stream))
                            .build();
                    entry = null;
                    return file;
                }

                throw new NoSuchElementException();
            }

            private Resource getResource(ZipInputStream inputStream) {
                return new InputStreamResource(StreamUtils.nonClosing(inputStream));
            }
        };
    }

    public List<Forsendelse> getForsendelser() {
        final List<Forsendelse> forsendelser = Lists.newArrayList();
        if (!isNullOrEmpty(properties.getFiks().getInn().getUsername())) {
            forsendelser.addAll(svarInnClient.checkForNewMessages(properties.getFiks().getInn().getOrgnr()));
        }
        properties.getFiks().getInn().getPaaVegneAv().keySet().forEach(orgnr -> {
            forsendelser.addAll(svarInnClient.checkForNewMessages(orgnr));
        });

        if (!forsendelser.isEmpty()) {
            Audit.info("%d new messages in FIKS".formatted(forsendelser.size()));
        }
        return forsendelser;
    }

    public void confirmMessage(Forsendelse forsendelse) {
        svarInnClient.confirmMessage(forsendelse);
    }

    public void setErrorStateForMessage(Forsendelse forsendelse, String errorMsg) {
        svarInnClient.setErrorStateForMessage(forsendelse, errorMsg);
    }
}
