package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.fiks.svarinn.SvarInnPackage;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import org.slf4j.MDC;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class SvarInnNextMoveForwarder implements Consumer<Forsendelse> {

    private final SvarInnNextMoveConverter svarInnNextMoveConverter;
    private final SvarInnService svarInnService;
    private final NextMoveQueue nextMoveQueue;

    @Override
    public void accept(Forsendelse forsendelse) {
        MDC.put(NextMoveConsts.CORRELATION_ID, forsendelse.getId());
        SvarInnPackage svarInnPackage = svarInnNextMoveConverter.convert(forsendelse);
        nextMoveQueue.enqueueIncomingMessage(svarInnPackage.getSbd(), ServiceIdentifier.DPF, svarInnPackage.getAsicStream());
        svarInnService.confirmMessage(forsendelse);
    }
}
