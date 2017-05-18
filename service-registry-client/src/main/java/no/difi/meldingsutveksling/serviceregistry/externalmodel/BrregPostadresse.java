package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import lombok.Data;

@Data
public class BrregPostadresse {

    private String adresse;
    private String postnummer;
    private String poststed;
    private String land;

    public BrregPostadresse() {
    }

    public BrregPostadresse(String adresse, String postnummer, String poststed, String land) {
        this.adresse = adresse;
        this.postnummer = postnummer;
        this.poststed = poststed;
        this.land = land;
    }
}
