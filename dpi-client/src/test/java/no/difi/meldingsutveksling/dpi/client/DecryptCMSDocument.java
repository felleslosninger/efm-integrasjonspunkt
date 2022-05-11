package no.difi.meldingsutveksling.dpi.client;

import lombok.RequiredArgsConstructor;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipient;

import java.io.IOException;
import java.io.InputStream;

@RequiredArgsConstructor
public class DecryptCMSDocument {

    private final JceKeyTransRecipient recipient;

    public InputStream decrypt(InputStream encrypted) {
        try {
            return getRecipientInformation(encrypted).getContentStream(recipient).getContentStream();
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't decrypt CMS", e);
        }
    }

    private RecipientInformation getRecipientInformation(InputStream encrypted) {
        return getParser(encrypted).getRecipientInfos().getRecipients()
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No recipients in CMS document."));
    }

    private CMSEnvelopedDataParser getParser(InputStream encrypted) {
        try {
            return new CMSEnvelopedDataParser(encrypted);
        } catch (CMSException | IOException e) {
            throw new IllegalStateException("Couldn't create CMS parser", e);
        }
    }

}
