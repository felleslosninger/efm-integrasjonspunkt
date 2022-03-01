package no.difi.meldingsutveksling.dpi.client.internal;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.cert.X509CertificateHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class CreateJWT {

    private final JWSHeader jwsHeader;
    private final JWSSigner jwsSigner;

    public CreateJWT(KeystoreHelper keystoreHelper) {
        this.jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .x509CertChain(getX509CertChain(keystoreHelper))
                .build();
        this.jwsSigner = new RSASSASigner(keystoreHelper.loadPrivateKey());
    }

    private List<Base64> getX509CertChain(KeystoreHelper keystoreHelper) {
        return Collections.singletonList(Base64.encode(X509CertificateHelper.getEncoded(keystoreHelper.getX509Certificate())));
    }

    public String createJWT(Map<String, Object> sbd) {
        JWSObject jwsObject = getJwsObject(sbd);
        sign(jwsObject);
        return jwsObject.serialize();
    }

    private void sign(JWSObject jwsObject) {
        try {
            jwsObject.sign(jwsSigner);
        } catch (JOSEException e) {
            throw new Exception("Signing failed!", e);
        }
    }

    private JWSObject getJwsObject(Map<String, Object> sbd) {
        Payload payload = new Payload(new JSONObject(sbd));
        return new JWSObject(jwsHeader, payload);
    }

    private static class Exception extends RuntimeException {
        public Exception(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
