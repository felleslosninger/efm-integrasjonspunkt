package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class DpfPolling {

    private final IntegrasjonspunktProperties properties;
    private final SvarInnService svarInnService;
    private final SvarInnPutMessageForwarder svarInnEduCoreForwarder;
    private final SvarInnNextMoveForwarder svarInnNextMoveForwarder;

    void poll() {
        log.debug("Checking for new FIKS messages");
        Consumer<Forsendelse> forwarder = getSvarInnForwarder();
        svarInnService.getForsendelser().forEach(forwarder);
    }

    private Consumer<Forsendelse> getSvarInnForwarder() {
        if (properties.getNoarkSystem().isEnable()
                && !properties.getNoarkSystem().getEndpointURL().isEmpty()) {
            return svarInnEduCoreForwarder;
        }

        return svarInnNextMoveForwarder;
    }
}
