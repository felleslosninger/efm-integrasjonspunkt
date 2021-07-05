package no.difi.meldingsutveksling.nextmove.v2;

import no.difi.meldingsutveksling.domain.sbdh.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SBDServiceTest {

    private SBDService target = new SBDService(Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DEFAULT_ZONE_ID));

    @Test
    public void notExpired() {
        assertThat(target.isExpired(getStandardBusinessDocument("2019-03-25T11:38:24Z"))).isFalse();
    }

    @Test
    public void expired() {
        assertThat(target.isExpired(getStandardBusinessDocument("2019-03-25T11:38:22Z"))).isTrue();
    }

    private StandardBusinessDocument getStandardBusinessDocument(String expectedResponseTime) {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .setType("ConversationId")
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(OffsetDateTime.parse(expectedResponseTime))
                                        )
                                )
                        )
                );
    }
}
