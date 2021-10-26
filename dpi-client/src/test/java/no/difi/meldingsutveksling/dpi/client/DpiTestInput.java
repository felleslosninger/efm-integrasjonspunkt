package no.difi.meldingsutveksling.dpi.client;

import lombok.Data;
import no.difi.meldingsutveksling.domain.sbdh.PartnerIdentification;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.BusinessMessage;
import org.springframework.core.io.Resource;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class DpiTestInput {

    private PartnerIdentification senderOrganizationIdentifier;
    private PartnerIdentification receiverOrganizationIdentifier;
    private String messageId;
    private String conversationId;
    private OffsetDateTime expectedResponseDateTime;
    private BusinessMessage businessMessage;
    private Resource mainDocument;
    private List<Resource> attachments;
    private String mailbox;
    private Resource receiverCertificate;
}
