package no.difi.meldingsutveksling.dpi;

public class MeldingsformidlerException extends Exception {
    public MeldingsformidlerException(String s, Exception e) {
        super(s, e);
    }

    public MeldingsformidlerException(String s) {
        super(s);
    }
}
