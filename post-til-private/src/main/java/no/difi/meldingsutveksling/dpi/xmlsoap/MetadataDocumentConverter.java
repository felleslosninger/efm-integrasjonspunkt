package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.ResourceUtils;
import no.difi.sdp.client2.domain.MetadataDokument;
import no.digdir.dpi.client.domain.MetadataDocument;
import org.springframework.core.io.ByteArrayResource;

public class MetadataDocumentConverter {

    public MetadataDocument toMetadataDocument(MetadataDokument dokument) {
        if (dokument == null) {
            return null;
        }

        return new MetadataDocument()
                .setResource(new ByteArrayResource(dokument.getBytes()))
                .setMimeType(dokument.getMimeType())
                .setFilename(dokument.getFileName());
    }

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
