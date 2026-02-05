package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.AltinnFormidlingsTjenestenConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.sbdh.SBDUtil;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.nextmove.NextMoveOutMessage;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData;
import no.difi.meldingsutveksling.nextmove.nhn.HealthcareRoutingService;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Optional;

import static no.difi.meldingsutveksling.nextmove.StandardBusinessDocumentTestData.ARKIVMELDING_MESSAGE_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class NextMoveOutMessageFactoryTest {

    @MockitoBean private IntegrasjonspunktProperties props;
    @MockitoBean private ServiceRecordProvider serviceRecordProvider;
    @MockitoBean private UUIDGenerator uuidGenerator;
    @MockitoBean private Clock clock;
    @MockitoBean private HealthcareRoutingService healthcareRoutingService;

    private NextMoveOutMessageFactory factory;

    private no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress;

    @BeforeEach
    public void setup() {
        factory = new NextMoveOutMessageFactory(props, serviceRecordProvider, uuidGenerator, clock,healthcareRoutingService);

        srPostAddress = Mockito.mock(no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        when(srPostAddress.getName()).thenReturn("Foo");
        when(srPostAddress.getPostalCode()).thenReturn("0468");
        when(srPostAddress.getPostalArea()).thenReturn("Oslo");
    }

    @Test
    void testMessageChannelDefaultNoScope() {
        StandardBusinessDocument sbd = StandardBusinessDocumentTestData.createSbd(ARKIVMELDING_MESSAGE_DATA);
        when(serviceRecordProvider.getServiceIdentifier(sbd)).thenReturn(ServiceIdentifier.DPO);

        AltinnFormidlingsTjenestenConfig dpo = new AltinnFormidlingsTjenestenConfig();
        String messageChannel = "foo-42";
        dpo.setMessageChannel(messageChannel);
        when(props.getDpo()).thenReturn(dpo);

        NextMoveOutMessage msg = factory.getNextMoveOutMessage(sbd);
        Optional<Scope> scope = SBDUtil.getOptionalMessageChannel(msg.getSbd());
        assertTrue(scope.isPresent());
        assertEquals(messageChannel, scope.get().getIdentifier());
    }

    @Test
    void testMessageChannelDefaultScopeExistsEmptyIdentifier() {
        ServiceRecord sr = mock(ServiceRecord.class);
        StandardBusinessDocument sbd = StandardBusinessDocumentTestData.createSbd(ARKIVMELDING_MESSAGE_DATA);
        sbd.addScope(new Scope().setType(ScopeType.MESSAGE_CHANNEL.toString()));
        when(serviceRecordProvider.getServiceIdentifier(sbd)).thenReturn(ServiceIdentifier.DPO);

        AltinnFormidlingsTjenestenConfig dpo = new AltinnFormidlingsTjenestenConfig();
        String messageChannel = "foo-42";
        dpo.setMessageChannel(messageChannel);
        when(props.getDpo()).thenReturn(dpo);

        NextMoveOutMessage msg = factory.getNextMoveOutMessage(sbd);
        Optional<Scope> scope = SBDUtil.getOptionalMessageChannel(msg.getSbd());
        assertTrue(scope.isPresent());
        assertEquals(messageChannel, scope.get().getIdentifier());
    }

    @Test
    void testReceiverDefaults() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        when(srPostAddress.getStreet()).thenReturn("Bergensgata 42");

        PostAddress postAddress = new PostAddress();
        Method m = NextMoveOutMessageFactory.class.getDeclaredMethod("setReceiverDefaults",
                PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(factory, postAddress, srPostAddress);

        assertEquals("Foo", postAddress.getNavn());
        assertEquals("0468", postAddress.getPostnummer());
        assertEquals("Oslo", postAddress.getPoststed());
        assertEquals("Bergensgata 42", postAddress.getAdresselinje1());
    }

    @Test
    void testMultipleAddressLines() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(srPostAddress.getStreet()).thenReturn("C/O Bar;Bergensgata 42");

        PostAddress postAddress = new PostAddress();
        Method m = NextMoveOutMessageFactory.class.getDeclaredMethod("setReceiverDefaults",
                PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(factory, postAddress, srPostAddress);

        assertEquals("C/O Bar", postAddress.getAdresselinje1());
        assertEquals("Bergensgata 42", postAddress.getAdresselinje2());
    }

    @Test
    void testAddressLinesOverflow() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(srPostAddress.getStreet()).thenReturn("a;b;c;d;e");

        PostAddress postAddress = new PostAddress();
        Method m = NextMoveOutMessageFactory.class.getDeclaredMethod("setReceiverDefaults",
                PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(factory, postAddress, srPostAddress);

        assertEquals("a", postAddress.getAdresselinje1());
        assertEquals("b", postAddress.getAdresselinje2());
        assertEquals("c", postAddress.getAdresselinje3());
        assertEquals("d", postAddress.getAdresselinje4());
    }

}
