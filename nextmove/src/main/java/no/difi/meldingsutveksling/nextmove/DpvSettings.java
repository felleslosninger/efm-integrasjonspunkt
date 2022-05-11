package no.difi.meldingsutveksling.nextmove;

import lombok.Data;

@Data
public class DpvSettings {
    private DpvVarselType varselType;
    private DpvVarselTransportType varselTransportType;
    private String varselTekst;
    private String taushetsbelagtVarselTekst;
    private Integer dagerTilSvarfrist;
}
