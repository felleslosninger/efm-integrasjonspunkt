package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.nextmove.DpiPrintMessageFiller;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DpiPrintMessageFillerTest {

    private no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress;
    private final DpiPrintMessageFiller filler = new DpiPrintMessageFiller();

    @Before
    public void setup() {
        srPostAddress = Mockito.mock(no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        when(srPostAddress.getName()).thenReturn("Foo");
        when(srPostAddress.getPostalCode()).thenReturn("0468");
        when(srPostAddress.getPostalArea()).thenReturn("Oslo");
    }

    @Test
    public void testReceiverDefaults() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        when(srPostAddress.getStreet()).thenReturn("Bergensgata 42");

        PostAddress postAddress = new PostAddress();
        Method m = DpiPrintMessageFiller.class.getDeclaredMethod("setReceiverDefaults",
            PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(filler, postAddress, srPostAddress);

        assertEquals("Foo", postAddress.getNavn());
        assertEquals("0468", postAddress.getPostnummer());
        assertEquals("Oslo", postAddress.getPoststed());
        assertEquals("Bergensgata 42", postAddress.getAdresselinje1());
    }

    @Test
    public void testMultipleAddressLines() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(srPostAddress.getStreet()).thenReturn("C/O Bar;Bergensgata 42");

        PostAddress postAddress = new PostAddress();
        Method m = DpiPrintMessageFiller.class.getDeclaredMethod("setReceiverDefaults",
            PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(filler, postAddress, srPostAddress);

        assertEquals("C/O Bar", postAddress.getAdresselinje1());
        assertEquals("Bergensgata 42", postAddress.getAdresselinje2());
    }

    @Test
    public void testAddressLinesOverflow() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(srPostAddress.getStreet()).thenReturn("a;b;c;d;e");

        PostAddress postAddress = new PostAddress();
        Method m = DpiPrintMessageFiller.class.getDeclaredMethod("setReceiverDefaults",
            PostAddress.class, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress.class);
        m.setAccessible(true);
        m.invoke(filler, postAddress, srPostAddress);

        assertEquals("a", postAddress.getAdresselinje1());
        assertEquals("b", postAddress.getAdresselinje2());
        assertEquals("c", postAddress.getAdresselinje3());
        assertEquals("d", postAddress.getAdresselinje4());
    }

}