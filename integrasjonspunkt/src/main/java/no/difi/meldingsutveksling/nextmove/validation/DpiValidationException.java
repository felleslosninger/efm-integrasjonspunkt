package no.difi.meldingsutveksling.nextmove.validation;

import no.difi.meldingsutveksling.nextmove.NextMoveException;

public class DpiValidationException extends NextMoveException {
    public DpiValidationException(Exception e) {
        super(e);
    }

    public DpiValidationException(String s) {
        super(s);
    }

    public DpiValidationException(String s, Exception e) {
        super(s, e);
    }
}
