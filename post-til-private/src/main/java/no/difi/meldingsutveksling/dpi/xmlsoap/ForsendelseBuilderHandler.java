package no.difi.meldingsutveksling.dpi.xmlsoap;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;

public abstract class ForsendelseBuilderHandler {
    protected final DigitalPostInnbyggerConfig config;

    protected ForsendelseBuilderHandler(DigitalPostInnbyggerConfig config) {
        this.config = config;
    }

    public abstract Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke);
}
