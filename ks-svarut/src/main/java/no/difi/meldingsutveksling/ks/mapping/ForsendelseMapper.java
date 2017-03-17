package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.ks.*;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class ForsendelseMapper {
    private IntegrasjonspunktProperties properties;
    private ServiceRegistryLookup serviceRegistry;

    public ForsendelseMapper(IntegrasjonspunktProperties properties, ServiceRegistryLookup serviceRegistry) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
    }

    public Forsendelse mapFrom(EDUCore eduCore, X509Certificate certificate) {
        final Forsendelse.Builder<Void> builder = Forsendelse.builder();
        builder.withEksternref(eduCore.getId());
        builder.withKunDigitalLevering(true);

        final MeldingType meldingType = eduCore.getPayloadAsMeldingType();
        builder.withTittel(meldingType.getNoarksak().getSaOfftittel());

        final FileTypeHandlerFactory fileTypeHandlerFactory = new FileTypeHandlerFactory(properties.getFiks(), certificate);
        builder.withDokumenter(mapFrom(meldingType.getJournpost().getDokument(), fileTypeHandlerFactory));

        builder.withKonteringskode(properties.getFiks().getUt().getKonverteringsKode());
        builder.withKryptert(properties.getFiks().isKryptert());
        builder.withAvgivendeSystem(properties.getNoarkSystem().getType());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(eduCore.getReceiver().getIdentifier());
        builder.withMottaker(mottakerFrom(receiverInfo));

        final InfoRecord senderInfo = serviceRegistry.getInfoRecord(eduCore.getSender().getIdentifier());
        builder.withSvarSendesTil(mottakerFrom(senderInfo));

        builder.withMetadataFraAvleverendeSystem(metaDataFrom(meldingType));

        return builder.build();
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(MeldingType meldingType) {

        NoarkMetadataFraAvleverendeSakssystem.Builder<Void> metadata = NoarkMetadataFraAvleverendeSakssystem.builder();
        metadata.withSakssekvensnummer(Integer.valueOf(meldingType.getNoarksak().getSaSeknr()));
        metadata.withSaksaar(Integer.valueOf(meldingType.getNoarksak().getSaSaar()));
        metadata.withJournalaar(Integer.valueOf(meldingType.getJournpost().getJpJaar()));
        metadata.withJournalsekvensnummer(Integer.valueOf(meldingType.getJournpost().getJpSeknr()));
        metadata.withJournalpostnummer(Integer.valueOf(meldingType.getJournpost().getJpJpostnr()));
        metadata.withJournalposttype(meldingType.getJournpost().getJpNdoktype());
        metadata.withJournalstatus(meldingType.getJournpost().getJpStatus());
        metadata.withJournaldato(journalDatoFrom(meldingType.getJournpost().getJpDokdato()));

        return metadata.build();
    }

    private XMLGregorianCalendar journalDatoFrom(String jpDokdato) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(jpDokdato);
        } catch (DatatypeConfigurationException e) {
            throw new ForsendelseMappingException("Unable to map journalDato", e);
        }
    }

    private Mottaker mottakerFrom(InfoRecord infoRecord) {
        Organisasjon.Builder<Void> mottaker = Organisasjon.builder();
        mottaker.withOrgnr(infoRecord.getIdentifier());

        mottaker.withPostnr("0192");
        mottaker.withPoststed("Oslo");
        mottaker.withNavn(infoRecord.getOrganizationName());
        return mottaker.build();
    }

    private List<Dokument> mapFrom(List<DokumentType> dokumentTypes, FileTypeHandlerFactory fileTypeHandlerFactory) {
        List<Dokument> dokumenter = new ArrayList<>(dokumentTypes.size());
        for (DokumentType d : dokumentTypes) {
            final FileTypeHandler fileTypeHandler = fileTypeHandlerFactory.createFileTypeHandler(d);
            Dokument.Builder dokumentBuilder =  fileTypeHandler.map(Dokument.builder());

            dokumentBuilder.withFilnavn(d.getVeFilnavn());
            dokumentBuilder.withMimetype(d.getVeMimeType());

            dokumenter.add(dokumentBuilder.build());
        }

        return dokumenter;
    }
}
