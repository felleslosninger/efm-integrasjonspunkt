package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.ks.Dokument;
import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.function.Function;

/**
 * Used to map domain FileTypeHandler to
 */
public class FileTypeHandler implements Handler<Dokument.Builder> {
    private final DokumentType domainDocument;
    private X509Certificate certificate;
    private final Function<byte[], byte[]> noop = b -> b;
    private final Function<byte[], byte[]> encrypt = b -> new CmsUtil().createCMS(b, certificate);
    private Function<byte[], byte[]> transform = noop;

    public FileTypeHandler(DokumentType domainDocument) {
        this.domainDocument = domainDocument;
    }

    /**
     * Use this method to make FileTypeHandler encrypt the binary contents of the file.
     * @param certificate should be the public certificate of KS SvarUt
     * @return handler to map filetypes to forsendelse dokument
     */
    FileTypeHandler encryptMappedDataWith(X509Certificate certificate) {
        this.certificate = certificate;
        transform = encrypt;
        return this;
    }

    @Override
    public Dokument.Builder map(Dokument.Builder builder) {
        try {
            final byte[] base64 = transform.apply(domainDocument.getFil().getBase64());

            final DataHandler dataHandler = new DataHandler(new ByteArrayDataSource(new ByteArrayInputStream(base64), domainDocument.getVeMimeType()));
            builder.withData(dataHandler);
        } catch (IOException e) {
            throw new DokumenterHandlerException("Unable to map EDUCore documents to KS SvarUt Dokument documents", e);
        }
        return builder;
    }
}
