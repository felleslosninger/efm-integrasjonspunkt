package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.ks.Dokument;
import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Used to map a single EDU DokumentType to Forsendelse
 */
public class DokumentTypeHandler implements Handler<Dokument.Builder> {
    private DokumentType domainDocument;

    public DokumentTypeHandler(DokumentType domainDocument) {
        this.domainDocument = domainDocument;
    }

    @Override
    public Dokument.Builder map(Dokument.Builder builder) {
        try {
            final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(domainDocument.getFil().getBase64()), domainDocument.getVeMimeType()));
            builder.withData(dataHandler);
            builder.withMimetype(domainDocument.getVeMimeType());
            builder.withFilnavn(domainDocument.getVeFilnavn());
        } catch (IOException e) {
            throw new DokumenterHandlerException("Unable to map EDUCore documents to KS SvarUt Dokument documents", e);
        }
        return builder;
    }
}
