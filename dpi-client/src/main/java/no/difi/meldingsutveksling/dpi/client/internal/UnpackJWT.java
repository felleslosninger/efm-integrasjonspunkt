package no.difi.meldingsutveksling.dpi.client.internal;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.util.X509CertChainUtils;
import lombok.RequiredArgsConstructor;
import no.difi.certvalidator.BusinessCertificateValidator;
import no.difi.meldingsutveksling.dpi.client.Blame;
import no.difi.meldingsutveksling.dpi.client.DpiException;
import org.springframework.util.Assert;

import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;

@RequiredArgsConstructor
public class UnpackJWT {

    private final BusinessCertificateValidator businessCertificateValidator;

    public Payload getPayload(String jwt) {
        JWSObject jwsObject = getJwsObject(jwt);
        verify(jwsObject);
        return jwsObject.getPayload();
    }

    private void verify(JWSObject jwsObject) {
        try {
            Assert.state(jwsObject.verify(getVerifier(jwsObject)), "Verifying JWT failed!");
        } catch (IllegalStateException | JOSEException e) {
            throw new IllegalStateException("Verifying JWT failed!", e);
        }
    }

    private RSASSAVerifier getVerifier(JWSObject jwsObject) {
        return new RSASSAVerifier((RSAPublicKey) getValidatedSigningCertificate(jwsObject).getPublicKey());
    }

    private JWSObject getJwsObject(String jwt) {
        try {
            return JWSObject.parse(jwt);
        } catch (ParseException e) {
            throw new IllegalStateException("Parsing JWT failed!", e);
        }
    }

    private X509Certificate getValidatedSigningCertificate(JWSObject jwsObject) {
        X509Certificate certificate = getSigningCertificate(jwsObject);
        businessCertificateValidator.validate(certificate);
        return certificate;
    }

    private X509Certificate getSigningCertificate(JWSObject jwsObject) {
        try {
            return X509CertChainUtils.parse(jwsObject.getHeader().getX509CertChain())
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new DpiException("Could not find signing certificate!", Blame.SERVER));
        } catch (ParseException e) {
            throw new IllegalStateException("Could not parse signing certificate!", e);
        }
    }

}
