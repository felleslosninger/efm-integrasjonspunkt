
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Smsvarsel {

    private String mobiltelefonnummer;
    private String varslingstekst;
    private List<Integer> repetisjoner = new ArrayList<>();
}
