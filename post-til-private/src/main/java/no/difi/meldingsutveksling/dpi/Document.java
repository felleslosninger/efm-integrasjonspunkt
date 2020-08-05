package no.difi.meldingsutveksling.dpi;

import lombok.Data;
import no.difi.sdp.client2.domain.MetadataDokument;

@Data
public class Document {

    private final byte[] contents;
    private final String mimeType;
    private final String fileName;
    private final String title;
    private MetadataDokument metadataDokument;

}
