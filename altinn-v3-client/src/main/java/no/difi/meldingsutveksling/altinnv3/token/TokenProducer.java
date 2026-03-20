package no.difi.meldingsutveksling.altinnv3.token;

import java.util.List;

public interface TokenProducer {
    String produceToken(List<String> scopes);
}
