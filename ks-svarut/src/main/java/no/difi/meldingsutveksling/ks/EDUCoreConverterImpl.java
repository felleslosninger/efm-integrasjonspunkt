package no.difi.meldingsutveksling.ks;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.ks.mapping.HandlerFactory;

import java.security.cert.X509Certificate;
import java.util.List;

public class EDUCoreConverterImpl implements EDUCoreConverter {
    HandlerFactory handlerFactory;

    public EDUCoreConverterImpl(HandlerFactory handlerFactory) {
        this.handlerFactory = handlerFactory;
    }

    @Override
    public Forsendelse convert(EDUCore domainMessage, X509Certificate certificate) {
        final List<Handler<Forsendelse.Builder>> handlers = handlerFactory.createHandlers(domainMessage, certificate);
        Forsendelse.Builder forsendelse = Forsendelse.builder();
        handlers.forEach(handler -> handler.map(forsendelse));
        return forsendelse.build();
    }
}
