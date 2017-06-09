package no.difi.meldingsutveksling.ks.svarut;

public interface SvarUtWebServiceClient {
    String sendMessage(SvarUtRequest svarUtRequest);

    String getForsendelseId(String uri, String eksternRef);

    ForsendelseStatus getForsendelseStatus(String uri, String forsendesId);
}
