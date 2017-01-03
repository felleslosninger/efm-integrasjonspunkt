package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.mapping.properties.AvgivendeSystemHandler;
import no.difi.meldingsutveksling.ks.mapping.properties.SvarUtConfigHandler;

import java.util.HashSet;
import java.util.Set;

public class PropertiesHandler implements Handler<Forsendelse.Builder> {
    private Set<Handler<Forsendelse.Builder>> handlers;

    public PropertiesHandler(IntegrasjonspunktProperties properties) {
        this();
        this.add(new AvgivendeSystemHandler(properties));
        this.add(new SvarUtConfigHandler(properties));
    }

    private PropertiesHandler() {
        this.handlers = new HashSet<>();
    }

    private PropertiesHandler add(Handler<Forsendelse.Builder> handler) {
        handlers.add(handler);
        return this;
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        for (Handler<Forsendelse.Builder> handler : handlers) {
            builder = handler.map(builder);
        }
        return builder;
    }

}
