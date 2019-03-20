package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.Given;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.altinn.MessagePolling;

@RequiredArgsConstructor
public class MessagePollingSteps {

    private final MessagePolling messagePolling;

    @Given("^the application checks for new Next Move DPO messages$")
    public void theApplicationChecksForNewNextMoveDPOMessages() throws MessageException {
        messagePolling.checkForNewMessages();
    }
}
