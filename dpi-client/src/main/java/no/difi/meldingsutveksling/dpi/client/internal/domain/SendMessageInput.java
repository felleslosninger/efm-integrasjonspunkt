package no.difi.meldingsutveksling.dpi.client.internal.domain;

import lombok.Data;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;

@Data
public class SendMessageInput {

    private String maskinportentoken;
    private String jwt;
    private CmsEncryptedAsice cmsEncryptedAsice;
    private String channel;
}
