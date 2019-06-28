package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.Embeddable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
@Embeddable
public class PostAddress {

    private static final Set<String> NORWAY_SET = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("NORGE", "NORWAY", "NO", "NOR")));

    private String navn;
    private String adresselinje1;
    private String adresselinje2;
    private String adresselinje3;
    private String adresselinje4;
    private String postnummer;
    private String poststed;
    private String landkode;
    private String land;

    public boolean isNorge() {
        return (land == null || "".equals(land)) || NORWAY_SET.contains(land.toUpperCase());
    }
}
