package no.difi.meldingsutveksling.dpi.client.domain.messagetypes;


import no.difi.meldingsutveksling.dpi.client.domain.sbd.Virksomhetmottaker;

public interface Kvittering extends BusinessMessage, TidspunktHolder {

    Virksomhetmottaker getMottaker();
}
