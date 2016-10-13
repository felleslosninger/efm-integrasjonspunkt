package no.difi.meldingsutveksling;

public enum ServiceIdentifier {
    EDU("EDU"),
    DPV("POST_VIRKSOMHET"),
    DPI("DPI");

    private final String fullname;

    ServiceIdentifier(String fullname) {
        this.fullname = fullname;
    }

    public String fullname() {
        return fullname;
    }
}
