package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Value;
import no.difi.meldingsutveksling.domain.StreamedFile;

import java.io.InputStream;

@Value(staticConstructor = "of")
public class SvarInnStreamedFile implements StreamedFile {

    private final String fileName;
    private final InputStream inputStream;
    private final String mimeType;
}
