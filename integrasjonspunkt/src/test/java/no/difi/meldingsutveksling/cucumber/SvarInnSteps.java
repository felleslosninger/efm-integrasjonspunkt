package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.After;
import cucumber.api.java.en.And;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
public class SvarInnSteps {

    private final Holder<Message> messageInHolder;
    private final MockServerRestSteps mockServerRestSteps;
    private final SvarInnZipFactory svarInnZipFactory;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Fiks prepares a message with the following body:$")
    public void fiksPreparesAMessageWithTheFollowingBody(String body) {
        messageInHolder.set(new Message().setBody(body));
    }

    @And("^Fiks has the message with conversationId=\"([^\"]*)\" available$")
    public void fiksHasTheMessageWithConversationIdAvailable(String id) {
        mockServerRestSteps.aRequestToWillRespondWithStatusAndTheFollowing(
                HttpMethod.GET.name(),
                "/mottaker/hentNyeForsendelser",
                HttpStatus.OK.value(),
                MediaType.APPLICATION_JSON_VALUE,
                messageInHolder.get().getBody()
        );

        mockServerRestSteps.aRequestToWillRespondWithStatus(
                HttpMethod.GET.name(),
                "/mottaker/forsendelse/" + id,
                HttpStatus.OK.value()
        );

        mockServerRestSteps.andTheFollowing(
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                svarInnZipFactory.createSvarInnZip(messageInHolder.get())
        );

        mockServerRestSteps.aRequestToWillRespondWithStatusAndTheFollowing(
                HttpMethod.POST.name(),
                "/kvitterMottak/forsendelse/" + id,
                HttpStatus.OK.value(),
                MediaType.APPLICATION_JSON_VALUE,
                ""
        );
    }
}
