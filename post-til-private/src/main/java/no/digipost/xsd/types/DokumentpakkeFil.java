package no.digipost.xsd.types;

import no.difi.begrep.sdp.schema_v10.SDPTittel;

public interface DokumentpakkeFil {

    String getHref();

    String getMime();

    default SDPTittel getTittel() {
        return null;
    }

}
