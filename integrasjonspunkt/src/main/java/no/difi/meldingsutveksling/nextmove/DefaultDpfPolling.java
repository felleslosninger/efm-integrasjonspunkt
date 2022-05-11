package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.DpfPolling;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.noarkexchange.SvarInnPutMessageForwarder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
@Order
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
public class DefaultDpfPolling implements DpfPolling {

    private final IntegrasjonspunktProperties properties;
    private final SvarInnService svarInnService;
    private final SvarInnPutMessageForwarder svarInnPutMessageForwarder;
    private final SvarInnNextMoveForwarder svarInnNextMoveForwarder;

    public DefaultDpfPolling(IntegrasjonspunktProperties properties,
                             SvarInnService svarInnService,
                             ObjectProvider<SvarInnPutMessageForwarder> svarInnPutMessageForwarderProvider,
                             SvarInnNextMoveForwarder svarInnNextMoveForwarder) {
        this.properties = properties;
        this.svarInnService = svarInnService;
        this.svarInnPutMessageForwarder = svarInnPutMessageForwarderProvider.getIfAvailable();
        this.svarInnNextMoveForwarder = svarInnNextMoveForwarder;
    }

    @Override
    @Timed
    public void poll() {
        Consumer<Forsendelse> forwarder = getSvarInnForwarder();
        log.trace("Checking for new DPF messages using {}", forwarder.getClass().getName());
        svarInnService.getForsendelser().forEach(forwarder);
    }

    private Consumer<Forsendelse> getSvarInnForwarder() {
        if (svarInnPutMessageForwarder != null && !isNullOrEmpty(properties.getNoarkSystem().getType())) {
            return svarInnPutMessageForwarder;
        }

        return svarInnNextMoveForwarder;
    }
}

