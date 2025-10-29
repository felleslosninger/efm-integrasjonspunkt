package no.difi.meldingsutveksling.nextmove.dph;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.HealthcareValidationException;
import no.difi.meldingsutveksling.nextmove.HealthcareTestData;
import no.difi.meldingsutveksling.nextmove.nhn.HealthcareRoutingService;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.*;

@ExtendWith(MockitoExtension.class)
public class HealthcareRoutingServiceTest {


    @Mock
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock
    private ServiceRecordProvider serviceRecordProvider;

    @InjectMocks
    private HealthcareRoutingService healthcareRoutingService;

    @BeforeEach
    public void setup() {
        Mockito.clearInvocations();
    }


  @Test
  public void whenIdentifierIsNotNHN_ThrowsValidationException() {
        StandardBusinessDocument sbd = HealthcareTestData.dialgmelding();
        sbd.getScopes().remove(sbd.getScopes().stream().filter(t -> Objects.equals(t.getType(), ScopeType.RECEIVER_HERID2.getFullname())).findFirst().orElse(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> healthcareRoutingService.validateAndApply(sbd),
            "Dialogmelding requires Receiver HerdId level 2 to be present"
        );
    }


    @Test
    public void whenRecieverIsNhn_verifyOrgnumMatchesSR() {
        StandardBusinessDocument sbd = HealthcareTestData.dialgmelding();
        ServiceRecord recieverRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnReceiverIdentifier);

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);

        safelyRun(()->healthcareRoutingService.validateAndApply(sbd));

        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(spyRecord).getOrganisationNumber();

    }

    @Test
    public void whenRecieverIsNhn_and_orgnumVerificationFail_ValidationException() {
        StandardBusinessDocument sbd = dialgmelding();
        ServiceRecord recieverRecord =  HealthcareTestData.serviceRecord(Identifier.validNhnReceiverIdentifier.withIdentifier("invalid-identifier"));
        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        try {
            healthcareRoutingService.validateAndApply(sbd);
        }catch (HealthcareValidationException throwable) {

        } catch (Throwable e) {
            e.printStackTrace();
            Assertions.fail("It was supposed to throw HealthcareValidationException");
        }
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(spyRecord,Mockito.times(1)).getOrganisationNumber();

    }



    @Test
    public void whenMultitenancyIsFalse_SenderHerdIsVerified() {

        StandardBusinessDocument sbd = HealthcareTestData.dialgmelding();
        sbd.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID1.getFullname()).setInstanceIdentifier("8767123123"));

        ServiceRecord recieverRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setSenderHerId1("4443333"));

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);

        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        Assertions.assertEquals("Multitenancy not supported: Routing information in message does not match Adressregister information for herID14443333 and orgnum 920640818", e.getArgs()[0]);

        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.SENDER);
        Mockito.verify(spyRecord, Mockito.times(1)).getOrganisationNumber();

        Mockito.clearInvocations(serviceRecordProvider,spyRecord);
        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setSenderHerId1(HealthcareTestData.Identifier.validNhnSenderIdentifier.getHerId1()));
        e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        Assertions.assertEquals("Multitenancy not supported: Routing information in message does not match Adressregister information for HerID level 1 8767123123 and orgnum 920640818", e.getArgs()[0]);

        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.SENDER);
        Mockito.verify(spyRecord, Mockito.times(1)).getOrganisationNumber();


    }

    @Test
    public void whenMultitenancyIsFalse_SenderOrgnumberIsVerified() {

        StandardBusinessDocument sbd = HealthcareTestData.dialgmelding().setSenderIdentifier(HealthcareTestData.Identifier.validNhnSenderIdentifier.withIdentifier("invalidIdentifier"));
        sbd.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID1.getFullname()).setInstanceIdentifier(HealthcareTestData.Identifier.validNhnSenderIdentifier.getHerId1()));

        ServiceRecord recieverRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setSenderHerId1(HealthcareTestData.Identifier.validNhnSenderIdentifier.getHerId1()));

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);

        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        Assertions.assertEquals("Multitenancy is not supported. Sender organisation number:invalidIdentifier is not registered in AR ", e.getArgs()[0]);

        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.SENDER);
        Mockito.verify(spyRecord, Mockito.times(1)).getOrganisationNumber();

    }

    @Test
    public void  MultitenancyIsEnabled_ArOrgnummerShouldBeWhitelisted() {
        final StandardBusinessDocument sbd = HealthcareTestData.dialgmelding();
        ServiceRecord recieverRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnSenderIdentifier);

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);


        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(true).setWhitelistOrgnum(List.of("is-not-whitelisted")));
        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        assertEquals("Sender not allowed 920640818",e.getArgs()[0]);
        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(true).setWhitelistOrgnum(List.of(HealthcareTestData.Identifier.validNhnSenderIdentifier.getIdentifier())));
        sbd.setSenderIdentifier(sbd.getSenderIdentifier().cast(NhnIdentifier.class).withIdentifier("invalidIdentifier"));
        e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        assertEquals("Sender information does not match Adressregister information.",e.getArgs()[0]);

    }

    @Test
    public void whenStandardBusinessDocumentPassValidation_allScopeElementsArePresent() {
        final StandardBusinessDocument sbd = HealthcareTestData.dialgmelding();

        ServiceRecord recieverRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = HealthcareTestData.serviceRecord(HealthcareTestData.Identifier.validNhnSenderIdentifier);

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);

        Mockito.lenient()
            .when(integrasjonspunktProperties.getDph()).
            thenReturn(new IntegrasjonspunktProperties.DphConfig()
                .setAllowMultitenancy(true)
                .setWhitelistOrgnum(List.of(Identifier.validNhnSenderIdentifier.getIdentifier())));


        healthcareRoutingService.validateAndApply(sbd);

        assertTrue(sbd.getScope(ScopeType.SENDER_HERID1).isPresent());
        assertTrue(sbd.getScope(ScopeType.SENDER_HERID2).isPresent());
        assertTrue(sbd.getScope(ScopeType.RECEIVER_HERID1).isPresent());
        assertTrue(sbd.getScope(ScopeType.RECEIVER_HERID2).isPresent());
        assertEquals(Identifier.validNhnSenderIdentifier.getHerId1(), sbd.getScope(ScopeType.SENDER_HERID1).get().getInstanceIdentifier());
        assertEquals(Identifier.validNhnSenderIdentifier.getHerId2(), sbd.getScope(ScopeType.SENDER_HERID2).get().getInstanceIdentifier());
        assertEquals(Identifier.validNhnReceiverIdentifier.getHerId1(), sbd.getScope(ScopeType.RECEIVER_HERID1).get().getInstanceIdentifier());
        assertEquals(Identifier.validNhnReceiverIdentifier.getHerId2(), sbd.getScope(ScopeType.RECEIVER_HERID2).get().getInstanceIdentifier());


    }




    private void safelyRun(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {

        }
    }


}
