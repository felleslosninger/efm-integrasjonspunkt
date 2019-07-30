package no.difi.meldingsutveksling.nextmove;

import com.google.common.collect.Sets;
import lombok.Data;

import javax.persistence.Embeddable;

@Data
@Embeddable
public class PostAddress {

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
        return (land == null || "".equals(land)) || Sets.newHashSet("NORGE", "NORWAY", "NO", "NOR").contains(land.toUpperCase());
    }
}
