package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.BusinessCertificate;
import no.difi.meldingsutveksling.dpi.client.domain.Document;
import no.difi.meldingsutveksling.dpi.client.domain.Parcel;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
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

    private BusinessCertificate getReceiverCertificate(DpiTestInput input) {
        try (InputStream inputStream = input.getReceiverCertificate().getInputStream()) {
            return BusinessCertificate.of(IOUtils.toByteArray(inputStream));
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't get receiver certificate!", e);
        }
    }

    private Parcel getParcel(DpiTestInput input) {
        return new Parcel()
                .setMainDocument(getDocument(input.getMainDocument()))
                .setAttachments(input.getAttachments()
                        .stream()
                        .map(this::getDocument)
                        .collect(Collectors.toList()));
    }

    private Document getDocument(Resource resource) {
        return new Document()
                .setTitle(resource.getDescription())
                .setFilename(resource.getFilename())
                .setResource(resource)
                .setMimeType(fileExtensionMapper.getMimetype(resource));
    }

}
