package no.difi.meldingsutveksling.ks.mapping;

import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandler;
import no.difi.meldingsutveksling.ks.mapping.edu.FileTypeHandlerFactory;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.noarkexchange.schema.core.AvsmotType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.DokumentType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

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

        final MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());
        builder.withTittel(meldingType.getJournpost().getJpOffinnhold());

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
        metadata.withJournaldato(journalDatoFrom(meldingType.getJournpost().getJpJdato()));
        metadata.withDokumentetsDato(journalDatoFrom(meldingType.getJournpost().getJpDokdato()));
        metadata.withTittel(meldingType.getJournpost().getJpOffinnhold());

        Optional<AvsmotType> avsender = getAvsender(meldingType);
        avsender.map(a -> a.getAmNavn()).ifPresent(metadata::withSaksbehandler);

        return metadata.build();
    }


    private Optional<AvsmotType> getAvsender(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "0".equals(f.getAmIhtype())).findFirst();
    }

    private XMLGregorianCalendar journalDatoFrom(String jpDato) {
        LocalDateTime localDateTime = LocalDateTime.of(LocalDate.parse(jpDato), LocalTime.of(0, 0));

        GregorianCalendar gcal = GregorianCalendar.from(localDateTime.atZone(ZoneId.systemDefault()));
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
        } catch (DatatypeConfigurationException e) {
            throw new ForsendelseMappingException("Unable to map date", e);
        }
    }

    private Adresse mottakerFrom(InfoRecord infoRecord) {
        OrganisasjonDigitalAdresse.Builder<Void> orgAdr = OrganisasjonDigitalAdresse.builder();
        orgAdr.withOrgnr(infoRecord.getIdentifier());

        Adresse.Builder<Void> mottaker = Adresse.builder();
        mottaker.withDigitalAdresse(orgAdr.build());

        PostAdresse.Builder<Void> postAdr = PostAdresse.builder();
        if (infoRecord.getPostadresse() != null) {
            postAdr.withAdresse1(infoRecord.getPostadresse().getAdresse());
            postAdr.withPostnr(infoRecord.getPostadresse().getPostnummer());
            postAdr.withPoststed(infoRecord.getPostadresse().getPoststed());
            postAdr.withLand(infoRecord.getPostadresse().getLand());
        } else {
            postAdr.withPostnr("0192");
            postAdr.withPoststed("Oslo");
            postAdr.withLand("Norge");
        }
        
        return mottaker.build();
    }

    private List<Dokument> mapFrom(List<DokumentType> dokumentTypes, FileTypeHandlerFactory fileTypeHandlerFactory) {
        List<Dokument> dokumenter = new ArrayList<>(dokumentTypes.size());
        for (DokumentType d : dokumentTypes) {
            final FileTypeHandler fileTypeHandler = fileTypeHandlerFactory.createFileTypeHandler(d);
            Dokument.Builder dokumentBuilder = fileTypeHandler.map(Dokument.builder());

            dokumentBuilder.withFilnavn(d.getVeFilnavn());
            dokumentBuilder.withMimetype(d.getVeMimeType());

            dokumenter.add(dokumentBuilder.build());
        }

        return dokumenter;
    }
}
