package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import org.springframework.core.io.Resource;

import java.util.Map;

@RequiredArgsConstructor
public class CreateStandardBusinessDocumentJWT {

    private final StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer;
    private final CreateJWT createJWT;

    public String createStandardBusinessDocumentJWT(StandardBusinessDocument standardBusinessDocument, Resource cmsEncryptedAsice, String maskinportenToken) {
        Map<String, Object> finalizedSBD = standBusinessDocumentJsonFinalizer.getFinalizedStandardBusinessDocumentAsJson(standardBusinessDocument,
                cmsEncryptedAsice,
                maskinportenToken);

        return createJWT.createJWT(finalizedSBD);
    }
}