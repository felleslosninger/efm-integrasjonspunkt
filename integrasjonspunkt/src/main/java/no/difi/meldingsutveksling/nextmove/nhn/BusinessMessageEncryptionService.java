package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.BusinessMessage;
import no.difi.meldingsutveksling.domain.EncryptedBusinessMessage;
import no.difi.meldingsutveksling.jpa.ObjectMapperHolder;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Dekryptering;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Kryptering;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class BusinessMessageEncryptionService {
    private final Kryptering kryptering;
    private final Dekryptering dekryptering;

    public EncryptedBusinessMessage encrypt(BusinessMessage businessMessage, String pemCertificate) throws EncryptionException {
        try {
            X509Certificate certificate = (X509Certificate) EncryptedBusinessMessage.cf.generateCertificate(new ByteArrayInputStream(pemCertificate.getBytes(StandardCharsets.UTF_8)));
            return this.encrypt(businessMessage, certificate);
        } catch (CertificateException e) {
            throw new EncryptionException("Not able to obtain encryption certificate",e);
        } catch (EncryptionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new EncryptionException("Not able to encrypt message",e);
        }
    }

    public EncryptedBusinessMessage encrypt(BusinessMessage businessMessage, X509Certificate certificate) throws EncryptionException {
        try{
            var businessMessageJson = ObjectMapperHolder.get().writeValueAsString(businessMessage);
            var encryptedMessage = kryptering.krypter(businessMessageJson.getBytes(StandardCharsets.UTF_8),certificate);
            var certificateString = new String(java.util.Base64.getEncoder().encode(certificate.getEncoded()));
            X509Certificate x509Certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(java.util.Base64.getDecoder().decode(certificateString.getBytes(StandardCharsets.UTF_8))));
            System.out.println(x509Certificate.getSubjectDN().getName());
            return new EncryptedBusinessMessage(certificateString,new String(Base64.encode(encryptedMessage)));
        } catch (JsonProcessingException e) {
            throw new EncryptionException("Not able to parse business message during encyrption",e);
        } catch (CertificateException e) {
            throw new EncryptionException("Not able to obtain encryption certificate",e);
        } catch (EncryptionException e) {
            throw e;
        }
        catch (Exception e) {
            throw new EncryptionException("Not able to encrypt message",e);
        }
    }

    public byte[] decrypt(EncryptedBusinessMessage encryptedBusinessMessage) throws EncryptionException {
        return dekryptering.dekrypter(encryptedBusinessMessage.getMessage().getBytes());
    }

}
