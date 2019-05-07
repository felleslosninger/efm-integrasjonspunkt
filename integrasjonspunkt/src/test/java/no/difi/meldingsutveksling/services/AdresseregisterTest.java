package no.difi.meldingsutveksling.services;

import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.noarkexchange.MessageException;
import no.difi.meldingsutveksling.noarkexchange.StatusMessage;
import no.difi.meldingsutveksling.noarkexchange.TestConstants;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookupException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AdresseregisterTest {

    Adresseregister adresseregister;
    public static final String SENDER_PARTY_NUMBER = "910075918";
    public static final String RECIEVER_PARTY_NUMBER = "910077473";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock(lenient = true)
    private ServiceRegistryLookup serviceRegistryLookup;

    @Mock
    private StandardBusinessDocument sbdMock;
    private String emptyCertificate = "";

    @Before
    public void setup() throws ServiceRegistryLookupException {
        adresseregister = new Adresseregister(serviceRegistryLookup);
        when(sbdMock.getSenderIdentifier()).thenReturn(SENDER_PARTY_NUMBER);
        when(sbdMock.getReceiverIdentifier()).thenReturn(RECIEVER_PARTY_NUMBER);
        when(serviceRegistryLookup.getServiceRecord(RECIEVER_PARTY_NUMBER)).thenReturn(new ServiceRecord(null, SENDER_PARTY_NUMBER, TestConstants.certificate, "http://localhost:123"));
        when(serviceRegistryLookup.getServiceRecord(SENDER_PARTY_NUMBER)).thenReturn(new ServiceRecord(null, SENDER_PARTY_NUMBER, TestConstants.certificate, "http://localhost:123"));
    }

    @Test
    public void senderCertificateIsMissing() throws Exception {
        expectedException.expect(MessageException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_SENDER_CERTIFICATE));
        when(serviceRegistryLookup.getServiceRecord(SENDER_PARTY_NUMBER)).thenReturn(new ServiceRecord(null, SENDER_PARTY_NUMBER, emptyCertificate, "http://localhost:123"));


        adresseregister.validateCertificates(sbdMock);

    }

    @Test
    public void recieverCertificateIsInValid() throws Exception {
        expectedException.expect(MessageException.class);
        expectedException.expect(new StatusMatches(StatusMessage.MISSING_RECIEVER_CERTIFICATE));
        when(serviceRegistryLookup.getServiceRecord(RECIEVER_PARTY_NUMBER)).thenReturn(new ServiceRecord(null, RECIEVER_PARTY_NUMBER, emptyCertificate, "http://localhost:123"));

        adresseregister.validateCertificates(sbdMock);
    }

    @Test
    public void certificatesAreValid() throws MessageException, ServiceRegistryLookupException {
        adresseregister.validateCertificates(sbdMock);
        when(serviceRegistryLookup.getServiceRecord(RECIEVER_PARTY_NUMBER)).thenReturn(new ServiceRecord(null, SENDER_PARTY_NUMBER, TestConstants.certificate, "http://localhost:123"));
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