package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.config.FiksConfig;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.ks.*;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class ForsendelseMapper {
    private IntegrasjonspunktProperties properties;
    FiksConfig fiksConfig;
    private X509Certificate certificate;
    private FileTypeHandlerFactory fileTypeHandlerFactory;

    public ForsendelseMapper(IntegrasjonspunktProperties properties, FiksConfig fiksConfig, X509Certificate certificate) {
        this.properties = properties;
        this.fiksConfig = fiksConfig;
        this.certificate = certificate;
        this.fileTypeHandlerFactory = new FileTypeHandlerFactory(fiksConfig, certificate);
    }

    Forsendelse mapFrom(EDUCore eduCore) {
        final Forsendelse.Builder<Void> builder = Forsendelse.builder();
        builder.withEksternref(eduCore.getId());
        builder.withKunDigitalLevering(true);

        final MeldingType meldingType = eduCore.getPayloadAsMeldingType();
        builder.withTittel(meldingType.getNoarksak().getSaOfftittel());

        builder.withDokumenter(mapFrom(meldingType.getJournpost().getDokument()));

        builder.withKonteringskode(fiksConfig.getUt().getKonverteringsKode());
        builder.withKryptert(fiksConfig.isKryptert());
        builder.withAvgivendeSystem(properties.getNoarkSystem().getType());

        builder.withMottaker(mottakerFrom(eduCore.getReceiver()));

        builder.withMetadataFraAvleverendeSystem(metaDataFrom())


        return builder.build();
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(MeldingType meldingType) {

        NoarkMetadataFraAvleverendeSakssystem.Builder<Void> metadata = NoarkMetadataFraAvleverendeSakssystem.builder();
        metadata.withSakssekvensnummer(Integer.valueOf(meldingType.getNoarksak().getSaSeknr()));
        metadata.withSaksaar(Integer.valueOf(meldingType.getNoarksak().getSaSaar()));
        metadata.withJournalaar(Integer.valueOf(meldingType.getJournpost().getJpJaar()));
        metadata.withJournalsekvensnummer(Integer.valueOf(meldingType.getJournpost().getJpSeknr()));
        metadata.with


        return null;
    }

    private Mottaker mottakerFrom(Receiver receiver) {
        Organisasjon.Builder<Void> mottaker = Organisasjon.builder();
        mottaker.withOrgnr(receiver.getIdentifier());

        mottaker.withPostnr("0192");
        mottaker.withPoststed("Oslo");
        mottaker.withNavn("Navn");
        return mottaker.build();
    }

    List<Dokument> mapFrom(List<DokumentType> dokumentTypes) {
        List<Dokument> dokumenter = new ArrayList<>(dokumentTypes.size());
        for (DokumentType d : dokumentTypes) {
            FileTypeHandlerFactory fileTypeHandlerFactory = this.fileTypeHandlerFactory;
            final FileTypeHandler fileTypeHandler = fileTypeHandlerFactory.createFileTypeHandler(d);
            Dokument.Builder dokumentBuilder =  fileTypeHandler.map(Dokument.builder());

            dokumentBuilder.withFilnavn(d.getVeFilnavn());
            dokumentBuilder.withMimetype(d.getVeMimeType());

            dokumenter.add(dokumentBuilder.build());
        }

        return dokumenter;
    }
}
