package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Data
@Embeddable
public class DpiNotification implements Serializable {

    String epostTekst;
    String smsTekst;
}
