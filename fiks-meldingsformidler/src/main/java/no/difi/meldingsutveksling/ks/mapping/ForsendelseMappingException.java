package no.difi.meldingsutveksling.ks.mapping;

import javax.xml.datatype.DatatypeConfigurationException;

public class ForsendelseMappingException extends RuntimeException {

    public ForsendelseMappingException(String s, DatatypeConfigurationException e) {
        super(s, e);
    }
}
