package no.difi.meldingsutveksling.dpi.forsendelse;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.ForsendelseBuilderHandler;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.sdp.client2.domain.Dokumentpakke;
import no.difi.sdp.client2.domain.Forsendelse;

public class PrintForsendelseHandler extends ForsendelseBuilderHandler {
    public PrintForsendelseHandler(DigitalPostInnbyggerConfig config) {
        super(config);
    }

    @Override
    public Forsendelse.Builder handle(MeldingsformidlerRequest request, Dokumentpakke dokumentpakke) {
        return null;
    }
}
