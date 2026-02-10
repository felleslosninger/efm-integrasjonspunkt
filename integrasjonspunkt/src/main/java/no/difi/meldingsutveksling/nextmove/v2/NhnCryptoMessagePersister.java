package no.difi.meldingsutveksling.nextmove.v2;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.api.MessagePersister;
import no.difi.meldingsutveksling.nhn.adapter.crypto.EncryptionException;
import no.difi.meldingsutveksling.nhn.adapter.crypto.Kryptering;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.cert.X509Certificate;

@Component
@RequiredArgsConstructor
public class NhnCryptoMessagePersister {

    private final MessagePersister messagePersister;
    private final Kryptering kryptering = new Kryptering();


    public void write(String messageId, String filename, Resource resource, X509Certificate certificate) throws EncryptionException {
      try{
          var enryptedFile =  kryptering.krypter(resource.getContentAsByteArray(), certificate);
          messagePersister.write(messageId,filename, new ByteArrayResource(enryptedFile));
      } catch (IOException e) {
          throw new EncryptionException("Not able to encrypt message due to IO error",e);
      }

    }

    public Resource read(String messageId, String filename) throws IOException {
        return messagePersister.read(messageId,filename);
    }


    public void delete(String messageId) throws IOException {
        throw  new UnsupportedOperationException();
    }
}
