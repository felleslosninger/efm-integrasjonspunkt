package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.config.SvarUtConfig;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;

public class FileTypeHandlerFactory {
    private final SvarUtConfig svarUtConfig;

    public FileTypeHandlerFactory(SvarUtConfig svarUtConfig) {
        this.svarUtConfig = svarUtConfig;
    }

    FileTypeHandler createFileTypeHandler(DokumentType dokumentType) {
        final FileTypeHandler fileTypeHandler = new FileTypeHandler(dokumentType);
        if (svarUtConfig.isKryptert()) {
            fileTypeHandler.encryptMappedDataWith(svarUtConfig.getCertificate());
        }
        return fileTypeHandler;
    }
}
