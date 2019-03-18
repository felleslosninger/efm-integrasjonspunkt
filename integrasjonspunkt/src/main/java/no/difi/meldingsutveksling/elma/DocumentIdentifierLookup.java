package no.difi.meldingsutveksling.elma;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import no.difi.vefa.peppol.common.model.DocumentTypeIdentifier;
import no.difi.vefa.peppol.common.model.ParticipantIdentifier;
import no.difi.vefa.peppol.lookup.LookupClient;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DocumentIdentifierLookup {

    private final LoadingCache<ParticipantIdentifier, List<DocumentTypeIdentifier>> cache;

    public DocumentIdentifierLookup(LookupClient lookupClient) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<ParticipantIdentifier, List<DocumentTypeIdentifier>>() {
                    @Override
                    public List<DocumentTypeIdentifier> load(ParticipantIdentifier key) throws Exception {
                        return lookupClient.getDocumentIdentifiers(key);
                    }
                });
    }

    public List<DocumentTypeIdentifier> getDocumentIdentifiers(ParticipantIdentifier participantIdentifier) {
        try {
            return cache.get(participantIdentifier);
        } catch (ExecutionException e) {
            log.warn("ELMA lookup of document identifiers failed for {}", participantIdentifier);
            return Collections.emptyList();
        }
    }
}
