package no.difi.meldingsutveksling.ks;

class SvarUtServiceException extends RuntimeException {
    SvarUtServiceException(String s, Exception e) {
        super(s, e);
    }
}
