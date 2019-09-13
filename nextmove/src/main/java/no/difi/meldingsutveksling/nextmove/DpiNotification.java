package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import java.io.Serializable;

@Data
public class DpiNotification implements Serializable {

    String epostTekst;
    String smsTekst;
}
