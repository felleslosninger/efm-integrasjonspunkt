package no.difi.meldingsutveksling.noarkexchange;

public class InvalidReceiverException extends IllegalArgumentException {

    /**
     *
     */
    private static final long serialVersionUID = 3051666224365966424L;

    public InvalidReceiverException(Exception e) {
        super(e);
    }

    public InvalidReceiverException(String s) {
        super(s);
    }
}
