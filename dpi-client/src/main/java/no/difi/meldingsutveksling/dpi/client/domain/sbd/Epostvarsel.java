
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Epostvarsel {

    private String epostadresse;
    private String varslingstekst;
    private List<Integer> repetisjoner = new ArrayList<>();
}
