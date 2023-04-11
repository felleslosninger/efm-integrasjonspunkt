package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.dokumentpakking.domain.Parcel;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.move.common.cert.X509CertificateHelper;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ShipmentFactory {

    private final FileExtensionMapper fileExtensionMapper;

    public Shipment getShipment(DpiTestInput input) {
        return new Shipment()
                .setSender(input.getSender())
                .setReceiver(input.getReceiver())
                .setMessageId(input.getMessageId())
                .setConversationId(input.getConversationId())
                .setExpectedResponseDateTime(input.getExpectedResponseDateTime())
                .setBusinessMessage(input.getBusinessMessage())
                .setParcel(getParcel(input))
                .setReceiverBusinessCertificate(getReceiverCertificate(input))
                .setLanguage("NO");
    }

    private X509Certificate getReceiverCertificate(DpiTestInput input) {
        try (InputStream inputStream = input.getReceiverCertificate().getInputStream()) {
            return X509CertificateHelper.createX509Certificate(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't get receiver certificate!", e);
        }
    }

    private Parcel getParcel(DpiTestInput input) {
        return Parcel.builder()
                .mainDocument(getDocument(input.getMainDocument()))
                .attachments(input.getAttachments()
                        .stream()
                        .map(this::getDocument)
                        .collect(Collectors.toList()))
                .build();
    }

    private Document getDocument(Resource resource) {
        return Document.builder()
                .title(resource.getDescription())
                .filename(resource.getFilename())
                .resource(resource)
                .mimeType(fileExtensionMapper.getMimetype(resource))
                .build();
    }
}
