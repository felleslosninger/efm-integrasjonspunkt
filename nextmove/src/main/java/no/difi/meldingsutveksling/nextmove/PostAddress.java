package no.difi.meldingsutveksling.nextmove;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = false)
public class PostAddress implements Serializable {

    private static final Set<String> NORWAY_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("NORGE", "NORWAY", "NO", "NOR")));

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

    @JsonIgnore
    public boolean isNorge() {
        return (land == null || "".equals(land)) || NORWAY_SET.contains(land.toUpperCase());
    }
}
