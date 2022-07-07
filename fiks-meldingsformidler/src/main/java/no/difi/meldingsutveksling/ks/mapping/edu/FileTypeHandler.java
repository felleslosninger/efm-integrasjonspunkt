package no.difi.meldingsutveksling.ks.mapping.edu;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.UnaryOperator;

/**
 * Used to map domain FileTypeHandler to
 */
@RequiredArgsConstructor
public class FileTypeHandler implements Handler<Dokument.Builder> {
    private final DokumentType domainDocument;
    private final UnaryOperator<byte[]> transform;

    @Override
    public Dokument.Builder map(Dokument.Builder builder) {
        try {
            final byte[] base64 = transform.apply(domainDocument.getFil().getBase64());

            final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(base64), domainDocument.getVeMimeType()));
            builder.withData(dataHandler);
        } catch (IOException e) {
            throw new DokumenterHandlerException("Unable to map NextMove documents to KS SvarUt Dokument documents", e);
        }
        return builder;
    }
}
