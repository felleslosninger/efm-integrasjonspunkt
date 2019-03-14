package no.difi.meldingsutveksling.cucumber;

import lombok.Value;
import no.difi.meldingsutveksling.domain.ByteArrayFile;

@Value
class Attachment implements ByteArrayFile {

    private final String fileName;
    private final String mimeType;
    private final byte[] bytes;
}
