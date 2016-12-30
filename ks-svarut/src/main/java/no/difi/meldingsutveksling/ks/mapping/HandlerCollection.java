package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.ks.Forsendelse;

import java.util.ArrayList;
import java.util.List;

/**
 * Main mapper entrypoint for EDUCore (EDU payload)
 *
 * Used to map EDUCore to Forsendelse (SvarUt payload)
 */
public class HandlerCollection implements Handler<Forsendelse.Builder> {
    List<Handler<Forsendelse.Builder>> handlers = new ArrayList<>();

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        for (Handler<Forsendelse.Builder> handler : handlers) {
            builder = handler.map(builder);
        }
        return builder;
    }
}
