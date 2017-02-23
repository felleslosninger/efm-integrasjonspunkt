package no.difi.meldingsutveksling.ks;

public interface SvarUtWebServiceClient {
    ForsendelseStatus getForsendelseStatus(String uri, String forsendesId);

    String sendMessage(SvarUtRequest svarUtRequest);
}
