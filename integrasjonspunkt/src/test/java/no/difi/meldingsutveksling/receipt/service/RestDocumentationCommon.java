package no.difi.meldingsutveksling.receipt.service;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.ApiType;
import no.difi.meldingsutveksling.DocumentType;
import no.difi.meldingsutveksling.domain.sbdh.DocumentIdentification;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.receipt.ReceiptStatus;
import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.RequestFieldsSnippet;
import org.springframework.restdocs.payload.ResponseFieldsSnippet;
import org.springframework.restdocs.request.ParameterDescriptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.springframework.restdocs.payload.PayloadDocumentation.*;
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

    static RequestFieldsSnippet requestFieldsStandardBusinessDocument() {
        ConstrainedFields sbdhFields = new ConstrainedFields(StandardBusinessDocumentHeader.class);
        ConstrainedFields partnerIdentificationFields = new ConstrainedFields(PartnerIdentification.class);
        ConstrainedFields documentIdentificationFields = new ConstrainedFields(DocumentIdentification.class);

        return requestFields()
                .andWithPrefix("standardBusinessDocumentHeader.",
                        sbdhFields.withPath("headerVersion")
                                .type(JsonFieldType.STRING)
                                .description("Header version. Expected value is 1.0")
                )
                .andWithPrefix("standardBusinessDocumentHeader.sender[].identifier.",
                        partnerIdentificationFields.withPath("value")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                        partnerIdentificationFields.withPath("authority")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor that qualifies the identifier used to identify the sending party.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.sender[].",
                        subsectionWithPath("contactInformation[]")
                                .ignored()
                )
                .andWithPrefix("standardBusinessDocumentHeader.receiver[].identifier.",
                        partnerIdentificationFields.withPath("value")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                        partnerIdentificationFields.withPath("authority")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor that qualifies the identifier used to identify the receiving party.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.receiver[].",
                        subsectionWithPath("contactInformation[]")
                                .ignored()
                )
                .andWithPrefix("standardBusinessDocumentHeader.documentIdentification.",
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
                                .optional()
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
                )
                .andWithPrefix("standardBusinessDocumentHeader.businessScope.scope[].",
                        fieldWithPath("type")
                                .type(JsonFieldType.STRING)
                                .description("Indicates the kind of\n" +
                                        "scope; an attribute describing the Scope. Example entries include: ConversationId, SenderRef, ReceiverRef"),
                        fieldWithPath("instanceIdentifier")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("A unique identifier that references the instance of the scope (e.g.\n" +
                                        "process execution instance, document instance). For example, the\n" +
                                        "Instance Identifier could be used to identify the specific instance of\n" +
                                        "a Business Process. This identifier would be used to correlate all\n" +
                                        "the way back to the business domain layer; it can be thought of as\n" +
                                        "a session descriptor at the business domain application level."),
                        fieldWithPath("identifier")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("An optional unique\n" +
                                        "descriptor that identifies the \"contract\" or \"agreement\" that this\n" +
                                        "instance relates to. It operates at the level of business domain, not\n" +
                                        "at the transport or messaging level, by providing the information\n" +
                                        "necessary and sufficient to configure the service at the other\n" +
                                        "partner's end.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.businessScope.scope[].scopeInformation[].",
                        fieldWithPath("expectedResponseDateTime")
                                .type(JsonFieldType.STRING)
                                .optional()
                                .description("Date and time when response is expected. This element could be\n" +
                                        "populated in an initial message of a correlation sequence, and should be\n" +
                                        "echoed back in a subsequent response. ")
                );
    }

    static ResponseFieldsSnippet responseFieldsStandardBusinessDocument() {
        return responseFields()
                .andWithPrefix("standardBusinessDocumentHeader.",
                        fieldWithPath("headerVersion")
                                .type(JsonFieldType.STRING)
                                .description("Header version. Expected value is 1.0")
                )
                .andWithPrefix("standardBusinessDocumentHeader.sender[].identifier.",
                        fieldWithPath("value")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                        fieldWithPath("authority")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor that qualifies the identifier used to identify the sending party.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.sender[].",
                        subsectionWithPath("contactInformation[]")
                                .ignored()
                )
                .andWithPrefix("standardBusinessDocumentHeader.receiver[].identifier.",
                        fieldWithPath("value")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor with information to identify this party. Requires a 0192: prefix for all norwegian organizations. Prefix is not required for individuals."),
                        fieldWithPath("authority")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor that qualifies the identifier used to identify the receiving party.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.receiver[].",
                        subsectionWithPath("contactInformation[]")
                                .ignored()
                )
                .andWithPrefix("standardBusinessDocumentHeader.documentIdentification.",
                        fieldWithPath("standard")
                                .type(JsonFieldType.STRING)
                                .description("The\n" +
                                        "originator of the type of the Business Data standard, e.g. SWIFT, OAG,\n" +
                                        "EAN.UCC, EDIFACT, X12; references which Data Dictionary is being\n" +
                                        "used. Used for the task of verifying that the grammar of a message is\n" +
                                        "valid"),
                        fieldWithPath("typeVersion")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor which contains versioning information or number of\n" +
                                        "the standard that defines the document which is specified in the ’Type’\n" +
                                        "data element, e.g. values could be ‘1.3’ or ‘D.96A’, etc. . This is the\n" +
                                        "version of the document itself and is different than the HeaderVersion."),
                        fieldWithPath("instanceIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor which contains reference information which uniquely identifies\n" +
                                        "this instance of the SBD between the sender and the receiver. This\n" +
                                        "identifier identifies this document as distinct from others. There is only\n" +
                                        "one SBD instance per Standard Header. The Instance Identifier is \n" +
                                        "automatically generated as an UUID if not specified."),
                        fieldWithPath("type")
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
                        fieldWithPath("creationDateAndTime")
                                .type(JsonFieldType.STRING)
                                .description("Descriptor which contains date and time of SBDH/document\n" +
                                        "creation. In the SBDH the parser translator or service component assigns\n" +
                                        "the SBD a Date and Time stamp. The creation date and time expressed\n" +
                                        "here most likely will be different from the date and time stamped in the\n" +
                                        "transport envelope.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.businessScope.scope[].",
                        fieldWithPath("type")
                                .type(JsonFieldType.STRING)
                                .description("Indicates the kind of\n" +
                                        "scope; an attribute describing the Scope. Example entries include: ConversationId, SenderRef, ReceiverRef"),
                        fieldWithPath("instanceIdentifier")
                                .type(JsonFieldType.STRING)
                                .description("A unique identifier that references the instance of the scope (e.g.\n" +
                                        "process execution instance, document instance). For example, the\n" +
                                        "Instance Identifier could be used to identify the specific instance of\n" +
                                        "a Business Process. This identifier would be used to correlate all\n" +
                                        "the way back to the business domain layer; it can be thought of as\n" +
                                        "a session descriptor at the business domain application level."),
                        fieldWithPath("identifier")
                                .type(JsonFieldType.STRING)
                                .description("An optional unique\n" +
                                        "descriptor that identifies the \"contract\" or \"agreement\" that this\n" +
                                        "instance relates to. It operates at the level of business domain, not\n" +
                                        "at the transport or messaging level, by providing the information\n" +
                                        "necessary and sufficient to configure the service at the other\n" +
                                        "partner's end.")
                )
                .andWithPrefix("standardBusinessDocumentHeader.businessScope.scope[].scopeInformation[].",
                        fieldWithPath("expectedResponseDateTime")
                                .type(JsonFieldType.STRING)
                                .description("Date and time when response is expected. This element could be\n" +
                                        "populated in an initial message of a correlation sequence, and should be\n" +
                                        "echoed back in a subsequent response. ")
                );
    }
}
