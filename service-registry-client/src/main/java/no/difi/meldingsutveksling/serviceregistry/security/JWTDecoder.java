package no.difi.meldingsutveksling.serviceregistry.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.proc.BadJWSException;
import no.difi.move.common.oauth.KeystoreHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

@Component
public class JWTDecoder {

    @Qualifier("signingKeystoreHelper")
    private KeystoreHelper keystoreHelper;

    public String getPayload(String serialized) throws BadJWSException {

        JWSObject jwsObject;
        try {
            jwsObject = JWSObject.parse(serialized);
        } catch (ParseException e) {
            throw new BadJWSException("Could not parse signed string", e);
        }

        byte[] decode = jwsObject.getHeader().getX509CertChain().get(0).decode();
        CertificateFactory certificateFactory = null;
        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new BadJWSException("Could not get certificate factory for type \"X.509\"", e);
        }

        Certificate certificate;
        try {
            certificate = certificateFactory.generateCertificate(new ByteArrayInputStream(decode));
        } catch (CertificateException e) {
            throw new BadJWSException("Could not generate certificate object from JWS", e);
        }

        JWSVerifier jwsVerifier = new RSASSAVerifier((RSAPublicKey) certificate.getPublicKey());
        try {
            if (!jwsObject.verify(jwsVerifier)) {
                throw new BadJWSException("Signature did not successfully verify");
            }
        } catch (JOSEException e) {
            throw new BadJWSException("Could not verify JWS", e);
        }

        return jwsObject.getPayload().toString();
    }
}
