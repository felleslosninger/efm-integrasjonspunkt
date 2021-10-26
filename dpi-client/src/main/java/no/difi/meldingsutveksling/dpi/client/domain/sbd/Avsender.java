
package no.difi.meldingsutveksling.dpi.client.domain.sbd;

import lombok.Data;

@Data
public class Avsender {

    private Identifikator virksomhetsidentifikator;
    private String avsenderidentifikator;

    /**
     * Invoice reference
     */
    private String fakturaReferanse;
}
