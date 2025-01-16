package no.difi.meldingsutveksling.api;

import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.nextmove.NextMoveMessage;
import no.difi.move.common.io.pipe.Reject;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.security.cert.X509Certificate;
import java.util.stream.Stream;

public interface AsicHandler {

    void createCmsEncryptedAsice(NextMoveMessage msg, WritableResource writableResource);

    Resource createCmsEncryptedAsice(NextMoveMessage msg, Reject reject);

    Resource createCmsEncryptedAsice(
        NextMoveMessage msg,
        Document mainDocument,
        Stream<Document> attachments,
        X509Certificate certificate,
        Reject reject
    );

}