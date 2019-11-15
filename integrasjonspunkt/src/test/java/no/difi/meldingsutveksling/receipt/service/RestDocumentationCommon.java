package no.difi.meldingsutveksling.receipt.service;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.domain.sbdh.*;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingMessage;
import no.difi.meldingsutveksling.nextmove.DigitalPostInfo;
import no.difi.meldingsutveksling.nextmove.DpiDigitalMessage;
import no.difi.meldingsutveksling.nextmove.DpiNotification;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import no.difi.meldingsutveksling.validation.group.ValidationGroups;
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

    static FieldDescriptor[] getPageFieldDescriptors() {
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

    static FieldDescriptor[] getPageableFieldDescriptors() {
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

    static FieldDescriptor[] getMessageStatusFieldDescriptors() {
        return new FieldDescriptor[]{
                fieldWithPath("id")
                        .type(JsonFieldType.NUMBER)
                        .description("Integer. The numeric message status ID."),
                fieldWithPath("convId")
                        .type(JsonFieldType.NUMBER)
                        .description("Integer. The numeric conversation ID."),
                fieldWithPath("messageId")
                        .type(JsonFieldType.STRING)
                        .description("The messageId. Typically an UUID"),
                fieldWithPath("conversationId")
                        .type(JsonFieldType.STRING)
                        .description("The conversationId. Typically an UUID"),
                fieldWithPath("status")
                        .type(JsonFieldType.STRING)
                        .description(String.format("The message status. Can be one of: %s", Arrays.stream(ReceiptStatus.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", ")))),
                fieldWithPath("description")
                        .type(JsonFieldType.STRING)
                        .description("Description"),
                fieldWithPath("lastUpdate")
                        .type(JsonFieldType.STRING)
                        .description("Date and time of status")
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
                        "in this field. Currently NextMove supports the following types: " + DocumentType.stream(ApiType.NEXTMOVE)
                        .map(DocumentType::getType)
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

    static FieldDescriptor[] arkivmeldingMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(ArkivmeldingMessage.class, prefix);

        return new FieldDescriptor[]{
                messageFields.withPath("hoveddokument")
                        .type(JsonFieldType.VARIES)
                        .optional()
                        .description("Name of the attachment that is the main document.")
        };
    }

    static List<FieldDescriptor> dpiDigitalMessageDescriptors(String prefix) {
        ConstrainedFields messageFields = new ConstrainedFields(DpiDigitalMessage.class, prefix);

        return new FieldDescriptorsBuilder()
                .fields(
                        messageFields.withPath("sikkerhetsnivaa")
                                .type(JsonFieldType.VARIES)
                                .description("Defines the authentication level required for the document to be opened."),
                        messageFields.withPath("hoveddokument")
                                .type(JsonFieldType.VARIES)
                                .optional()
                                .description("Name of the attachment that is the main document."),
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
                                        "Receiver overrides its own notification preferences")
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
}