package no.difi.meldingsutveksling.ks.mapping;


import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.mapping.edu.MeldingTypeHandler;

import java.util.ArrayList;
import java.util.List;

public class HandlerFactory {
    private IntegrasjonspunktProperties properties;

    public HandlerFactory(IntegrasjonspunktProperties properties) {
        this.properties = properties;
    }

    public List<Handler<Forsendelse.Builder>> createHandlers(EDUCore eduCore) {
        List<Handler<Forsendelse.Builder>> handlers = new ArrayList<>();
        handlers.add(createEduHandlers(eduCore));
        handlers.add(createPropertiesHandler());
        return handlers;
    }

    HandlerCollection createEduHandlers(EDUCore eduCore) {
        HandlerCollection handler = new HandlerCollection();
        final MeldingTypeHandler meldingTypeHandler = new MeldingTypeHandler(
                eduCore.getPayloadAsMeldingType(),
                new FileTypeHandlerFactory(properties.getDps())
        );
        handler.handlers.add(meldingTypeHandler);
        return handler;
    }

    PropertiesHandler createPropertiesHandler() {
        return new PropertiesHandler(properties);
    }

}
