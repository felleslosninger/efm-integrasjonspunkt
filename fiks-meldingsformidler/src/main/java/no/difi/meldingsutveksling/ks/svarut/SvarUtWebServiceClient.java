package no.difi.meldingsutveksling.ks.svarut;

import java.util.List;

public interface SvarUtWebServiceClient {
    String sendMessage(SvarUtRequest svarUtRequest);

    String getForsendelseId(String uri, String eksternRef);

    ForsendelseStatus getForsendelseStatus(String uri, String forsendesId);

    List<String> retreiveForsendelseTyper(String uri);
}
