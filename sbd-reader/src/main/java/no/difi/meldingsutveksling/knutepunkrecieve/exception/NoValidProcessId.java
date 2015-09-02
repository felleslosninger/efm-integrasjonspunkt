package no.difi.meldingsutveksling.knutepunkrecieve.exception;

public class NoValidProcessId extends Exception {
	private static final long serialVersionUID = -5067625569091839518L;

	public NoValidProcessId() {
		super("The Standard Business Document does not have an acceptable PROCESSID");
	}
}
