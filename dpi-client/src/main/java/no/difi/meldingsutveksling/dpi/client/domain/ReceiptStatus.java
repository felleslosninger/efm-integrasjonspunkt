package no.difi.meldingsutveksling.dpi.client.domain;

public enum ReceiptStatus {

    /*
     Forsendelse er markert som opprettet (men ikke sendt) i avsenders aksesspunkt
     */
    OPPRETTET,

    /*
     Forsendelse er markert som sendt OK til mottakers aksesspunkt
     */
    SENDT,

    /*
              orsendelse er markert som feilet i avsenderes aksesspunkt
     */
    FEILET
}
