package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;
import org.springframework.stereotype.Service;

@Service
public class SvarUtService {
    private EDUCoreConverter messageConverter;
    private SvarUtWebServiceClient client;

    public SvarUtService(EDUCoreConverter messageConverter, SvarUtWebServiceClient svarUtClient) {
        this.messageConverter = messageConverter;
        this.client = svarUtClient;
    }

    public String send(EDUCore message) {
        final Forsendelse forsendelse = messageConverter.convert(message);

        return client.sendMessage(forsendelse);
    }
}
