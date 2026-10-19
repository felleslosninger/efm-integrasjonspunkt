package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Maps;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import no.idporten.validators.identifier.PersonIdentifier;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@NoArgsConstructor
@XmlRootElement(name = "dialogmelding", namespace = "urn:no:difi:meldingsutveksling:2.0")
public class DialogmeldingMessage extends BusinessMessageAsAttachment<DialogmeldingMessage> {

    public static final Set<String> MIME_TYPES = Set.of(
        MediaType.APPLICATION_PDF_VALUE, MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE);

    @Valid
    @NotNull
    private Pasient pasient;

    private Map<@NotNull String, @NotNull Metadata> metadataFiler = Maps.newHashMap();

    @Data
    public static class Pasient {

        @NotNull
        @PersonIdentifier
        private String fnr;

        @NotNull
        @NotEmpty
        private String fornavn;

        private String mellomnavn;

        @NotNull
        @NotEmpty
        private String etternavn;
    }

    @Data
    public static class Metadata {

        private Instant issueDate;
        private String description;
    }
}
