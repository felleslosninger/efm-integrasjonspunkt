package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
public class DpfPolling {

    private final IntegrasjonspunktProperties properties;
    private final SvarInnService svarInnService;
    private final SvarInnPutMessageForwarder svarInnPutMessageForwarder;
    private final SvarInnNextMoveForwarder svarInnNextMoveForwarder;

    public DpfPolling(IntegrasjonspunktProperties properties,
                      SvarInnService svarInnService,
                      ObjectProvider<SvarInnPutMessageForwarder> svarInnPutMessageForwarderProvider,
                      SvarInnNextMoveForwarder svarInnNextMoveForwarder) {
        this.properties = properties;
        this.svarInnService = svarInnService;
        this.svarInnPutMessageForwarder = svarInnPutMessageForwarderProvider.getIfAvailable();
        this.svarInnNextMoveForwarder = svarInnNextMoveForwarder;
    }

    public void poll() {
        log.debug("Checking for new FIKS messages");
        Consumer<Forsendelse> forwarder = getSvarInnForwarder();
        svarInnService.getForsendelser().forEach(forwarder);
    }

    private Consumer<Forsendelse> getSvarInnForwarder() {
        if (svarInnPutMessageForwarder != null && !isNullOrEmpty(properties.getNoarkSystem().getType())) {
            return svarInnPutMessageForwarder;
        }

        return svarInnNextMoveForwarder;
    }
}
