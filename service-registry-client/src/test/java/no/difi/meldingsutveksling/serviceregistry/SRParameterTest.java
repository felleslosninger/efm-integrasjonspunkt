package no.difi.meldingsutveksling.serviceregistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SRParameterTest {

    private static final String IDENTIFIER = "123123123";

    @Test
    public void queryWithNoParameters() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER).build();
        assertEquals(IDENTIFIER, parameter.getIdentifier());
        assertNull(parameter.getSecurityLevel());
    }

    @Test
    public void queryShouldOnlyContainSecurityLevel() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .securityLevel(4)
                .build();
        assertEquals(Integer.valueOf(4), parameter.getSecurityLevel());
        assertEquals("securityLevel=4", parameter.getQuery());
    }

    @Test
    public void queryShouldOnlyContainConversationId() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .conversationId("foo123")
                .build();
        assertEquals("conversationId=foo123", parameter.getQuery());
    }

    @Test
    public void queryShouldHaveSecurityLevelAndConversationId() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .conversationId("foo123")
                .securityLevel(3)
                .build();
        assertEquals("securityLevel=3&conversationId=foo123", parameter.getQuery());
    }
}
