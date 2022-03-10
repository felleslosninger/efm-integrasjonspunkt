package no.difi.meldingsutveksling.dpi.client.internal.domain;

import lombok.Data;
import org.springframework.core.io.Resource;

@Data
public class SendMessageInput {

    private String maskinportentoken;
    private String jwt;
    private Resource cmsEncryptedAsice;
    private String channel;
}
