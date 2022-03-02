package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;

@Data
public class Virksomhetmottaker {

    private Identifikator virksomhetsidentifikator;
    private String motakeridentifikator;
}
