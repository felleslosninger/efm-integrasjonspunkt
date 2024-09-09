package no.difi.meldingsutveksling.nextmove;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = false)
public class PostAddress implements Serializable {

    @NotNull
    private String navn;
    @NotNull
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String adresselinje4;
    @NotNull
    private String postnummer;
    @NotNull
    private String poststed;
    @NotNull
    private String land;
    private String landkode;
}
