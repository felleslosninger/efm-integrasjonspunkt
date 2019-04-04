package no.difi.meldingsutveksling.cucumber;

import cucumber.api.java.en.Given;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.DpoMessage;

import java.time.ZonedDateTime;

import static no.difi.meldingsutveksling.domain.sbdh.SBDUtil.isExpired;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * checking if expectedResponseDateTime has expired which means the message should not be handled further.
 * ExpectedResponseDateTime is a value set in the Standard Business Document Header to indicate how long this message
 * will be allowed to attempt being delivered.  If this time ever expires the validation will catch it.
 *
 * isCurrentTimeAfterExpectedResponseDateTime checks the
 * isExpectedReponseDateTimeExpired
 *
 * @Author Jaflaten
 */

public class TimeToLiveValidationSteps {
    private StandardBusinessDocument sbd = getDocument();
    private StandardBusinessDocument sbd2 = getDocument2();

    @Given("^I have obtained expectedResponseDateTime and currentDateTime$")
    public void isCurrentTimeAfterExpectedResponseDateTime() {
        ZonedDateTime currentDateTime = sbd.getStandardBusinessDocumentHeader().getExpectedResponseDateTime();
        ZonedDateTime expectedResponseDateTime = sbd2.getStandardBusinessDocumentHeader().getExpectedResponseDateTime();

        assertThat(currentDateTime.isAfter(expectedResponseDateTime)).isTrue();
    }

    @Given("^I have obtained expectedResponseDateTime $")
    public void isExpectedReponseDateTimeExpired() {
        StandardBusinessDocumentHeader header = sbd.getStandardBusinessDocumentHeader();
        assertThat(isExpired(header)).isTrue();
    }

    private StandardBusinessDocument getDocument() {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(ZonedDateTime.parse("2019-05-10T00:31:52Z"))
                                        )
                                        .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setCreationDateAndTime(ZonedDateTime.parse("2019-01-11T15:29:58.753+02:00"))
                                .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
                                .setStandard("urn:no:difi:meldingsutveksling:2.0")
                                .setType("DPO")
                                .setTypeVersion("2.0")
                        )
                        .setHeaderVersion("1.0")
                        .addReceiver(new Receiver()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910075918")
                                )
                        )
                        .addSender(new Sender()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910077473")
                                )
                        )
                )
                .setAny(new DpoMessage()
                        .setDpoField("foo")
                        .setSecurityLevel("3")
                );
    }

    private StandardBusinessDocument getDocument2() {
        return new StandardBusinessDocument()
                .setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                        .setBusinessScope(new BusinessScope()
                                .addScope(new Scope()
                                        .addScopeInformation(new CorrelationInformation()
                                                .setExpectedResponseDateTime(ZonedDateTime.parse("2019-04-10T00:31:52Z"))
                                        )
                                        .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                        .setInstanceIdentifier("37efbd4c-413d-4e2c-bbc5-257ef4a65a45")
                                        .setType("ConversationId")
                                )
                        )
                        .setDocumentIdentification(new DocumentIdentification()
                                .setCreationDateAndTime(ZonedDateTime.parse("2019-02-11T15:29:58.753+02:00"))
                                .setInstanceIdentifier("ff88849c-e281-4809-8555-7cd54952b916")
                                .setStandard("urn:no:difi:meldingsutveksling:2.0")
                                .setType("DPO")
                                .setTypeVersion("2.0")
                        )
                        .setHeaderVersion("1.0")
                        .addReceiver(new Receiver()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910075918")
                                )
                        )
                        .addSender(new Sender()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("9908:910077473")
                                )
                        )
                )
                .setAny(new DpoMessage()
                        .setDpoField("foo")
                        .setSecurityLevel("3")
                );
    }
}
