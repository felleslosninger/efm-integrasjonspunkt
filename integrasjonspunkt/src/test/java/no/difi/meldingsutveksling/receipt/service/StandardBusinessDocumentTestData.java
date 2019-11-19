package no.difi.meldingsutveksling.receipt.service;

import lombok.Data;
import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@UtilityClass
class StandardBusinessDocumentTestData {

    @Data
    static class MessageData {
        private final String messageId = UUID.randomUUID().toString();
        private final String conversationId = UUID.randomUUID().toString();
        private BusinessMessage businessMessage;
        private String standard;
        private String type;
    }

    static final MessageData ARKIVMELDING_MESSAGE_DATA = new MessageData()
            .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
            .setType("arkivmelding")
            .setBusinessMessage(new ArkivmeldingMessage()
                    .setHoveddokument("before_the_law.txt"));

    static final StandardBusinessDocument ARKIVMELDING_INPUT = getInputSbd(ARKIVMELDING_MESSAGE_DATA);

    static final NextMoveOutMessage ARKIVMELDING_MESSAGE = NextMoveOutMessage.of(getResponseSbd(ARKIVMELDING_MESSAGE_DATA), ServiceIdentifier.DPO);

    private static final MessageData DPI_DIGITAL_MESSAGE_DATA = new MessageData()
            .setStandard("urn:no:difi:digitalpost:xsd:digital::digital")
            .setType("digital")
            .setBusinessMessage(new DpiDigitalMessage()
                    .setSikkerhetsnivaa(4)
                    .setHoveddokument("kafka_quotes.txt")
                    .setSpraak("en")
                    .setTittel("Kafka quotes")
                    .setDigitalPostInfo(new DigitalPostInfo()
                            .setVirkningsdato(LocalDate.parse("2019-04-01"))
                            .setAapningskvittering(true)
                    ).setVarsler(new DpiNotification()
                            .setEpostTekst("Many a book is like a key to unknown chambers within the castle of one’s own self.")
                            .setSmsTekst("A book must be the axe for the frozen sea within us.")
                    )

            );

    static final StandardBusinessDocument DPI_DIGITAL_INPUT = getInputSbd(DPI_DIGITAL_MESSAGE_DATA);

    static final NextMoveOutMessage DPI_DIGITAL_MESSAGE = NextMoveOutMessage.of(getResponseSbd(DPI_DIGITAL_MESSAGE_DATA), ServiceIdentifier.DPI);

    static StandardBusinessDocument getInputSbd(MessageData message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        return sbd;
    }

    private static StandardBusinessDocument getResponseSbd(MessageData message) {
        StandardBusinessDocument sbd = new StandardBusinessDocument();
        fill(sbd, message);
        sbd.getStandardBusinessDocumentHeader().getDocumentIdentification()
                .setCreationDateAndTime(OffsetDateTime.parse("2019-03-25T11:38:23+02:00"));

        return sbd;
    }

    private static void fill(StandardBusinessDocument sbd, MessageData message) {
        sbd.setStandardBusinessDocumentHeader(new StandardBusinessDocumentHeader()
                .setBusinessScope(new BusinessScope()
                        .addScope(new Scope()
                                .addScopeInformation(new CorrelationInformation()
                                        .setExpectedResponseDateTime(OffsetDateTime.parse("2019-04-25T11:38:23+02:00"))
                                )
                                .setIdentifier("urn:no:difi:meldingsutveksling:2.0")
                                .setInstanceIdentifier(message.conversationId)
                                .setType("ConversationId")
                        )
                )
                .setDocumentIdentification(new DocumentIdentification()
                        .setInstanceIdentifier(message.messageId)
                        .setStandard(message.getStandard())
                        .setType(message.getType())
                        .setTypeVersion("1.0")
                )
                .setHeaderVersion("1.0")
                .addReceiver(new Receiver()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority("iso6523-actorid-upis")
                                .setValue("0192:910075918")
                        )
                )
                .addSender(new Sender()
                        .setIdentifier(new PartnerIdentification()
                                .setAuthority("iso6523-actorid-upis")
                                .setValue("0192:910077473")
                        )
                )
        )
                .setAny(message.businessMessage);
    }
}
