package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.UUIDGenerator;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class NextMoveOutMessageFactoryTest {

    @MockBean
    private IntegrasjonspunktProperties props;
    @MockBean
    private ServiceRecordProvider serviceRecordProvider;
    @MockBean
    private UUIDGenerator uuidGenerator;
    @MockBean
    private Clock clock;

    private NextMoveOutMessageFactory factory;

    private no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress;

    @BeforeEach
    public void setup() {
        factory = new NextMoveOutMessageFactory(props, serviceRecordProvider, uuidGenerator, clock);

        srPostAddress = Mockito.mock(no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        when(srPostAddress.getName()).thenReturn("Foo");
        when(srPostAddress.getPostalCode()).thenReturn("0468");
        when(srPostAddress.getPostalArea()).thenReturn("Oslo");
    }

    @Test
    public void testReceiverDefaults() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
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
    public void testMultipleAddressLines() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
    public void testAddressLinesOverflow() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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