package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import org.bouncycastle.cms.CMSException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateEncodingException;

@Slf4j
@RequiredArgsConstructor
public class CreateCmsEncryptedAsice {

    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;
    private final CreateAsice createASiCE;
    private final CreateCMSDocument createCMS;

    public CmsEncryptedAsice createCmsEncryptedAsice(Shipment shipment) {
        return new CmsEncryptedAsice(createCMS(shipment, createAsice(shipment)));
    }

    private InMemoryWithTempFileFallbackResource createCMS(Shipment shipment, InMemoryWithTempFileFallbackResource asic) {
        InMemoryWithTempFileFallbackResource cms = resourceFactory.getResource("dpi-", ".asic.cms");

        try (InputStream inputStream = asic.getInputStream(); OutputStream outputStream = cms.getOutputStream()) {
            createCMS.createCMS(inputStream, outputStream, shipment.getReceiverBusinessCertificate());
        } catch (IOException e) {
            throw new IllegalStateException("CMS encryption failed!", e);
        }
        return cms;
    }

    private InMemoryWithTempFileFallbackResource createAsice(Shipment shipment) {
        InMemoryWithTempFileFallbackResource asic = resourceFactory.getResource("dpi-", ".asic");

        try (OutputStream outputStream = asic.getOutputStream()) {
            createASiCE.createAsice(shipment, outputStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Creating ASiC-E failed!", e);
        }
        return asic;
    }
}
