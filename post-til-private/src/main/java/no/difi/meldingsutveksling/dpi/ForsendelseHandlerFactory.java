package no.difi.meldingsutveksling.dpi;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.config.DigitalPostInnbyggerConfig;
import no.difi.meldingsutveksling.dpi.forsendelse.DigitalForsendelseHandler;
import no.difi.meldingsutveksling.dpi.forsendelse.PrintForsendelseHandler;

@RequiredArgsConstructor
public class ForsendelseHandlerFactory {

    private final DigitalPostInnbyggerConfig config;

    public ForsendelseBuilderHandler create(MeldingsformidlerRequest request) {
        if (request.isPrintProvider()) {
            return new PrintForsendelseHandler(config);
        } else {
            return new DigitalForsendelseHandler(config);
        }
    }
}
