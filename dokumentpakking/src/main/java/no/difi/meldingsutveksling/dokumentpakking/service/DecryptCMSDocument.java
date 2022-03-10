package no.difi.meldingsutveksling.dokumentpakking.service;

import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.move.common.cert.KeystoreHelper;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResource;
import no.difi.move.common.io.InMemoryWithTempFileFallbackResourceFactory;
import no.difi.move.common.io.OutputStreamResource;
import no.difi.move.common.io.WritableByteArrayResource;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class DecryptCMSDocument {

    private final Plumber plumber;
    private final InMemoryWithTempFileFallbackResourceFactory resourceFactory;

    public byte[] toByteArray(Input input) {
        WritableByteArrayResource output = new WritableByteArrayResource();
        decrypt(input, output);
        return output.toByteArray();
    }

    public InMemoryWithTempFileFallbackResource decrypt(Input input) {
        InMemoryWithTempFileFallbackResource output = resourceFactory.getResource(input.getTempFilePrefix(), "");
        decrypt(input, output);
        return output;
    }

    public InputStreamResource decrypt(Input input, Reject reject) {
        return new InputStreamResource(plumber.pipe("Decrypting CMS document", inlet -> {
            try {
                decrypt(input, new OutputStreamResource(inlet));
            } catch (Exception e) {
                reject.reject(new IllegalStateException("Couldn't decrypt CMS", e));
            }
        }, reject).outlet());
    }

    private void decrypt(Input input, WritableResource writableResource) {
        try (OutputStream outputStream = writableResource.getOutputStream()) {
            StreamUtils.copy(getRecipientInformation(input.getResource()).getContentStream(getRecipient(input)).getContentStream(), outputStream);
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't decrypt CMS", e);
        }
    }

    private RecipientInformation getRecipientInformation(Resource resource) {
        return getParser(resource).getRecipientInfos().getRecipients()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No recipients in CMS document."));
    }

    private CMSEnvelopedDataParser getParser(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new CMSEnvelopedDataParser(inputStream);
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't create CMS parser", e);
        }
    }

    private JceKeyTransRecipient getRecipient(Input input) {
        KeystoreHelper keystoreHelper = input.getKeystoreHelper();
        JceKeyTransRecipient recipient = new JceKeyTransEnvelopedRecipient(keystoreHelper.loadPrivateKey());
        recipient.setProvider(keystoreHelper.shouldLockProvider() ? keystoreHelper.getKeyStore().getProvider() : null);
        return recipient;
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
}
