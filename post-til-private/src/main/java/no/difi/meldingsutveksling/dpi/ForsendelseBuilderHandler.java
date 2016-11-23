package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;

public abstract class ForsendelseBuilderHandler {
    protected final DigitalPostInnbyggerConfig config;

    public ForsendelseBuilderHandler(DigitalPostInnbyggerConfig config) {
        this.config = config;
    }

    public abstract Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke);
}
