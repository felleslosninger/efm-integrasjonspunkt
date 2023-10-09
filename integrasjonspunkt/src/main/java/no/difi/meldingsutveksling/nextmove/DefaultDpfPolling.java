package no.difi.meldingsutveksling.nextmove;

import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.api.DpfPolling;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;

@Slf4j
@Order
@ConditionalOnProperty(name = "difi.move.fiks.inn.enable", havingValue = "true")
public class DefaultDpfPolling implements DpfPolling {

    private final SvarInnService svarInnService;
    private final SvarInnNextMoveForwarder svarInnNextMoveForwarder;

    public DefaultDpfPolling(SvarInnService svarInnService,
                             SvarInnNextMoveForwarder svarInnNextMoveForwarder) {
        this.svarInnService = svarInnService;
        this.svarInnNextMoveForwarder = svarInnNextMoveForwarder;
    }

    @Override
    @Timed
    public void poll() {
        svarInnService.getForsendelser().forEach(svarInnNextMoveForwarder);
    }

}

