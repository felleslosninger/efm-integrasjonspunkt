package no.difi.meldingsutveksling.nextmove;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.NextMoveConsts;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.api.NextMoveQueue;
import no.difi.meldingsutveksling.fiks.svarinn.SvarInnPackage;
import no.difi.meldingsutveksling.ks.svarinn.Forsendelse;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnService;
import no.difi.meldingsutveksling.pipes.PromiseMaker;
import org.slf4j.MDC;

import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
public class SvarInnNextMoveForwarder implements Consumer<Forsendelse> {

    private final SvarInnNextMoveConverter svarInnNextMoveConverter;
    private final SvarInnService svarInnService;
    private final NextMoveQueue nextMoveQueue;
    private final PromiseMaker promiseMaker;

    @Override
    public void accept(Forsendelse forsendelse) {
        MDC.put(NextMoveConsts.CORRELATION_ID, forsendelse.getId());
        promiseMaker.promise(reject -> {
            SvarInnPackage svarInnPackage = svarInnNextMoveConverter.convert(forsendelse, reject);
            nextMoveQueue.enqueueIncomingMessage(svarInnPackage.getSbd(), ServiceIdentifier.DPF, svarInnPackage.getAsic());
            return null;
        }).await();
        svarInnService.confirmMessage(forsendelse.getId());
    }
}
