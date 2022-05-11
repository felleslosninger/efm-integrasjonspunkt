package no.difi.meldingsutveksling.cucumber;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.ks.svarinn.SvarInnClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
public class SvarInnSteps {

    private final Holder<Message> messageInHolder;
    private final MockServerRestSteps mockServerRestSteps;
    private final SvarInnZipFactory svarInnZipFactory;
    private final SvarInnClient svarInnClient;

    @After
    public void after() {
        messageInHolder.reset();
    }

    @And("^Fiks prepares a message with the following body:$")
    public void fiksPreparesAMessageWithTheFollowingBody(String body) {
        messageInHolder.set(new Message().setBody(body).setServiceIdentifier(ServiceIdentifier.DPF));
    }

    @And("^Fiks has the message with conversationId=\"([^\"]*)\" available$")
    public void fiksHasTheMessageWithConversationIdAvailable(String id) {
        mockServerRestSteps.aRequestToWillRespondWithStatusAndTheFollowing(
                HttpMethod.GET.name(),
                svarInnClient.getRootUri() + "/mottaker/hentNyeForsendelser",
                HttpStatus.OK.value(),
                MediaType.APPLICATION_JSON_VALUE,
                messageInHolder.get().getBody()
        );


        mockServerRestSteps.aRequestToWillRespondWithStatus(
                HttpMethod.GET.name(),
                svarInnClient.getRootUri() + "/mottaker/forsendelse/" + id,
                HttpStatus.OK.value()
        );

        mockServerRestSteps.andTheFollowing(
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                svarInnZipFactory.createSvarInnZip(messageInHolder.get())
        );

        mockServerRestSteps.aRequestToWillRespondWithStatusAndTheFollowing(
                HttpMethod.POST.name(),
                svarInnClient.getRootUri() + "/kvitterMottak/forsendelse/" + id,
                HttpStatus.OK.value(),
                MediaType.APPLICATION_JSON_VALUE,
                ""
        );
    }
}
