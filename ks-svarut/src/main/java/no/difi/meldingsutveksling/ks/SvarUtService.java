package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SvarUtService {
    @Autowired
    public SvarUtWebServiceClient client;

    @Autowired
    public EDUCoreConverter messageConverter;

    public String send(EDUCore message) {
        final Forsendelse forsendelse = messageConverter.convert(message);

        final String forsendelseId = client.sendMessage(forsendelse);

        return forsendelseId;
    }
}
