package no.difi.meldingsutveksling.noarkexchange;

public class InvalidSenderException extends IllegalArgumentException {

    /**
     *
     */
    private static final long serialVersionUID = 6592654538686548520L;

    public InvalidSenderException(Exception e) {
        super(e);
    }

    public InvalidSenderException(String s) {
        super(s);
    }
}
