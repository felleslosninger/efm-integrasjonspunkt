package no.difi.meldingsutveksling.nextmove;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.domain.webhooks.Subscription;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Utskrift;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Retur;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.validation.group.NextMoveValidationGroups;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
import org.jetbrains.annotations.NotNull;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;

@UtilityClass
class RestDocumentationCommon {

    static HeaderDescriptor[] getDefaultHeaderDescriptors() {
        return new HeaderDescriptor[]{
        };
    }

    static ParameterDescriptor[] getPagingParameterDescriptors() {
        return new ParameterDescriptor[]{parameterWithName("page")
                .description("Page you want to retrieve. First page is page 0")
                .optional(),
                parameterWithName("size")
                        .description("Size of the page you want to retrieve.")
                        .optional(),
                parameterWithName("sort")
                        .description("Properties that should be sorted by in the format property,property(,ASC|DESC). Default sort direction is ascending. Use multiple sort parameters if you want to switch directions, e.g. ?sort=firstname&sort=lastname,asc.")
                        .optional()};
    }

    static FieldDescriptor[] pageDescriptors() {
        return new FieldDescriptor[]{
                fieldWithPath("last").description("A boolean value indicating if this is the last page or not."),
                fieldWithPath("totalElements").description("The total number of elements"),
                fieldWithPath("totalPages").description("The total number of pages"),
                fieldWithPath("size").description("The page size"),
                fieldWithPath("number").description("The page number"),
                fieldWithPath("first").description("A boolean value indicating if this is the first page or not."),
                fieldWithPath("numberOfElements").description("Number of elements returned in the page."),
                fieldWithPath("empty").description("True if the page is empty. False if not."),
                fieldWithPath("sort.sorted").description("True if the result set is sorted. False otherwise."),
                fieldWithPath("sort.unsorted").description("True if the result set is unsorted. False otherwise."),
                fieldWithPath("sort.empty").description("True if no sorting. False otherwise")
        };
    }

    static FieldDescriptor[] pageableDescriptors() {
        return new FieldDescriptor[]{
                fieldWithPath("offset")
                        .type(JsonFieldType.NUMBER)
                        .description("The offset to be taken according to the underlying page and page size."),
                fieldWithPath("pageSize")
                        .type(JsonFieldType.NUMBER)
                        .description("The requested page size"),
                fieldWithPath("pageNumber")
                        .type(JsonFieldType.NUMBER)
                        .description("The requested page number"),
                fieldWithPath("unpaged")
                        .type(JsonFieldType.BOOLEAN)
                        .ignored(),
                fieldWithPath("paged")
                        .type(JsonFieldType.BOOLEAN)
                        .ignored(),
                fieldWithPath("sort.sorted")
                        .type(JsonFieldType.BOOLEAN)
                        .description("True if the result set is sorted. False otherwise."),
                fieldWithPath("sort.unsorted")
                        .type(JsonFieldType.BOOLEAN)
                        .description("True if the result set is unsorted. False otherwise."),
                fieldWithPath("sort.empty")
                        .type(JsonFieldType.BOOLEAN)
                        .description("True if no sorting. False otherwise")
        };
    }

    static List<FieldDescriptor> errorFieldDescriptors(boolean withErrors) {
        FieldDescriptorsBuilder builder = new FieldDescriptorsBuilder();
        builder.fields(
                fieldWithPath("timestamp")
                        .type(JsonFieldType.STRING)
                        .description("Date and time for when the error occured."),
                fieldWithPath("status")
                        .type(JsonFieldType.NUMBER)
                        .description("HTTP status code."),
                fieldWithPath("error")
                        .type(JsonFieldType.STRING)
                        .description("Error description"),
                fieldWithPath("exception")
                        .optional()
                        .type(JsonFieldType.STRING)
                        .description("The java class of the Exception that was thrown"),
                fieldWithPath("message")
                        .type(JsonFieldType.STRING)
                        .description("A message describing the error."),
                fieldWithPath("path")
                        .type(JsonFieldType.STRING)
                        .description("The request URI"),
                fieldWithPath("description")
                        .optional()
                        .type(JsonFieldType.STRING)
                        .description("A more detailed description of the error.")
        );

        if (withErrors) {
            builder.fields(fieldWithPath("errors")
                    .type(JsonFieldType.ARRAY)
                    .description("Constraint validations details.")
            ).fields(errorsFieldDescriptors("errors[]."));
        }

        return builder.build();
    }

    private static List<FieldDescriptor> errorsFieldDescriptors(String prefix) {
        return new FieldDescriptorsBuilder()
                .fields(
                        fieldWithPath(prefix + "codes[]")
                                .type(JsonFieldType.ARRAY)
                                .description("The message codes to be used to resolve this message."),
                        fieldWithPath(prefix + "defaultMessage")
                                .type(JsonFieldType.STRING)
                                .description("The default message to be used to resolve this message."),
                        fieldWithPath(prefix + "objectName")
                                .type(JsonFieldType.STRING)
                                .description("The name/path of the object where the constraint validation occurred."),
                        fieldWithPath(prefix + "field")
                                .type(JsonFieldType.STRING)
                                .description("The name/path of the field where the constraint validation occurred."),
                        fieldWithPath(prefix + "rejectedValue")
                                .optional()
                                .type(JsonFieldType.STRING)
                                .description("The rejected field value."),
                        fieldWithPath(prefix + "bindingFailure")
                                .type(JsonFieldType.BOOLEAN)
                                .description("Whether this error represents a binding failure (like a type mismatch); otherwise it is a validation failure."),
                        fieldWithPath(prefix + "code")
                                .type(JsonFieldType.STRING)
                                .description("The message code to be used to resolve this message.")
                ).build();
    }

    static FieldDescriptor[] messageStatusDescriptors(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id")
                        .type(JsonFieldType.NUMBER)
                        .description("Integer. The numeric message status ID."),
                fieldWithPath(prefix + "convId")
                        .type(JsonFieldType.NUMBER)
                        .description("Integer. The numeric conversation ID."),
                fieldWithPath(prefix + "messageId")
                        .type(JsonFieldType.STRING)
                        .description("The messageId. Typically an UUID."),
                fieldWithPath(prefix + "conversationId")
                        .type(JsonFieldType.STRING)
                        .description("The conversationId. Typically an UUID."),
                statusDescription(prefix),
                fieldWithPath(prefix + "description")
                        .type(JsonFieldType.STRING)
                        .description("Description."),
                fieldWithPath(prefix + "lastUpdate")
                        .type(JsonFieldType.STRING)
                        .description("Date and time of status."),
                fieldWithPath(prefix + "rawReceipt")
                        .optional()
                        .type(JsonFieldType.STRING)
                        .description("The raw receipt.")
        };
    }

    @NotNull
    private static FieldDescriptor statusDescription(String prefix) {
        return fieldWithPath(prefix + "status")
                .type(JsonFieldType.STRING)
                .description(String.format("The message status. Can be one of: %s. " +
                        "More details can be found https://difi.github.io/felleslosninger/eformidling_selfhelp_traffic_flow.html[here].", Arrays.stream(ReceiptStatus.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "))));
    }

    static List<FieldDescriptor> conversationDescriptors(String prefix) {
        return new FieldDescriptorsBuilder()
                .fields(
                        fieldWithPath(prefix + "id")
                                .type(JsonFieldType.NUMBER)
                                .description("Integer. The numeric message status ID."),
                        fieldWithPath(prefix + "messageId")
                                .type(JsonFieldType.STRING)
                                .description("The messageId. Typically an UUID."),
                        fieldWithPath(prefix + "conversationId")
                                .type(JsonFieldType.STRING)
                                .description("The conversationId. Typically an UUID."),
                        fieldWithPath(prefix + "senderIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify the sender. Requires a 0192: prefix for all norwegian organizations."),
                        fieldWithPath(prefix + "receiverIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify the receiver. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                        fieldWithPath(prefix + "processIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("The process identifier used by the message."),
                        fieldWithPath(prefix + "messageReference")
                                .type(JsonFieldType.STRING)
                                .description("The message reference"),
                        fieldWithPath(prefix + "messageTitle")
                                .type(JsonFieldType.STRING)
                                .description("The message title"),
                        fieldWithPath(prefix + "serviceCode")
                                .type(JsonFieldType.STRING)
                                .description("Altinn service code"),
                        fieldWithPath(prefix + "serviceEditionCode")
                                .type(JsonFieldType.STRING)
                                .description("Altinn service edition code."),
                        fieldWithPath(prefix + "lastUpdate")
                                .type(JsonFieldType.STRING)
                                .description("Date and time of status."),
                        fieldWithPath(prefix + "finished")
                                .type(JsonFieldType.BOOLEAN)
                                .description("If the conversation has a finished state or not."),
                        fieldWithPath(prefix + "expiry")
                                .type(JsonFieldType.STRING)
                                .description("Expiry timestamp"),
                        fieldWithPath(prefix + "direction")
                                .type(JsonFieldType.STRING)
                                .description(String.format("The direction. Can be one of: %s", Arrays.stream(ConversationDirection.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                        fieldWithPath(prefix + "serviceIdentifier")
                                .type(JsonFieldType.STRING)
                                .description(String.format("The service identifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                        fieldWithPath(prefix + "messageStatuses")
                                .type(JsonFieldType.ARRAY)
                                .description("An array of message statuses.")
                ).fields(conversationMessageStatusDescriptors(prefix + "messageStatuses[]."))
                .build();
    }

    private static FieldDescriptor[] conversationMessageStatusDescriptors(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "id")
                        .type(JsonFieldType.NUMBER)
                        .description("Integer. The numeric message status ID."),
                statusDescription(prefix),
                fieldWithPath(prefix + "description")
                        .type(JsonFieldType.STRING)
                        .description("Description."),
                fieldWithPath(prefix + "lastUpdate")
                        .type(JsonFieldType.STRING)
                        .description("Date and time of status.")
        };
    }

    static List<FieldDescriptor> standardBusinessDocumentHeaderDescriptors(String prefix) {
        ConstrainedFields sbdhFields = new ConstrainedFields(StandardBusinessDocumentHeader.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        sbdhFields.withPath("headerVersion")
                                .type(JsonFieldType.STRING)
                                .description("Header version. Expected value is 1.0"),
                        sbdhFields.withPath("sender")
                                .type(JsonFieldType.ARRAY)
                                .description("Logical party representing the\n" +
                                        "organization that has created the standard business document."),
                        sbdhFields.withPath("receiver")
                                .type(JsonFieldType.ARRAY)
                                .description("Logical party representing the\n" +
                                        "organization that receives the SBD."),
                        sbdhFields.withPath("documentIdentification")
                                .type(JsonFieldType.OBJECT)
                                .description("Characteristics containing identification about the document."),
                        sbdhFields.withPath("businessScope")
                                .type(JsonFieldType.OBJECT)
                                .description("The business scope\n" +
                                        "contains 1 to many [1..*] scopes. It is not mandatory to put all intermediary\n" +
                                        "scopes in an SBDH. Only those scopes that the parties agree to are valid. The\n" +
                                        "following examples are all valid: transaction; business process; collaboration. A\n" +
                                        "Profile may be used to group well-formedness rules together. The business\n" +
                                        "scope block consists of the Scope block.")
                )
                .fields(senderDescriptors(prefix + "sender[]."))
                .fields(receiverDescriptors(prefix + "receiver[]."))
                .fields(documentIdentificationDescriptors(prefix + "documentIdentification."))
                .fields(businessScopeDescriptors(prefix + "businessScope."))
                .build();
    }

    private static FieldDescriptor[] senderDescriptors(String prefix) {
        ConstrainedFields senderFields = new ConstrainedFields(PartnerIdentification.class, prefix, ValidationGroups.Partner.Sender.class);

        return new FieldDescriptor[]{
                senderFields.withPath("identifier.value")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                senderFields.withPath("identifier.authority")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor that qualifies the identifier used to identify the sending party."),
                senderFields.withPath("contactInformation[]")
                        .optional()
                        .ignored()
        };
    }

    private static FieldDescriptor[] receiverDescriptors(String prefix) {
        ConstrainedFields receiverFields = new ConstrainedFields(PartnerIdentification.class, prefix, ValidationGroups.Partner.Receiver.class);

        return new FieldDescriptor[]{
                receiverFields.withPath("identifier.value")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                receiverFields.withPath("identifier.authority")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor that qualifies the identifier used to identify the receiving party."),
                receiverFields.withPath("contactInformation[]")
                        .optional()
                        .ignored()
        };
    }

    private static FieldDescriptor[] documentIdentificationDescriptors(String prefix) {
        ConstrainedFields documentIdentificationFields = new ConstrainedFields(DocumentIdentification.class, prefix);

        return new FieldDescriptor[]{
                documentIdentificationFields.withPath("standard")
                        .type(JsonFieldType.STRING)
                        .description("The\n" +
                        "originator of the type of the Business Data standard, e.g. SWIFT, OAG,\n" +
                        "EAN.UCC, EDIFACT, X12; references which Data Dictionary is being\n" +
                        "used. Used for the task of verifying that the grammar of a message is\n" +
                        "valid"),
                documentIdentificationFields.withPath("typeVersion")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor which contains versioning information or number of\n" +
                        "the standard that defines the document which is specified in the ’Type’\n" +
                        "data element, e.g. values could be ‘1.3’ or ‘D.96A’, etc. . This is the\n" +
                        "version of the document itself and is different than the HeaderVersion."),
                documentIdentificationFields.withPath("instanceIdentifier")
                        .type(JsonFieldType.STRING)
                        .description("Descriptor which contains reference information which uniquely identifies\n" +
                        "this instance of the SBD between the sender and the receiver. This\n" +
                        "identifier identifies this document as distinct from others. There is only\n" +
                        "one SBD instance per Standard Header. The Instance Identifier is \n" +
                        "automatically generated as an UUID if not specified."),
                documentIdentificationFields.withPath("type")
                        .type(JsonFieldType.STRING)
                        .description("A logical indicator\n" +
                        "representing the type of Business Data being sent or the named type of\n" +
                        "business data. This attribute identifies the type of document and not the\n" +
                        "instance of that document. The instance document or interchange can\n" +
                        "contain one or more business documents of a single document type or\n" +
                        "closely related types. The industry standard body (as referenced in the\n" +
                        "‘Standard’ element) is responsible for defining the Type value to be used\n" +
                        "in this field. Currently NextMove supports the following types: " + no.difi.meldingsutveksling.MessageType.stream()
                        .map(no.difi.meldingsutveksling.MessageType::getType)
                        .collect(Collectors.joining(", "))
                ),
                documentIdentificationFields.withPath("creationDateAndTime")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("Descriptor which contains date and time of SBDH/document\n" +
                        "creation. In the SBDH the parser translator or service component assigns\n" +
                        "the SBD a Date and Time stamp. The creation date and time expressed\n" +
                        "here most likely will be different from the date and time stamped in the\n" +
                        "transport envelope.")
        };
    }

    private static List<FieldDescriptor> businessScopeDescriptors(String prefix) {
        ConstrainedFields businessScopeFields = new ConstrainedFields(BusinessScope.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        businessScopeFields.withPath("scope")
                                .type(JsonFieldType.ARRAY)
                                .description("Indicates the type of scope,\n" +
                                        "the identifiers for the scope, other supporting information and the scope\n" +
                                        "content itself. The importance of the Scope is that it allows the SBDH to\n" +
                                        "operate under auspices of an agreement; that parties agree that they only\n" +
                                        "include reference agreements")
                ).fields(scopeDescriptors(prefix + "scope[]."))
                .build();
    }

    private static List<FieldDescriptor> scopeDescriptors(String prefix) {
        ConstrainedFields scopeFields = new ConstrainedFields(Scope.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        scopeFields.withPath("type")
                                .type(JsonFieldType.STRING)
                                .description("Indicates the kind of\n" +
                                        "scope; an attribute describing the Scope. Example entries include: ConversationId, SenderRef, ReceiverRef"),
                        scopeFields.withPath("instanceIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("A unique identifier that references the instance of the scope (e.g.\n" +
                                        "process execution instance, document instance). For example, the\n" +
                                        "Instance Identifier could be used to identify the specific instance of\n" +
                                        "a Business Process. This identifier would be used to correlate all\n" +
                                        "the way back to the business domain layer; it can be thought of as\n" +
                                        "a session descriptor at the business domain application level."),
                        scopeFields.withPath("identifier")
                                .type(JsonFieldType.STRING)
                                .description("An optional unique\n" +
                                        "descriptor that identifies the \"contract\" or \"agreement\" that this\n" +
                                        "instance relates to. It operates at the level of business domain, not\n" +
                                        "at the transport or messaging level, by providing the information\n" +
                                        "necessary and sufficient to configure the service at the other\n" +
                                        "partner's end."),
                        scopeFields.withPath("scopeInformation")
                                .type(JsonFieldType.ARRAY)
                                .description("An optional unique\n" +
                                        "descriptor that identifies the \"contract\" or \"agreement\" that this\n" +
                                        "instance relates to. It operates at the level of business domain, not\n" +
                                        "at the transport or messaging level, by providing the information\n" +
                                        "necessary and sufficient to configure the service at the other\n" +
                                        "partner's end.")
                )
                .fields(scopeInformationDescriptors(prefix + "scopeInformation[]."))
                .build();
    }

    private static FieldDescriptor[] scopeInformationDescriptors(String prefix) {
        ConstrainedFields correlationInformationFields = new ConstrainedFields(CorrelationInformation.class, prefix);

        return new FieldDescriptor[]{
                correlationInformationFields.withPath("expectedResponseDateTime")
                        .type(JsonFieldType.STRING)
                        .description("Date and time when response is expected. This element could be\n" +
                        "populated in an initial message of a correlation sequence, and should be\n" +
                        "echoed back in a subsequent response. ")
        };
    }

    private static FieldDescriptor sikkerhetsnivaaDescriptor(ConstrainedFields messageFields) {
        return messageFields.withPath("sikkerhetsnivaa")
                .type(JsonFieldType.VARIES)
                .optional()
                .description("Defines the authentication level required for the document to be opened.");
    }

    private static FieldDescriptor hoveddokumentDescriptor(ConstrainedFields messageFields, String additionalDescription) {
        return messageFields.withPath("hoveddokument")
                .type(JsonFieldType.VARIES)
                .optional()
                .description("Name of the attachment that is the main document. " +
                        "Especially when there are more than one attachment, there " +
                        "is a need to know which document is the main one. "
                        + additionalDescription
                );
    }

    static List<FieldDescriptor> arkivmeldingMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(ArkivmeldingMessage.class, prefix, NextMoveValidationGroups.MessageType.Arkivmelding.class);
        return new FieldDescriptorsBuilder()
                .fields(hoveddokumentDescriptor(messageFields, "Should only be specified for DPF."))
                .build();
    }

    static List<FieldDescriptor> dpiDigitalMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(DpiDigitalMessage.class, prefix, NextMoveValidationGroups.MessageType.Digital.class);

        return new FieldDescriptorsBuilder()
                .fields(hoveddokumentDescriptor(messageFields, ""), sikkerhetsnivaaDescriptor(messageFields))
                .fields(
                        messageFields.withPath("tittel")
                                .type(JsonFieldType.VARIES)
                                .description("The title of the message."),
                        messageFields.withPath("spraak")
                                .type(JsonFieldType.VARIES)
                                .description("Language of the message."),
                        messageFields.withPath("digitalPostInfo")
                                .type(JsonFieldType.VARIES)
                                .description("Language of the message."),
                        messageFields.withPath("varsler")
                                .type(JsonFieldType.VARIES)
                                .description("Information on how the mailbox supplier should notify the Recipient of the new mail. " +
                                        "Receiver overrides its own notification preferences"),
                        messageFields.withPath("metadataFiler")
                                .type(JsonFieldType.VARIES)
                                .description("Map of metadatadocuments.")
                )
                .fields(digitalPostInfoDescriptors(prefix + "digitalPostInfo."))
                .fields(dpiNotificationDescriptors(prefix + "varsler."))
                .build();
    }

    private static List<FieldDescriptor> digitalPostInfoDescriptors(String prefix) {
        ConstrainedFields digitalPostInfoFields = new ConstrainedFields(DigitalPostInfo.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        digitalPostInfoFields.withPath("virkningsdato")
                                .type(JsonFieldType.VARIES)
                                .description("Date for when a message is to be made available to the Resident in the Resident's mailbox. " +
                                        "The document will be delivered to the mailbox before this time, but will not be visible / accessible to Citizen. " +
                                        "Note that the field is only a DATE and cannot be controlled at the time. In practice, " +
                                        "this means that the mail will be made available on the night of the business day and " +
                                        "that the mailbox can spend all day accessing mail with the same effective date."),
                        digitalPostInfoFields.withPath("aapningskvittering")
                                .type(JsonFieldType.VARIES)
                                .description("If you want a receipt when the recipient has opened the document.")
                ).build();
    }

    private static List<FieldDescriptor> dpiNotificationDescriptors(String prefix) {
        ConstrainedFields dpiNotificationFields = new ConstrainedFields(DpiNotification.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        dpiNotificationFields.withPath("epostTekst")
                                .type(JsonFieldType.VARIES)
                                .optional()
                                .description("Email content"),
                        dpiNotificationFields.withPath("smsTekst")
                                .type(JsonFieldType.VARIES)
                                .optional()
                                .description("SMS content")
                ).build();
    }

    static List<FieldDescriptor> dpiPrintMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(DpiPrintMessage.class, prefix, NextMoveValidationGroups.MessageType.Print.class);

        return new FieldDescriptorsBuilder()
                .fields(hoveddokumentDescriptor(messageFields, ""))
                .fields(
                        messageFields.withPath("mottaker")
                                .type(JsonFieldType.OBJECT)
                                .description("Postal address of the recipient."),
                        messageFields.withPath("utskriftsfarge")
                                .type(JsonFieldType.STRING)
                                .description(String.format("Used to specify type of print. Can be one of: %s", Arrays.stream(PrintColor.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                        messageFields.withPath("posttype")
                                .type(JsonFieldType.STRING)
                                .description(String.format("Mail type. Can be one of: %s", Arrays.stream(Utskrift.Posttype.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                        messageFields.withPath("retur")
                                .type(JsonFieldType.OBJECT)
                                .description("Return address to be placed on the back of the envelope."),
                        messageFields.withPath("printinstruksjoner")
                                .type(JsonFieldType.OBJECT)
                                .description("Map of print instructions")
                )
                .fields(postAddressDescriptors(prefix + "mottaker."))
                .fields(mailReturnDescriptors(prefix + "retur."))
                .build();
    }

    private static FieldDescriptor[] postAddressDescriptors(String prefix) {
        ConstrainedFields fields = new ConstrainedFields(PostAddress.class, prefix);

        return new FieldDescriptor[]{
                fields.withPath("navn")
                        .type(JsonFieldType.STRING)
                        .description("Name of the recipient."),
                fields.withPath("adresselinje1")
                        .type(JsonFieldType.STRING)
                        .description("Address line 1. Usually the street address."),
                fields.withPath("adresselinje2")
                        .type(JsonFieldType.STRING)
                        .description("Address line 2."),
                fields.withPath("adresselinje3")
                        .type(JsonFieldType.STRING)
                        .description("Address line 3."),
                fields.withPath("adresselinje4")
                        .type(JsonFieldType.STRING)
                        .description("Address line 4."),
                fields.withPath("postnummer")
                        .type(JsonFieldType.STRING)
                        .description("Postal code."),
                fields.withPath("poststed")
                        .type(JsonFieldType.STRING)
                        .description("City / Postal area."),
                fields.withPath("land")
                        .type(JsonFieldType.STRING)
                        .description("Name of country.")
        };
    }

    private static List<FieldDescriptor> mailReturnDescriptors(String prefix) {
        ConstrainedFields fields = new ConstrainedFields(MailReturn.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        fields.withPath("mottaker")
                                .type(JsonFieldType.OBJECT)
                                .description("Postal address of the sender."),
                        fields.withPath("returhaandtering")
                                .type(JsonFieldType.STRING)
                                .description(String.format("Used to specify type of return. Can be one of: %s", Arrays.stream(Retur.Returposthaandtering.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", "))))
                )
                .fields(postAddressDescriptors(prefix + "mottaker."))
                .build();
    }

    static FieldDescriptor[] digitalDpvMessageDescriptors(String prefix) {
        ConstrainedFields fields = new ConstrainedFields(DigitalDpvMessage.class, prefix);

        return new FieldDescriptor[]{
                fields.withPath("tittel")
                        .type(JsonFieldType.STRING)
                        .description("The title."),
                fields.withPath("sammendrag")
                        .type(JsonFieldType.STRING)
                        .description("Summary."),
                fields.withPath("innhold")
                        .type(JsonFieldType.STRING)
                        .description("The document content.")
        };
    }

    static List<FieldDescriptor> innsynskravMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(InnsynskravMessage.class, prefix, NextMoveValidationGroups.MessageType.Innsynskrav.class);

        return new FieldDescriptorsBuilder()
                .fields(
                        messageFields.withPath("orgnr")
                                .type(JsonFieldType.VARIES)
                                .description("Business registration number for the organization that you want to gain insight to."),
                        messageFields.withPath("epost")
                                .type(JsonFieldType.VARIES)
                                .description("E-mail of the recipient of the inquiry.")
                )
                .build();
    }

    static List<FieldDescriptor> publiseringMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(PubliseringMessage.class, prefix, NextMoveValidationGroups.MessageType.Innsynskrav.class);

        return new FieldDescriptorsBuilder()
                .fields(
                        messageFields.withPath("orgnr")
                                .type(JsonFieldType.VARIES)
                                .description("Business registration number.")
                )
                .build();
    }

    static List<FieldDescriptor> subscriptionInputDescriptors(String prefix, Class<?> group) {
        return subscriptionDescriptors(prefix, false, group);
    }

    static List<FieldDescriptor> subscriptionDescriptors(String prefix, Class<?> group) {
        return subscriptionDescriptors(prefix, true, group);
    }

    private static List<FieldDescriptor> subscriptionDescriptors(String prefix, boolean isResponse, Class<?> group) {
        ConstrainedFields constrainedFields = new ConstrainedFields(Subscription.class, prefix, group);

        FieldDescriptorsBuilder builder = new FieldDescriptorsBuilder();

        if (isResponse) {
            builder.fields(constrainedFields.withPath("id")
                    .type(JsonFieldType.NUMBER)
                    .description("The resource identifier.")
            );
        }

        return builder
                .fields(
                        constrainedFields.withPath("name")
                                .type(JsonFieldType.STRING)
                                .description("A label to remember why it was created. Use it for whatever purpose you'd like."),
                        constrainedFields.withPath("pushEndpoint")
                                .type(JsonFieldType.STRING)
                                .description("URL to push the webhook messages to."),
                        constrainedFields.withPath("resource")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("Indicates the noun being observed."),
                        constrainedFields.withPath("event")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("Further narrows the events by specifying the action that would trigger a notification to your backend."),
                        constrainedFields.withPath("filter")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("A set of filtering criteria. Generally speaking, webhook filters will be a subset of the query parameters available when GETing a list of the target resource. It is an optional property. To add multiple filters, separate them with the “&” symbol. Supported filters are: status, serviceIdentifier, direction.")
                ).build();
    }

    static List<FieldDescriptor> capabilitiesDescriptors() {
        return new FieldDescriptorsBuilder()
                .fields(capabilityDescriptors("capabilities[]."))
                .build();
    }

    private static List<FieldDescriptor> capabilityDescriptors(String prefix) {
        return new FieldDescriptorsBuilder()
                .fields(
                        fieldWithPath(prefix + "process")
                                .type(JsonFieldType.STRING)
                                .description("Type of process."),
                        fieldWithPath(prefix + "serviceIdentifier")
                                .type(JsonFieldType.STRING)
                                .description(String.format("The service identifier. Can be one of: %s", Arrays.stream(ServiceIdentifier.values())
                                        .map(Enum::name)
                                        .collect(Collectors.joining(", ")))),
                        fieldWithPath(prefix + "postAddress")
                                .optional()
                                .type(JsonFieldType.OBJECT)
                                .description("An postal address."),
                        fieldWithPath(prefix + "returnAddress")
                                .optional()
                                .type(JsonFieldType.OBJECT)
                                .description("An return address."),
                        fieldWithPath(prefix + "documentTypes")
                                .type(JsonFieldType.ARRAY)
                                .description("An postal address.")
                )
                .fields(postalAddressDescriptors(prefix + "postAddress."))
                .fields(postalAddressDescriptors(prefix + "returnAddress."))
                .fields(documentTypeDescriptors(prefix + "documentTypes[]."))
                .build();
    }

    private static FieldDescriptor[] postalAddressDescriptors(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "name")
                        .type(JsonFieldType.STRING)
                        .description("Name of the recipient."),
                fieldWithPath(prefix + "street")
                        .type(JsonFieldType.STRING)
                        .description("Street name"),
                fieldWithPath(prefix + "postalCode")
                        .type(JsonFieldType.STRING)
                        .description("Postal code."),
                fieldWithPath(prefix + "postalArea")
                        .type(JsonFieldType.STRING)
                        .description("City / Postal area."),
                fieldWithPath(prefix + "country")
                        .type(JsonFieldType.STRING)
                        .description("Country.")
        };
    }

    private static FieldDescriptor[] documentTypeDescriptors(String prefix) {
        return new FieldDescriptor[]{
                fieldWithPath(prefix + "type")
                        .type(JsonFieldType.STRING)
                        .description("Message type. This is always identical to the last part of the standard."),
                fieldWithPath(prefix + "standard")
                        .type(JsonFieldType.STRING)
                        .description("Document type.")
        };
    }
}