package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.CmsEncryptedAsice;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Dokumentpakkefingeravtrykk;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@RequiredArgsConstructor
public class CreateParcelFingerprint {

    public Dokumentpakkefingeravtrykk createParcelFingerprint(CmsEncryptedAsice cmsEncryptedAsice) {
        try (InputStream inputStream = cmsEncryptedAsice.getResource().getInputStream()) {
            return new Dokumentpakkefingeravtrykk()
                    .setDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256")
                    .setDigestValue(Base64.getEncoder().encodeToString(DigestUtils.sha256(inputStream)));
        } catch (IOException e) {
            throw new Exception("Failed to create parcel fingerprint", e);
        }
    }

    private static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
