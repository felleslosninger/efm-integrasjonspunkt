package no.difi.meldingsutveksling.nextmove.dph;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.EncryptedBusinessMessage;
import no.difi.meldingsutveksling.domain.NhnIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.exceptions.HealthcareValidationException;
import no.difi.meldingsutveksling.nextmove.HealthcareTestData;
import no.difi.meldingsutveksling.nextmove.nhn.BusinessMessageEncryptionService;
import no.difi.meldingsutveksling.nextmove.nhn.HealthcareRoutingService;
import no.difi.meldingsutveksling.nextmove.v2.Participant;
import no.difi.meldingsutveksling.nextmove.v2.ServiceRecordProvider;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;

import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.Identifier;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.createDialogMelding;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.dialgmelding;
import static no.difi.meldingsutveksling.nextmove.HealthcareTestData.serviceRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class HealthcareRoutingServiceTest {

    @Mock
    private IntegrasjonspunktProperties integrasjonspunktProperties;

    @Mock
    private ServiceRecordProvider serviceRecordProvider;

    @Mock
    private BusinessMessageEncryptionService businessMessageEncryptionService;

    @InjectMocks
    private HealthcareRoutingService healthcareRoutingService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(healthcareRoutingService, "dphEnabled", true);
        Mockito.clearInvocations();
    }


  @Test
  public void whenSenderHerId2NotPresent_ThrowIllegalArgumentException() {
        StandardBusinessDocument sbd = dialgmelding();
        sbd.getScopes().remove(sbd.getScopes().stream().filter(t -> Objects.equals(t.getType(), ScopeType.SENDER_HERID2.getFullname())).findFirst().orElse(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> healthcareRoutingService.validateAndApply(sbd),
            "Dialogmelding requires Receiver HerdId level 2 to be present"
        );
    }

    @Test
    public void whenDphNotEnabled_ThrowIllegalArgumentException() {
        ReflectionTestUtils.setField(healthcareRoutingService, "dphEnabled", false);
        StandardBusinessDocument sbd = dialgmelding();
        HealthcareValidationException ex = assertThrows(
            HealthcareValidationException.class,
            () -> healthcareRoutingService.validateAndApply(sbd),
            "When Healthcare feature is disabled. Validation should fail."
        );

        assertEquals("Can not process request. Healthcare feature is disabled.",ex.getArgs()[0]);
    }



    @Test
    public void whenRecieverIsNhn_verifyOrgnumMatchesSR() {
        StandardBusinessDocument sbd = dialgmelding();
        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);

        safelyRun(()->healthcareRoutingService.validateAndApply(sbd));

        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(spyRecord,Mockito.times(1)).getOrganisationNumber();

    }

    @Test
    public void whenRecieverIsNhn_and_orgnumVerificationFail_ValidationException() {
        StandardBusinessDocument sbd = dialgmelding();
        ServiceRecord recieverRecord =  serviceRecord(Identifier.validNhnReceiverIdentifier.withIdentifier("invalid-identifier"));
        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        try {
            healthcareRoutingService.validateAndApply(sbd);
        }catch (HealthcareValidationException throwable) {

        } catch (Throwable e) {
            e.printStackTrace();
            fail("It was supposed to throw HealthcareValidationException");
        }
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(spyRecord,Mockito.times(1)).getOrganisationNumber();

    }

    @Test
    public void whenMultitenancyIsFalse_moreThanOneOrgnummerWhitelisted_thenValidationExceptioon() {
        List<String> WHITE_LISTED_ORGNUM = List.of("223423423","3232434");
        StandardBusinessDocument sbd = dialgmelding();
        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setWhitelistOrgnum(WHITE_LISTED_ORGNUM));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);
        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        assertEquals("Multitenancy configuration error. Only one organisation number should be whitelisted.",e.getArgs()[0]);
    }

    @Test
    public void whenMultitenancyFalse_and_SenderHerId2_Organization_is_notWhitelisted_ThrowValidationException() {
        List<String> WHITE_LISTED_ORGNUM = List.of("223423423");
        StandardBusinessDocument sbd = dialgmelding();

        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setWhitelistOrgnum(WHITE_LISTED_ORGNUM));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);
        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        assertEquals("Multitenancy is not supported. Sender organisation number:920640818 is not allowed to send in.",e.getArgs()[0]);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.SENDER);

    }

    @Test
    public void whenMultitenancyFalse_and_SenderOrgnum_DoesNotMatch_AR_then_ValidationException() {
        List<String> WHITE_LISTED_ORGNUM = List.of(HealthcareTestData.Identifier.validNhnSenderIdentifier.getIdentifier());

        StandardBusinessDocument sbd = dialgmelding();
        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setWhitelistOrgnum(WHITE_LISTED_ORGNUM));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier.withIdentifier("77777"));

        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);


        HealthcareValidationException e = assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));
        assertEquals("Multitenancy is not supported. Sender organisation number:920640818 is not registered in AR.",e.getArgs()[0]);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.RECEIVER);
        Mockito.verify(serviceRecordProvider,Mockito.times(1)).getServiceRecord(sbd, Participant.SENDER);

    }
   // Vi trenger å ha AR organisasjonsnummer som er basert på HerID2i scopes å matche whitelisted organisasjonsnummer og matche organisasjonsnummeret på sender
    @Test
    public void whenSenderOrgnumDoesNotMatchHerId2OrgNum_throwsValidationException(){
        List<String> WHITE_LISTED_ORGNUM = List.of(Identifier.validNhnSenderIdentifier.getIdentifier());

        StandardBusinessDocument sbd = createDialogMelding(Identifier.validNhnSenderIdentifier.withIdentifier("7234234"),Identifier.validNhnReceiverIdentifier);

        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setWhitelistOrgnum(WHITE_LISTED_ORGNUM));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);

        assertThrows(HealthcareValidationException.class ,()->healthcareRoutingService.validateAndApply(sbd));


    }

    public void whenSenderOrgnum_and_ArOrgnum_and_whitelistedOrgnum_doesNotMatch_throwValidationException(){
        List<String> WHITE_LISTED_ORGNUM = List.of(Identifier.validNhnSenderIdentifier.getIdentifier());
        StandardBusinessDocument sbd = dialgmelding();
        Mockito.lenient().when(integrasjonspunktProperties.getDph()).thenReturn(new IntegrasjonspunktProperties.DphConfig().setAllowMultitenancy(false).setWhitelistOrgnum(WHITE_LISTED_ORGNUM));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier.withIdentifier("77777"));


        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);



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
    public void whenStandardBusinessDocumentPassValidation_allScopeElementsArePresent() throws EncryptionException {
        final StandardBusinessDocument sbd = dialgmelding();

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);

        ServiceRecord spyRecord = Mockito.spy(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.RECEIVER)).thenReturn(spyRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(sbd, Participant.SENDER)).thenReturn(senderRecord);
        Mockito.lenient().when(businessMessageEncryptionService.encrypt(any(), (String) any())).thenReturn(new EncryptedBusinessMessage("dymmy","dummy"));

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

    @Test
    public void whenFnrReceiverAndReceiverHerId2_isTampered_throwsValidationException() {
        final StandardBusinessDocument tamperedSenderHerId1 = createDialogMelding(Identifier.validNhnSenderIdentifier,Identifier.validFastlegeReceiverIdentifier);
        tamperedSenderHerId1.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID2.getFullname()).setInstanceIdentifier("343437"));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validFastlegeReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(tamperedSenderHerId1, Participant.RECEIVER)).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(tamperedSenderHerId1, Participant.SENDER)).thenReturn(senderRecord);

        assertThrows(HealthcareValidationException.class,()->healthcareRoutingService.validateAndApply(tamperedSenderHerId1)) ;

    }

    @Test
    public void whenSbdScopesAreTampered_scopes_are_validated() {
        final StandardBusinessDocument tamperedSenderHerId1 = dialgmelding();
        tamperedSenderHerId1.getScopes().add(new Scope().setType(ScopeType.SENDER_HERID1.getFullname()).setInstanceIdentifier("343437"));

        ServiceRecord recieverRecord = serviceRecord(Identifier.validNhnReceiverIdentifier);
        ServiceRecord senderRecord = serviceRecord(Identifier.validNhnSenderIdentifier);

        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(Mockito.argThat(incomingSbd -> incomingSbd!=null && incomingSbd.getReceiverIdentifier().getIdentifier().equals(Identifier.validNhnReceiverIdentifier.getIdentifier())), eq(Participant.RECEIVER))).thenReturn(recieverRecord);
        Mockito.lenient().when(serviceRecordProvider.getServiceRecord(Mockito.argThat(incomingSbd -> incomingSbd.getSenderIdentifier().getIdentifier().equals(Identifier.validNhnSenderIdentifier.getIdentifier())), eq(Participant.SENDER))).thenReturn(senderRecord);

        Mockito.lenient()
            .when(integrasjonspunktProperties.getDph()).
            thenReturn(new IntegrasjonspunktProperties.DphConfig()
                .setAllowMultitenancy(true)
                .setWhitelistOrgnum(List.of(Identifier.validNhnSenderIdentifier.getIdentifier())));

        assertThrows(HealthcareValidationException.class,()->healthcareRoutingService.validateAndApply(tamperedSenderHerId1)) ;

        final StandardBusinessDocument tamperedReceiverHerId1 = dialgmelding();
        tamperedReceiverHerId1.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID1.getFullname()).setInstanceIdentifier("343437"));

        assertThrows(HealthcareValidationException.class,()->healthcareRoutingService.validateAndApply(tamperedReceiverHerId1)) ;

        final StandardBusinessDocument tamperedReceiverHerId2 = dialgmelding();
        tamperedReceiverHerId2.getScopes().removeIf((t)-> t.getType().equals(ScopeType.RECEIVER_HERID2.getFullname()));
        tamperedReceiverHerId2.getScopes().add(new Scope().setType(ScopeType.RECEIVER_HERID2.getFullname()).setInstanceIdentifier("343437"));

        assertThrows(HealthcareValidationException.class,()->healthcareRoutingService.validateAndApply(tamperedReceiverHerId2)) ;

    }




    private void safelyRun(Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {

        }
    }


}
