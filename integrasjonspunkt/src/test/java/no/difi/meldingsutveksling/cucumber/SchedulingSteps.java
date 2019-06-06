package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpePolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpfPolling;
import no.difi.meldingsutveksling.noarkexchange.altinn.DpoPolling;

@RequiredArgsConstructor
public class SchedulingSteps {

    private final DpoPolling dpoPolling;
    private final DpePolling dpePolling;
    private final DpfPolling dpfPolling;

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
