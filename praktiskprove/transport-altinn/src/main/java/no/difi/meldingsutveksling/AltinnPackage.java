package no.difi.meldingsutveksling;

import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Manifest;
import no.difi.meldingsutveksling.altinn.mock.brokerbasic.Recipient;
import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocument;
import no.difi.meldingsutveksling.shipping.ManifestBuilder;
import no.difi.meldingsutveksling.shipping.RecipientBuilder;
import no.difi.meldingsutveksling.shipping.Request;

public class AltinnPackage {
    private final Manifest manifest;
    private final Recipient recipient;
    private final StandardBusinessDocument document;

    public AltinnPackage(Manifest manifest, Recipient recipient, StandardBusinessDocument document) {
        this.manifest = manifest;
        this.recipient = recipient;
        this.document = document;
    }

    public static AltinnPackage from(Request document) {
        ManifestBuilder manifest = new ManifestBuilder();
        manifest.withSender(document.getSender());
        manifest.withSenderReference(document.getSenderReference());


        RecipientBuilder recipient = new RecipientBuilder(document.getReceiver());
        return new AltinnPackage(manifest.build(), recipient.build(), document.getPayload());
    }

}
