package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.ResourceUtils;
import no.difi.meldingsutveksling.dpi.MetadataDocument;
import no.difi.sdp.client2.domain.MetadataDokument;

public class MetadataDocumentConverter {

    public MetadataDokument toMetadataDokument(MetadataDocument document) {
        if (document == null) {
            return null;
        }

        return new MetadataDokument(
                document.getFilename(),
                document.getMimeType(),
                ResourceUtils.toByteArray(document.getResource())
        );
    }
}
