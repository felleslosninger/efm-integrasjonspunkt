package no.difi.meldingsutveksling.nextmove;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;

@UtilityClass
class SubscriptionTestData {

    static Subscription incomingMessages() {
        Subscription spy = spy(incomingMessagesInput());
        given(spy.getId()).willReturn(84L);
        return spy;
    }

    static Subscription incomingMessagesInput() {
        return new Subscription()
                .setName("Incoming messages")
                .setPushEndpoint("http://my.pushendpoint.no/messages/incoming")
                .setResource("messages")
                .setEvent("status")
                .setFilter("status=INNKOMMENDE_MOTTAT&direction=INCOMING");
    }

    static Subscription failedMessages() {
        Subscription spy = spy(new Subscription()
                .setName("Incoming messages")
                .setPushEndpoint("http://my.pushendpoint.no/messages/incoming")
                .setResource("messages")
                .setEvent("status")
                .setFilter("status=FEIL,LEVETID_UTLOPT"));
        given(spy.getId()).willReturn(32L);
        return spy;
    }
}
