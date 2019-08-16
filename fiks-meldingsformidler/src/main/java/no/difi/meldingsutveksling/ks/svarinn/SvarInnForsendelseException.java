package no.difi.meldingsutveksling.ks.svarinn;

import java.io.IOException;

public class SvarInnForsendelseException extends RuntimeException {
    SvarInnForsendelseException(String s, IOException e) {
        super(s, e);
    }
}
