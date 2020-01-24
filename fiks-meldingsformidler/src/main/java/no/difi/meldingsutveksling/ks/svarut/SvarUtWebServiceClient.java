package no.difi.meldingsutveksling.ks.svarut;

import java.util.List;
import java.util.Set;

public interface SvarUtWebServiceClient {
    String sendMessage(SvarUtRequest svarUtRequest);

    String getForsendelseId(String uri, String eksternRef);

    ForsendelseStatus getForsendelseStatus(String uri, String forsendesId);

    List<StatusResult> getForsendelseStatuser(String uri, Set<String> forsendelseIds);

    List<String> retreiveForsendelseTyper(String uri);
}
