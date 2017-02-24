package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.config.SvarUtConfig;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

import java.security.cert.X509Certificate;

public class FileTypeHandlerFactory {
    private final SvarUtConfig svarUtConfig;
    private X509Certificate certificate;

    public FileTypeHandlerFactory(SvarUtConfig svarUtConfig, X509Certificate certificate) {
        this.svarUtConfig = svarUtConfig;
        this.certificate = certificate;
    }

    FileTypeHandler createFileTypeHandler(DokumentType dokumentType) {
        final FileTypeHandler fileTypeHandler = new FileTypeHandler(dokumentType);
        if (svarUtConfig.isKryptert()) {
            fileTypeHandler.encryptMappedDataWith(certificate);
        }
        return fileTypeHandler;
    }
}
