package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.noarkexchange.altinn.DefaultDpePolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DefaultDpfPolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DefaultDpoPolling;

@RequiredArgsConstructor
public class SchedulingSteps {

    private final DefaultDpoPolling dpoPolling;
    private final DefaultDpePolling dpePolling;
    private final DefaultDpfPolling dpfPolling;

    @Given("^the application checks for new DPO messages$")
    public void theApplicationChecksForNewNextMoveDPOMessages() {
        dpoPolling.poll();
    }

    @Given("^the application checks for new DPF messages$")
    public void theApplicationChecksForNewNextMoveDPFMessages() {
        dpfPolling.poll();
    }

    @And("^the application checks for new DPE messages$")
    public void theApplicationChecksForNewNextMoveDPEMessages() {
        dpePolling.poll();
    }
}
