package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;

@RequiredArgsConstructor
public class MessagePollingSteps {

    private final MessagePolling messagePolling;

    @Given("^the application checks for new Next Move DPO messages$")
    public void theApplicationChecksForNewNextMoveDPOMessages() {
        messagePolling.checkForNewMessages();
    }

    @Given("^the application checks for new Next Move DPF messages$")
    public void theApplicationChecksForNewNextMoveDPFMessages() {
        messagePolling.checkForFiksMessages();
    }

    @And("^the application checks for new Next Move DPE messages$")
    public void theApplicationChecksForNewNextMoveDPEMessages() {
        messagePolling.checkForNewNextMoveMessages();
    }
}
