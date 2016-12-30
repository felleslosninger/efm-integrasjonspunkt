package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;
import org.springframework.stereotype.Service;

@Service
public class SvarUtService {
    public EDUCoreConverter messageConverter;
    public SvarUtWebServiceClient client;

    public SvarUtService(EDUCoreConverter messageConverter, SvarUtWebServiceClient client) {
        this.messageConverter = messageConverter;
        this.client = client;
    }

    public String send(EDUCore message) {
        final Forsendelse forsendelse = messageConverter.convert(message);

        final String forsendelseId = client.sendMessage(forsendelse);

        return forsendelseId;
    }
}
