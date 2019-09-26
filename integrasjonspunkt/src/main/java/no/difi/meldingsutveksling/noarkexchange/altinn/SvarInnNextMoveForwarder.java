package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.nextmove.NextMoveQueue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Slf4j
@Component
@ConditionalOnProperty(name = "difi.move.feature.enableDPF", havingValue = "true")
@RequiredArgsConstructor
public class SvarInnNextMoveForwarder implements Consumer<Forsendelse> {

    private final SvarInnNextMoveConverter svarInnNextMoveConverter;
    private final SvarInnService svarInnService;
    private final NextMoveQueue nextMoveQueue;

    @Override
    public void accept(Forsendelse forsendelse) {
        StandardBusinessDocument sbd = svarInnNextMoveConverter.convert(forsendelse);
        nextMoveQueue.enqueue(sbd, ServiceIdentifier.DPF);
        svarInnService.confirmMessage(forsendelse.getId());
    }
}
