package no.difi.meldingsutveksling.ks.mapping.edu;

import no.difi.meldingsutveksling.ks.mapping.Handler;
import no.difi.meldingsutveksling.ks.svarut.Dokument;
import no.difi.meldingsutveksling.ks.svarut.Forsendelse;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.NoarksakType;

import java.util.List;
import java.util.Objects;

/**
 * Used to map EDU MeldingType to Forsendelse
 */
public class MeldingTypeHandler implements Handler<Forsendelse.Builder> {
    private final MeldingType meldingType;
    private final FileTypeHandlerFactory fileTypeHandlerFactory;

    public MeldingTypeHandler(MeldingType meldingType, FileTypeHandlerFactory factory) {
        this.meldingType = meldingType;
        this.fileTypeHandlerFactory = factory;
    }

    @Override
    public Forsendelse.Builder map(Forsendelse.Builder builder) {
        final List<DokumentType> dokumentTypes = meldingType.getJournpost().getDokument();
        final NoarksakType noarksak = meldingType.getNoarksak();
        builder.withTittel(noarksak.getSaOfftittel());
        for (DokumentType d : dokumentTypes) {
            final FileTypeHandler fileTypeHandler = this.fileTypeHandlerFactory.createFileTypeHandler(d);
            final DokumentTypeHandler dokumentHandler = new DokumentTypeHandler(d, fileTypeHandler);
            Dokument.Builder dokumentBuilder = dokumentHandler.map(Dokument.builder());

            builder.addDokumenter(dokumentBuilder.build());
        }
        return builder;
    }

    @Override
    @SuppressWarnings({"squid:S00122", "squid:S1067"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MeldingTypeHandler)) return false;
        MeldingTypeHandler that = (MeldingTypeHandler) o;
        return Objects.equals(meldingType, that.meldingType) &&
                Objects.equals(fileTypeHandlerFactory, that.fileTypeHandlerFactory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meldingType, fileTypeHandlerFactory);
    }
}
