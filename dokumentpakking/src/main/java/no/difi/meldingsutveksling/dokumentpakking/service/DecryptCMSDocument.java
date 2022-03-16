package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.difi.move.common.cert.KeystoreHelper;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.util.Objects;

@RequiredArgsConstructor
public class DecryptCMSDocument {

    public Resource decrypt(Input input) {
        return new CmsResource(input);
    }

    @Value
    @Builder
    public static class Input {
        @NonNull Resource resource;
        @NonNull KeystoreHelper keystoreHelper;
        String alias;
        @Builder.Default
        String tempFilePrefix = "";
    }

    @RequiredArgsConstructor
    private static class CmsResource extends AbstractResource {
        private final Input input;

        @NonNull
        @Override
        public InputStream getInputStream() throws IOException {
            RecipientInformation recipientInformation = getRecipientInformation(input.getResource());
            CMSTypedStream contentStream = getCmsTypedStream(recipientInformation);
            return contentStream.getContentStream();
        }

        private CMSTypedStream getCmsTypedStream(RecipientInformation recipientInformation) throws IOException {
            try {
                return recipientInformation.getContentStream(getRecipient(input));
            } catch (CMSException e) {
                throw new IOException("Could not get CMS content stream", e);
            }
        }

        private RecipientInformation getRecipientInformation(Resource resource) {
            return getParser(resource).getRecipientInfos().getRecipients()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No recipients in CMS document."));
        }

        private CMSEnvelopedDataParser getParser(Resource resource) {
            try {
                return new CMSEnvelopedDataParser(resource.getInputStream());
            } catch (CMSException | IOException e) {
                throw new IllegalStateException("Couldn't create CMS parser", e);
            }
        }

        private JceKeyTransRecipient getRecipient(Input input) {
            KeystoreHelper keystoreHelper = input.getKeystoreHelper();
            JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(getRecipientKey(input));
            if (keystoreHelper.shouldLockProvider()) {
                recipient.setProvider(keystoreHelper.getKeyStore().getProvider());
            }
            return recipient;
        }

        private PrivateKey getRecipientKey(Input input) {
            KeystoreHelper keystoreHelper = input.getKeystoreHelper();
            return input.getAlias() != null ? keystoreHelper.loadPrivateKey(input.getAlias()) : keystoreHelper.loadPrivateKey();
        }

        @NonNull
        @Override
        public String getDescription() {
            return "CMS resource";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CmsResource)) return false;
            if (!super.equals(o)) return false;
            CmsResource that = (CmsResource) o;
            return input.equals(that.input);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), input);
        }
    }
}
