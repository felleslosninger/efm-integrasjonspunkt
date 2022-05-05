package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;

import lombok.Data;

@Data
public class Varslingfeiletkvittering extends AbstractKvittering {

    private Varslingskanal varslingskanal;
    private String beskrivelse;
}
