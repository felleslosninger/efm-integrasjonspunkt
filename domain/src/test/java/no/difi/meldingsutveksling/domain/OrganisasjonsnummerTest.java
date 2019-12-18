package no.difi.meldingsutveksling.domain;

import org.junit.Test;

import static org.junit.Assert.*;

public class OrganisasjonsnummerTest {

    private static final String orgnr = "123123123";
    private static final String onbehalf = "321321312";

    @Test
    public void asIso6523() {
        String iso6523orgnr = "0192:"+orgnr;
        String iso6523onbehalf = "0192:"+orgnr+":"+onbehalf;

        assertEquals(iso6523orgnr, Organisasjonsnummer.fromIso6523(iso6523orgnr).asIso6523());
        assertEquals(iso6523orgnr, Organisasjonsnummer.from(orgnr).asIso6523());
        assertEquals(iso6523onbehalf, Organisasjonsnummer.fromIso6523(iso6523onbehalf).asIso6523());
        assertEquals(iso6523onbehalf, Organisasjonsnummer.from(orgnr, onbehalf).asIso6523());
    }

    @Test
    public void fromIso6523() {
        Organisasjonsnummer organisasjonsnummer = Organisasjonsnummer.fromIso6523("0192:"+orgnr);
        assertEquals(orgnr, organisasjonsnummer.toString());
        assertFalse(organisasjonsnummer.getPaaVegneAvOrgnr().isPresent());

        Organisasjonsnummer organisasjonsnummer2 = Organisasjonsnummer.fromIso6523("0192:" + orgnr + ":" + onbehalf);
        assertEquals(orgnr, organisasjonsnummer.toString());
        assertEquals(onbehalf, organisasjonsnummer2.getPaaVegneAvOrgnr().orElse(""));
    }
}