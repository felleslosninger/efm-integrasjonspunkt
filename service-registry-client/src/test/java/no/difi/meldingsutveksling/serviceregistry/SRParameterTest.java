package no.difi.meldingsutveksling.serviceregistry;

import no.difi.meldingsutveksling.NextMoveConsts;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SRParameterTest {

    private static final String IDENTIFIER = "123123123";

    @Test
    public void shouldUseDefaultValueForSecuritylevel() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER).build();
        assertEquals(IDENTIFIER, parameter.getIdentifier());
        assertEquals(NextMoveConsts.DEFAULT_SECURITY_LEVEL, parameter.getSecurityLevel());
    }

    @Test
    public void canOverrideDefaultSecurityLevel() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .securityLevel(4)
                .build();
        assertEquals(Integer.valueOf(4), parameter.getSecurityLevel());
    }

    @Test(expected = NullPointerException.class)
    public void nullNotAllowedForSecurityLevel() {
        SRParameter.builder(IDENTIFIER)
                .securityLevel(null)
                .build();
    }

    @Test
    public void queryShouldOnlyContainSecurityParameterDefault() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER).build();
        assertEquals("securityLevel=3", parameter.getQuery());
    }

    @Test
    public void queryShouldContainConversationId() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .conversationId("foo123")
                .build();
        assertEquals("securityLevel=3&conversationId=foo123", parameter.getQuery());
    }
}
