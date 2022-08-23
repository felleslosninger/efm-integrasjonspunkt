package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

/**
 * Used to map a single EDU DokumentType to Forsendelse
 */
public class DokumentTypeHandler implements Handler<Dokument.Builder> {
    private final DokumentType domainDocument;
    private final FileTypeHandler fileTypeHandler;

    public DokumentTypeHandler(DokumentType domainDocument, FileTypeHandler fileTypeHandler) {
        this.domainDocument = domainDocument;
        this.fileTypeHandler = fileTypeHandler;
    }

    @Override
    public Dokument.Builder map(Dokument.Builder builder) {
        fileTypeHandler.map(builder);
        builder.withMimetype(domainDocument.getVeMimeType());
        builder.withFilnavn(domainDocument.getVeFilnavn());
        return builder;
    }
}
