package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

import java.security.cert.X509Certificate;

public class FileTypeHandlerFactory {
    private final FiksConfig fiksConfig;
    private X509Certificate certificate;

    public FileTypeHandlerFactory(FiksConfig fiksConfig, X509Certificate certificate) {
        this.fiksConfig = fiksConfig;
        this.certificate = certificate;
    }

    FileTypeHandler createFileTypeHandler(DokumentType dokumentType) {
        final FileTypeHandler fileTypeHandler = new FileTypeHandler(dokumentType);
        if (fiksConfig.isKryptert()) {
            fileTypeHandler.encryptMappedDataWith(certificate);
        }
        return fileTypeHandler;
    }
}
