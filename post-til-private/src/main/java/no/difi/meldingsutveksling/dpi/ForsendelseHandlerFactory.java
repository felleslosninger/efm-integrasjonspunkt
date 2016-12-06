package no.difi.meldingsutveksling.dpi;

import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.forsendelse.DigitalForsendelseHandler;
import no.difi.meldingsutveksling.dpi.forsendelse.PrintForsendelseHandler;

class ForsendelseHandlerFactory {
    DigitalPostInnbyggerConfig config;

    ForsendelseHandlerFactory(DigitalPostInnbyggerConfig config) {
        this.config = config;
    }

    public ForsendelseBuilderHandler create(MeldingsformidlerRequest request) throws MeldingsformidlerException {
        if (!request.isPrintProvider()) {
            return new DigitalForsendelseHandler(config);
        } else {
            if(config.getFeature().isEnablePrint()) {
                return new PrintForsendelseHandler(config);
            } else {
                throw new MeldingsformidlerException("Print is disabled, unable to send message");
            }

        }
    }
}
