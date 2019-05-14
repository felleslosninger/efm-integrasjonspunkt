package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;

@RequiredArgsConstructor
public class MessagePollingSteps {

    private final MessagePolling messagePolling;

    @Given("^the application checks for new DPO messages$")
    public void theApplicationChecksForNewNextMoveDPOMessages() {
        messagePolling.checkForNewAltinnMessages();
    }

    @Given("^the application checks for new DPF messages$")
    public void theApplicationChecksForNewNextMoveDPFMessages() {
        messagePolling.checkForFiksMessages();
    }

    @And("^the application checks for new DPE messages$")
    public void theApplicationChecksForNewNextMoveDPEMessages() {
        messagePolling.checkForNewEinnsynMessages();
    }
}
