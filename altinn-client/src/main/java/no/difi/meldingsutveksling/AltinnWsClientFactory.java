package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.PromiseMaker;

@RequiredArgsConstructor
public class AltinnWsClientFactory {

    private final ApplicationContextHolder applicationContextHolder;
    private final AltinnWsConfigurationFactory altinnWsConfigurationFactory;
    private final Plumber plumber;
    private final PromiseMaker promiseMaker;

    public AltinnWsClient getAltinnWsClient() {
        AltinnWsConfiguration configuration = altinnWsConfigurationFactory.create();
        return new AltinnWsClient(
                configuration,
                applicationContextHolder.getApplicationContext(),
                plumber,
                promiseMaker);
    }
}
