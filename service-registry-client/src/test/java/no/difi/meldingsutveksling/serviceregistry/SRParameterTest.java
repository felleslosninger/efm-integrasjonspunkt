package no.difi.meldingsutveksling.serviceregistry;


import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SRParameterTest {

    private static final Iso6523 IDENTIFIER = Iso6523.of(ICD.NO_ORG, "123123123");

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
        assertTrue(parameter.getUrlTemplate().contains("securityLevel="));
        assertEquals("4", parameter.getUrlVariables().get("securityLevel"));
    }

    @Test
    public void queryShouldOnlyContainConversationId() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .conversationId("foo123")
                .build();
        assertTrue(parameter.getUrlTemplate().contains("conversationId="));
        assertEquals("foo123", parameter.getUrlVariables().get("conversationId"));
    }

    @Test
    public void queryShouldHaveSecurityLevelAndConversationId() {
        SRParameter parameter = SRParameter.builder(IDENTIFIER)
                .conversationId("foo123")
                .securityLevel(3)
                .build();
        assertTrue(parameter.getUrlTemplate().contains("securityLevel="));
        assertTrue(parameter.getUrlTemplate().contains("conversationId="));
        assertEquals("3", parameter.getUrlVariables().get("securityLevel"));
        assertEquals("foo123", parameter.getUrlVariables().get("conversationId"));
    }
}
