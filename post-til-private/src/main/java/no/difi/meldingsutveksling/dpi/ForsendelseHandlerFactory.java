package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.forsendelse.DigitalForsendelseHandler;
import no.difi.meldingsutveksling.dpi.forsendelse.PrintForsendelseHandler;

public class ForsendelseHandlerFactory {
    DigitalPostInnbyggerConfig config;

    public ForsendelseHandlerFactory(DigitalPostInnbyggerConfig config) {
        this.config = config;
    }

    public ForsendelseBuilderHandler create(MeldingsformidlerRequest request) {
        if (!request.isPrintProvider()) {
            return new DigitalForsendelseHandler(config);
        } else {
            return new PrintForsendelseHandler(config);
        }
    }
}
