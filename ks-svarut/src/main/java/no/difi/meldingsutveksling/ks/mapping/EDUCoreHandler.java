package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.Forsendelse;
import no.difi.meldingsutveksling.ks.mapping.edu.MeldingTypeHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Main mapper entrypoint for EDUCore (EDU payload)
 *
 * Used to map EDUCore to Forsendelse (SvarUt payload)
 */
public class EDUCoreHandler implements Handler<Forsendelse.Builder> {
    private EDUCore eduCore;
    List<Handler<Forsendelse.Builder>> handlers;

    public EDUCoreHandler(EDUCore eduCore) {
        this.eduCore = eduCore;
        handlers = new ArrayList<>();
        handlers.add(new MeldingTypeHandler(eduCore.getPayloadAsMeldingType()));
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        for (Handler<Forsendelse.Builder> handler : handlers) {
            builder = handler.map(builder);
        }
        return builder;
    }
}
