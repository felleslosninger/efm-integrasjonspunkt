package no.difi.meldingsutveksling.ks.svarut.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.client.core.WebServiceTemplate;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
class SvarUtInternalWebServiceClientHolder implements SvarUtWebServiceTemplateSupplier {

    private final Map<String, SvarUtInternalWebServiceClient> clients;

    public SvarUtInternalWebServiceClient getClient(String orgnr) {
        if (!clients.containsKey(orgnr)) {
            throw new IllegalArgumentException("No SvarUt client configured for orgnr: " + orgnr);
        }
        return clients.get(orgnr);
    }

    @Override
    public WebServiceTemplate getWebServiceTemplate() {
        return clients.values().iterator().next().getWebServiceTemplate();
    }
}
