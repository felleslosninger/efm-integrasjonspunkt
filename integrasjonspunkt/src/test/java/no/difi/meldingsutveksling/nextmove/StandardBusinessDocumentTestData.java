package no.difi.meldingsutveksling.nextmove;

import lombok.Data;
import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@UtilityClass
public class StandardBusinessDocumentTestData {

    @Data
    public static class MessageData {
        private final String messageId = UUID.randomUUID().toString();
        private final String conversationId = UUID.randomUUID().toString();
        private BusinessMessage businessMessage;
        private String process;
        private String standard;
        private String type;
        private String senderIdentifier = "910077473";
        private String receiverIdentifier = "910075918";
    }

    public static final MessageData ARKIVMELDING_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0")
            .setStandard("urn:no:difi:arkivmelding:xsd::arkivmelding")
            .setType("arkivmelding")
            .setBusinessMessage(new ArkivmeldingMessageAsAttachment()
                    .setHoveddokument("before_the_law.txt"));


    static final StandardBusinessDocument ARKIVMELDING_INPUT = getInputSbd(ARKIVMELDING_MESSAGE_DATA);
    static final StandardBusinessDocument ARKIVMELDING_SBD = getResponseSbd(ARKIVMELDING_MESSAGE_DATA);
    static final NextMoveOutMessage ARKIVMELDING_MESSAGE = NextMoveOutMessage.of(ARKIVMELDING_SBD, ServiceIdentifier.DPO);

    private static final MessageData DPI_DIGITAL_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:digitalpost:info:ver1.0")
            .setStandard("urn:no:difi:digitalpost:xsd:digital::digital")
            .setType("digital")
            .setBusinessMessage(new DpiDigitalMessageAsAttachment()
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
    static final StandardBusinessDocument DPI_DIGITAL_SBD = getResponseSbd(DPI_DIGITAL_MESSAGE_DATA);
    static final NextMoveOutMessage DPI_DIGITAL_MESSAGE = NextMoveOutMessage.of(DPI_DIGITAL_SBD, ServiceIdentifier.DPI);

    private static final MessageData DIGITAL_DPV_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:digitalpost:info:ver1.0")
            .setStandard("urn:no:difi:digitalpost:xsd:digital::digital_dpv")
            .setType("digital_dpv")
            .setBusinessMessage(new DigitalDpvMessageAsAttachment()
                    .setTittel("Our winters")
                    .setSammendrag("A quote from Franz Kafka's The Castle")
                    .setInnhold("Our winters are very long here, very long and very monotonous. But we don't complain about it downstairs, we're shielded against the winter. Oh, spring does come eventually, and summer, and they last for a while, but now, looking back, spring and summer seem too short, as if they were not much more than a couple of days, and even on those days, no matter how lovely the day, it still snows occasionally.")
            );

    static final StandardBusinessDocument DIGITAL_DPV_INPUT = getInputSbd(DIGITAL_DPV_MESSAGE_DATA);
    static final StandardBusinessDocument DIGITAL_DPV_SBD = getResponseSbd(DIGITAL_DPV_MESSAGE_DATA);
    static final NextMoveOutMessage DIGITAL_DPV_MESSAGE = NextMoveOutMessage.of(DIGITAL_DPV_SBD, ServiceIdentifier.DPV);

    private static final MessageData DPI_PRINT_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:digitalpost:vedtak:ver1.0")
            .setStandard("urn:no:difi:digitalpost:xsd:fysisk::print")
            .setType("print")
            .setBusinessMessage(new DpiPrintMessageAsAttachment()
                    .setHoveddokument("kafka_quotes.txt")
                    .setMottaker(PostAddress.builder()
                            .navn("Ola Nordmann")
                            .adresselinje1("Langtoppilia 1")
                            .adresselinje2("")
                            .adresselinje3("")
                            .adresselinje4("")
                            .postnummer("9999")
                            .poststed("FJELL")
                            .land("Norway")
                            .build()
                    )
                    .setUtskriftsfarge(PrintColor.FARGE)
                    .setPosttype(PostalCategory.A_PRIORITERT)
                    .setRetur(new MailReturn()
                            .setMottaker(PostAddress.builder()
                                    .navn("Fjellheimen kommune")
                                    .adresselinje1("Luftigveien 1")
                                    .adresselinje2("")
                                    .adresselinje3("")
                                    .adresselinje4("")
                                    .postnummer("9999")
                                    .poststed("FJELL")
                                    .land("Norway")
                                    .build()
                            )
                            .setReturhaandtering(ReturnHandling.DIREKTE_RETUR)
                    )
            );

    static final StandardBusinessDocument DPI_PRINT_INPUT = getInputSbd(DPI_PRINT_MESSAGE_DATA);
    static final StandardBusinessDocument DPI_PRINT_SBD = getResponseSbd(DPI_PRINT_MESSAGE_DATA);
    static final NextMoveOutMessage DPI_PRINT_MESSAGE = NextMoveOutMessage.of(DPI_PRINT_SBD, ServiceIdentifier.DPI);

    private static final MessageData INNSYNSKRAV_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:einnsyn:innsynskrav:ver1.0")
            .setStandard("urn:no:difi:einnsyn:xsd::innsynskrav")
            .setType("innsynskrav")
            .setBusinessMessage(new InnsynskravMessageAsAttachment()
                    .setOrgnr("98765432")
                    .setEpost("doofenshmirtz@evil.inc")
            );

    static final StandardBusinessDocument INNSYNSKRAV_INPUT = getInputSbd(INNSYNSKRAV_MESSAGE_DATA);
    static final StandardBusinessDocument INNSYNSKRAV_SBD = getResponseSbd(INNSYNSKRAV_MESSAGE_DATA);
    static final NextMoveOutMessage INNSYNSKRAV_MESSAGE = NextMoveOutMessage.of(INNSYNSKRAV_SBD, ServiceIdentifier.DPE);

    private static final MessageData PUBLISERING_MESSAGE_DATA = new MessageData()
            .setProcess("urn:no:difi:profile:einnsyn:journalpost:ver1.0")
            .setStandard("urn:no:difi:einnsyn:xsd::publisering")
            .setType("publisering")
            .setBusinessMessage(new PubliseringMessageAsAttachment()
                    .setOrgnr("98765432")
            );

    static final StandardBusinessDocument PUBLISERING_INPUT = getInputSbd(PUBLISERING_MESSAGE_DATA);
    static final StandardBusinessDocument PUBLISERING_SBD = getResponseSbd(PUBLISERING_MESSAGE_DATA);
    static final NextMoveOutMessage PUBLISERING_MESSAGE = NextMoveOutMessage.of(PUBLISERING_SBD, ServiceIdentifier.DPE);
    static final NextMoveInMessage PUBLISERING_MESSAGE_RESPONSE = NextMoveInMessage.of(getResponseSbd(PUBLISERING_MESSAGE_DATA), ServiceIdentifier.DPE);

    public static StandardBusinessDocument createSbd(MessageData messageData) {
        return getResponseSbd(messageData);
    }

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
                                        .setIdentifier(message.process)
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
                        .addReceiver(new Partner()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("0192:" + message.receiverIdentifier)
                                )
                        )
                        .addSender(new Partner()
                                .setIdentifier(new PartnerIdentification()
                                        .setAuthority("iso6523-actorid-upis")
                                        .setValue("0192:" + message.senderIdentifier)
                                )
                        )
                )
                .setAny(message.businessMessage);
    }
}
