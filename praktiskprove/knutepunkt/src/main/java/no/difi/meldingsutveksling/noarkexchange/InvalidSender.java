package no.difi.meldingsutveksling.noarkexchange;

public class InvalidSender extends IllegalArgumentException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6592654538686548520L;
    public InvalidSender(Exception e) {
        super(e);
    }
}
