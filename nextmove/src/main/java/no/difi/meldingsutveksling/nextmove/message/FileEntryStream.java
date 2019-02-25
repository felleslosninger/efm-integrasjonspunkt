package no.difi.meldingsutveksling.nextmove.message;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor(staticName = "of")
public class FileEntryStream {

    private InputStream inputStream;
    private long size;

}
