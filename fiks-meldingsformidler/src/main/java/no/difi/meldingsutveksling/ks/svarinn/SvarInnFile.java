package no.difi.meldingsutveksling.ks.svarinn;


import lombok.NonNull;
import lombok.Value;
import org.springframework.http.MediaType;

/**
 * Represents a file downloaded from SvarInn: contents in bytes along with MediaType as returned by Http client.
 */
@Value
public class SvarInnFile {
    @NonNull
    private final String filnavn;
    @NonNull
    private final MediaType mediaType;
    @NonNull
    private final byte[] contents;
}
