package no.difi.meldingsutveksling.ks.mapping;


import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.mapping.edu.MeldingTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.ReceiverHandler;

import java.util.ArrayList;
import java.util.List;

public class HandlerFactory {
    private IntegrasjonspunktProperties properties;

    public HandlerFactory(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    public List<Handler<Forsendelse.Builder>> createHandlers(EDUCore eduCore) {
        List<Handler<Forsendelse.Builder>> handlers = new ArrayList<>();
        handlers.add(createMeldingHandlers(eduCore));
        handlers.add(createPropertiesHandler());
        handlers.add(new ReceiverHandler(eduCore));
        return handlers;
    }

    private HandlerCollection createMeldingHandlers(EDUCore eduCore) {
        HandlerCollection handler = new HandlerCollection();
        final MeldingTypeHandler meldingTypeHandler = new MeldingTypeHandler(
                eduCore.getPayloadAsMeldingType(),
                new FileTypeHandlerFactory(properties.getDps())
        );
        handler.handlers.add(meldingTypeHandler);
        return handler;
    }

    private PropertiesHandler createPropertiesHandler() {
        return new PropertiesHandler(properties);
    }

}
