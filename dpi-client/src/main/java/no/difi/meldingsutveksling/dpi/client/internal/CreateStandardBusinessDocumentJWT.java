package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;

import java.util.Map;

@RequiredArgsConstructor
public class CreateStandardBusinessDocumentJWT {

    private final StandBusinessDocumentJsonFinalizer standBusinessDocumentJsonFinalizer;
    private final CreateJWT createJWT;

    public String createStandardBusinessDocumentJWT(StandardBusinessDocument standardBusinessDocument, CmsEncryptedAsice cmsEncryptedAsice, String maskinportenToken) {
        Map<String, Object> finalizedSBD = standBusinessDocumentJsonFinalizer.getFinalizedStandardBusinessDocumentAsJson(standardBusinessDocument,
                cmsEncryptedAsice,
                maskinportenToken);

        return createJWT.createJWT(finalizedSBD);
    }
}