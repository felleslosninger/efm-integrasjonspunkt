package no.difi.meldingsutveksling.ks;

public interface SvarUtWebServiceClient {
    String sendMessage(SvarUtRequest svarUtRequest);

    String getForsendelseId(String uri, String eksternRef);

    ForsendelseStatus getForsendelseStatus(String uri, String forsendesId);
}
