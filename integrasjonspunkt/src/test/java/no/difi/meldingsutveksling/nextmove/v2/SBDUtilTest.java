package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SBDUtilTest {

    private SBDUtil sbdUtil = new SBDUtil(Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), ZoneId.of("UTC")));

    @Test
    public void notExpired() {
        assertThat(sbdUtil.isExpired(getStandardBusinessDocument("2019-03-25T11:39:23Z"))).isFalse();
    }

    @Test
    public void expired() {
        assertThat(sbdUtil.isExpired(getStandardBusinessDocument("2019-03-25T11:37:23Z"))).isTrue();
    }

    private StandardBusinessDocument getStandardBusinessDocument(String expectedResponseTime) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .setType("ConversationId")
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(ZonedDateTime.parse(expectedResponseTime))
                                        )
                                )
                        )
                );
    }
}
