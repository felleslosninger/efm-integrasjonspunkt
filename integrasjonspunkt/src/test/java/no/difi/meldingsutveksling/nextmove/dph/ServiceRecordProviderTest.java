package no.difi.meldingsutveksling.nextmove.dph;


import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.IdentifierNotFoundException;
import no.difi.meldingsutveksling.exceptions.UnsupportedOperationStatusException;
import no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.serviceregistry.SRParameter;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.Identifier;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.createDialogMelding;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.dialgmelding;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ServiceRecordProviderTest {

    @Mock
    ServiceRegistryLookup serviceRegistryLookup;


    @InjectMocks
    ServiceRecordProvider serviceRecordProvider;

    @BeforeAll
    static void beforeAll() {
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(true);
    }

    @AfterAll
    static void afterAll() {
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(false);
    }

    @Test
    public void whenMessageIsNotDialogMessage_participantSenderNotSuppoerted() {
       StandardBusinessDocument sbd = StandardBusinessDocumentTestData.createSbd(StandardBusinessDocumentTestData.ARKIVMELDING_MESSAGE_DATA);
       assertThrows(UnsupportedOperationStatusException.class,()->serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER));

    }

    @Test
    public void whenHerId2IsNotSupplied_throwUnsupportedOperationException() {
        StandardBusinessDocument sbd = dialgmelding();

        assertTrue(sbd.getScope(ScopeType.SENDER_HERID2).isPresent());

        sbd.getScopes().remove(sbd.getScope(ScopeType.SENDER_HERID2).get());
        assertThrows(UnsupportedOperationStatusException.class, ()->serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER));

    }

    @Test
    public void whenSenderHerId2IsProvided_lookupByHerIdIsPerformed() {
        StandardBusinessDocument sbd = dialgmelding();

        ArgumentCaptor<SRParameter> capture = ArgumentCaptor.forClass(SRParameter.class);


        assertDoesNotThrow(()->{
            when(serviceRegistryLookup.getServiceRecord(capture.capture(), eq(sbd.getDocumentType()))).thenReturn(ServiceRecord.EMPTY);
            serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER);
        });

        SRParameter capturedParam = capture.getValue();
        assertEquals(Identifier.validNhnSenderIdentifier.getHerId2(), capturedParam.getIdentifier());

    }
    @Test
    public void whenDialogmeldingRecieverISFastlege_thenLookupByFnrIsPerformed(){
        StandardBusinessDocument sbd = createDialogMelding(Identifier.validNhnSenderIdentifier,Identifier.validFastlegeReceiverIdentifier);
        ArgumentCaptor<SRParameter> capture = ArgumentCaptor.forClass(SRParameter.class);


        assertDoesNotThrow(()->{
            when(serviceRegistryLookup.getServiceRecord(capture.capture(), eq(sbd.getDocumentType()))).thenReturn(ServiceRecord.EMPTY);
            serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        });
        SRParameter capturedParam = capture.getValue();
        assertEquals(Identifier.validFastlegeReceiverIdentifier.getIdentifier(), capturedParam.getIdentifier());

    }

    @Test
    public void whenDialogmeldingReceiverIsNhn_thenLookupByHerId2IsPerformed(){
        StandardBusinessDocument sbd = createDialogMelding(Identifier.validNhnSenderIdentifier,Identifier.validNhnReceiverIdentifier);
        ArgumentCaptor<SRParameter> capture = ArgumentCaptor.forClass(SRParameter.class);


        assertDoesNotThrow(()->{
            when(serviceRegistryLookup.getServiceRecord(capture.capture(), eq(sbd.getDocumentType()))).thenReturn(ServiceRecord.EMPTY);
            serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        });

        SRParameter capturedParam = capture.getValue();
        assertEquals(Identifier.validNhnReceiverIdentifier.getHerId2(), capturedParam.getIdentifier());

    }

    @Test
    public void whenDialogmeldingReceiverIsNhn_andHerId2NotProvided_thenUnsupportedOperationStatusException(){
        StandardBusinessDocument sbd = createDialogMelding(Identifier.validNhnSenderIdentifier,Identifier.validNhnReceiverIdentifier);

        assertTrue(sbd.getScope(ScopeType.RECEIVER_HERID2).isPresent());
        sbd.getScopes().remove(sbd.getScope(ScopeType.RECEIVER_HERID2).get());

        ArgumentCaptor<SRParameter> capture = ArgumentCaptor.forClass(SRParameter.class);

        IdentifierNotFoundException ex = assertThrows(IdentifierNotFoundException.class, ()->{
            serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER);
        });

        assertEquals("Missing valid identifier and HerID2 definition for DIALOGMELDING", ex.getArgs()[0]);

    }


}
