package no.difi.meldingsutveksling.ks.svarinn;


import lombok.Data;
import lombok.NonNull;
import org.springframework.http.MediaType;

/**
 * Represents a file downloaded from SvarInn: contents in bytes along with MediaType as returned by Http client.
 */
@Data
class SvarInnFile {
    @NonNull
    private String filnavn;
    @NonNull
    private MediaType mediaType;
    @NonNull
    private byte[] contents;
}
