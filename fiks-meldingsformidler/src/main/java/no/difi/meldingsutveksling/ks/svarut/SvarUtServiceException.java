package no.difi.meldingsutveksling.ks.svarut;

class SvarUtServiceException extends RuntimeException {
    SvarUtServiceException(String s, Exception e) {
        super(s, e);
    }
}
