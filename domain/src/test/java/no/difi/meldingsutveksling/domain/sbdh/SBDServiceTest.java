package no.difi.meldingsutveksling.domain.sbdh;

import lombok.Builder;
import lombok.Data;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.domain.Organisasjonsnummer;
import no.difi.meldingsutveksling.domain.sbdh.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import static no.difi.meldingsutveksling.DateTimeUtil.DEFAULT_ZONE_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SBDServiceTest {
    public static final Organisasjonsnummer IP_ORG_NUMBER = Organisasjonsnummer.parse("0192:111111111");
    public static final Organisasjonsnummer ON_BEHALF_OF = Organisasjonsnummer.parse("0192:222222222");
    public static final Organisasjonsnummer RECEIVER = Organisasjonsnummer.parse("0192:333333333");
    public static final Organisasjonsnummer IP_ORG_NUMMER_AND_ON_BEHALF_OF = Organisasjonsnummer.parse("0192:111111111:222222222");

    private final IntegrasjonspunktProperties properties = new IntegrasjonspunktProperties()
            .setOrg(new IntegrasjonspunktProperties.Organization()
                    .setNumber(IP_ORG_NUMBER.getOrgNummer())
            );

    private final SBDService target = new SBDService(Clock.fixed(Instant.parse("2019-03-25T11:38:23Z"), DEFAULT_ZONE_ID), properties);

    @Test
    void notExpired() {
        assertThat(target.isExpired(Sbd.builder().expectedResponseTime("2019-03-25T11:38:24Z").build())).isFalse();
    }

    @Test
    void expired() {
        assertThat(target.isExpired(Sbd.builder().expectedResponseTime("2019-03-25T11:38:22Z").build())).isTrue();
    }

    @Test
    void getSenderIdentifier() {
        assertThat(target.getSenderIdentifier(Sbd.builder().sender(IP_ORG_NUMBER).receiver(RECEIVER).build())).isEqualTo(IP_ORG_NUMBER.getOrgNummer());
        assertThat(target.getSenderIdentifier(Sbd.builder().sender(ON_BEHALF_OF).receiver(RECEIVER).build())).isEqualTo(IP_ORG_NUMBER.getOrgNummer());
        assertThat(target.getSenderIdentifier(Sbd.builder().sender(RECEIVER).receiver(IP_ORG_NUMBER).build())).isEqualTo(RECEIVER.getOrgNummer());
        assertThat(target.getSenderIdentifier(Sbd.builder().sender(IP_ORG_NUMMER_AND_ON_BEHALF_OF).receiver(RECEIVER).build())).isEqualTo(IP_ORG_NUMMER_AND_ON_BEHALF_OF.getOrgNummer());
    }

    @Test
    void getOnBehalfOfOrgNr() {
        assertThat(target.getOnBehalfOfOrgNr(Sbd.builder().sender(IP_ORG_NUMBER).receiver(RECEIVER).build())).isEmpty();
        assertThat(target.getOnBehalfOfOrgNr(Sbd.builder().sender(ON_BEHALF_OF).receiver(RECEIVER).build())).hasValue(ON_BEHALF_OF.getOrgNummer());
        assertThat(target.getOnBehalfOfOrgNr(Sbd.builder().sender(RECEIVER).receiver(IP_ORG_NUMBER).build())).isEmpty();
        assertThat(target.getOnBehalfOfOrgNr(Sbd.builder().sender(IP_ORG_NUMMER_AND_ON_BEHALF_OF).receiver(RECEIVER).build())).isEqualTo(IP_ORG_NUMMER_AND_ON_BEHALF_OF.getPaaVegneAvOrgnr());
    }

    @Test
    void getReceiverIdentifier() {
        assertThat(target.getReceiverIdentifier(Sbd.builder().receiver(RECEIVER).build())).isEqualTo(RECEIVER.getOrgNummer());
        assertThat(target.getReceiverIdentifier(Sbd.builder().receiver(IP_ORG_NUMBER).build())).isEqualTo(IP_ORG_NUMBER.getOrgNummer());
    }

    @Data
    @Builder
    private static class Sbd {

        private Organisasjonsnummer sender;
        private Organisasjonsnummer receiver;
        private String expectedResponseTime;

        @SuppressWarnings("unused")
        public static class SbdBuilder {

            private StandardBusinessDocument build() {
                StandardBusinessDocumentHeader header = new StandardBusinessDocumentHeader();

                if (sender != null) {
                    header.addSender(new Partner()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(sender.asIso6523())
                            )
                    );
                }

                if (receiver != null) {
                    header.addReceiver(new Partner()
                            .setIdentifier(new PartnerIdentification()
                                    .setValue(receiver.asIso6523())
                            )
                    );
                }

                if (expectedResponseTime != null) {
                    header.setBusinessScope(new BusinessScope()
                            .addScope(new Scope()
                                    .setType("ConversationId")
                                    .addScopeInformation(new CorrelationInformation()
                                            .setExpectedResponseDateTime(OffsetDateTime.parse(expectedResponseTime))
                                    )
                            )
                    );
                }

                return new StandardBusinessDocument()
                        .setStandardBusinessDocumentHeader(header);
            }
        }
    }
}
