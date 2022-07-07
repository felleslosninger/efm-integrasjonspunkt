package no.difi.meldingsutveksling.dpi.client.internal;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.Dokumentpakkefingeravtrykk;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@RequiredArgsConstructor
public class CreateParcelFingerprint {

    public Dokumentpakkefingeravtrykk createParcelFingerprint(Resource cmsEncryptedAsice) {
        try (InputStream inputStream = cmsEncryptedAsice.getInputStream()) {
            return new Dokumentpakkefingeravtrykk()
                    .setDigestMethod("http://www.w3.org/2001/04/xmlenc#sha256")
                    .setDigestValue(Base64.getEncoder().encodeToString(DigestUtils.sha256(inputStream)));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create parcel fingerprint", e);
        }
    }
}
