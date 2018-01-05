package no.difi.meldingsutveksling.ks.svarut;

class SvarUtServiceException extends RuntimeException {
    SvarUtServiceException(String s) {
        super(s);
    }

    SvarUtServiceException(String s, Exception e) {
        super(s, e);
    }
}
