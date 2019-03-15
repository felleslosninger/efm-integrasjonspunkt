package no.difi.meldingsutveksling.cucumber;

import lombok.Data;
import no.difi.meldingsutveksling.domain.ByteArrayFile;

@Data
class Attachment implements ByteArrayFile {

    private String fileName;
    private String mimeType;
    private byte[] bytes;
}
