package no.difi.meldingsutveksling.nextmove;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DialogmeldingOut{
    private Notat notat;
    private String responsibleHealthcareProfessionalId;
    private String vedleggBeskrivelse;
    private Person patient;


}
