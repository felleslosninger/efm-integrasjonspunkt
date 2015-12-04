package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StandardBusinessDocumentWrapper;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.virksert.client.VirksertClient;
import no.difi.virksert.client.VirksertClientException;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdresseregisterVirksertTest {

    AdresseregisterVirksert adresseregisterVirksert;
    public static final String SENDER_PARTY_NUMBER = "910075918";
    public static final String RECIEVER_PARTY_NUMBER = "910077473";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private VirksertClient virksertClient;

    @Mock
    private StandardBusinessDocumentWrapper documentWrapper;

    @Before
    public void setup() {
        adresseregisterVirksert = new AdresseregisterVirksert(virksertClient);
        when(documentWrapper.getSenderOrgNumber()).thenReturn(SENDER_PARTY_NUMBER);
        when(documentWrapper.getReceiverOrgNumber()).thenReturn(RECIEVER_PARTY_NUMBER);
    }

    @Test
    public void senderCertificateIsInvalid() throws Exception {
        expectedException.expect(MessageException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_SENDER_CERTIFICATE));
        when(virksertClient.fetch(SENDER_PARTY_NUMBER)).thenThrow(new VirksertClientException(""));


        adresseregisterVirksert.validateCertificates(documentWrapper);

    }

    @Test
    public void recieverCertificateIsValid() throws Exception {
        expectedException.expect(MessageException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_CERTIFICATE));
        when(virksertClient.fetch(RECIEVER_PARTY_NUMBER)).thenThrow(new VirksertClientException(""));

        adresseregisterVirksert.validateCertificates(documentWrapper);
    }

    @Test
    public void certificatesAreValid() throws MessageException {
        adresseregisterVirksert.validateCertificates(documentWrapper);
    }

    private class StatusMatches extends TypeSafeMatcher<MessageException> {
        private final StatusMessage expectedStatusMessage;

        public StatusMatches(StatusMessage expectedStatusMessage) {
            this.expectedStatusMessage = expectedStatusMessage;
        }

        @Override
        protected boolean matchesSafely(MessageException e) {
            return e.getStatusMessage() == expectedStatusMessage;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("with status ").appendValue(expectedStatusMessage);
        }

        @Override
        public void describeMismatchSafely(MessageException exception, Description mismatchDescription) {
            mismatchDescription.appendText("was ").appendValue(exception.getStatusMessage());
        }
    }
}