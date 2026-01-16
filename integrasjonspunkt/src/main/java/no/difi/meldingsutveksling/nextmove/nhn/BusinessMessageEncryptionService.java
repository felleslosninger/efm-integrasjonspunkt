package no.difi.meldingsutveksling.nextmove.nhn;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.json.ObjectMapperHolder;
import no.difi.meldingsutveksling.nextmove.BusinessMessage;
import no.difi.meldingsutveksling.nextmove.EncryptedBusinessMessage;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Kryptering;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class BusinessMessageEncryptionService {
    private final Kryptering kryptering;

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
            return new EncryptedBusinessMessage(new String(Base64.encode(encryptedMessage)),new String(Base64.encode(certificate.getEncoded())));
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
}
