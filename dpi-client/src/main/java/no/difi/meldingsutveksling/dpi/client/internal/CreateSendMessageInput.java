package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.internal.domain.SendMessageInput;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateSendMessageInput {

    private final CreateMaskinportenToken createMaskinportenToken;
    private final CreateStandardBusinessDocument createStandardBusinessDocument;
    private final no.difi.meldingsutveksling.dpi.client.internal.CreateStandardBusinessDocumentJWT createStandardBusinessDocumentJWT;

    public SendMessageInput createSendMessageInput(Shipment shipment, CmsEncryptedAsice cmsEncryptedAsice) {
        String maskinportenToken = createMaskinportenToken.createMaskinportenTokenForSending(shipment.getBusinessMessage().getAvsender());
        StandardBusinessDocument sbd = createStandardBusinessDocument.createStandardBusinessDocument(shipment);
        String jwt = createStandardBusinessDocumentJWT.createStandardBusinessDocumentJWT(sbd, cmsEncryptedAsice, maskinportenToken);
        return new SendMessageInput()
                .setMaskinportentoken(maskinportenToken)
                .setJwt(jwt)
                .setCmsEncryptedAsice(cmsEncryptedAsice)
                .setChannel(shipment.getChannel());
    }
}
