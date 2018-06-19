package no.difi.meldingsutveksling.ks.mapping;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
public class ForsendelseMapper {
    private IntegrasjonspunktProperties properties;
    private ServiceRegistryLookup serviceRegistry;

    public ForsendelseMapper(IntegrasjonspunktProperties properties, ServiceRegistryLookup serviceRegistry) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
    }

    public SendForsendelseMedId mapFrom(EDUCore eduCore, X509Certificate certificate) {
        final Forsendelse.Builder<Void> forsendelse = Forsendelse.builder();
        forsendelse.withEksternref(eduCore.getId());
        forsendelse.withKunDigitalLevering(false);
        forsendelse.withSvarPaForsendelse(eduCore.getReceiver().getRef());

        final MeldingType meldingType = EDUCoreConverter.payloadAsMeldingType(eduCore.getPayload());
        forsendelse.withTittel(meldingType.getJournpost().getJpOffinnhold());

        final FileTypeHandlerFactory fileTypeHandlerFactory = new FileTypeHandlerFactory(properties.getFiks(), certificate);
        forsendelse.withDokumenter(mapFrom(meldingType.getJournpost().getDokument(), fileTypeHandlerFactory));

        forsendelse.withKonteringskode(properties.getFiks().getUt().getKonverteringsKode());
        forsendelse.withKryptert(properties.getFiks().isKryptert());
        forsendelse.withAvgivendeSystem(properties.getNoarkSystem().getType());

        forsendelse.withPrintkonfigurasjon(Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build());

        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(eduCore.getReceiver().getIdentifier());
        forsendelse.withMottaker(mottakerFrom(receiverInfo));

        Optional<AvsmotType> avsender = getAvsender(meldingType);
        if (avsender.isPresent()) {
            forsendelse.withSvarSendesTil(mottakerFrom(avsender.get(), receiverInfo.getIdentifier()));
        } else {
            final InfoRecord senderInfo = serviceRegistry.getInfoRecord(eduCore.getSender().getIdentifier());
            forsendelse.withSvarSendesTil(mottakerFrom(senderInfo));
        }

        forsendelse.withMetadataFraAvleverendeSystem(metaDataFrom(meldingType));
        String senderRef;
        if (Strings.isNullOrEmpty(eduCore.getSender().getRef())) {
            log.warn("No envelope.sender.ref in message, using conversationId instead..");
            senderRef = eduCore.getId();
        } else {
            senderRef = eduCore.getSender().getRef();
        }

        return SendForsendelseMedId.builder()
                .withForsendelse(forsendelse.build())
                .withForsendelsesid(senderRef)
                .build();
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

        Optional<AvsmotType> avsender = getSaksbehandler(meldingType);
        avsender.map(a -> a.getAmNavn()).ifPresent(metadata::withSaksbehandler);

        return metadata.build();
    }


    private Optional<AvsmotType> getSaksbehandler(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "0".equals(f.getAmIhtype())).findFirst();
    }

    private Optional<AvsmotType> getAvsender(MeldingType meldingType) {
        List<AvsmotType> avsmotlist = meldingType.getJournpost().getAvsmot();
        return avsmotlist.stream().filter(f -> "1".equals(f.getAmIhtype())).findFirst();
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

    private Adresse mottakerFrom(AvsmotType avsmotType, String orgnr) {
        Adresse.Builder<Void> mottaker = Adresse.builder();

        OrganisasjonDigitalAdresse orgAdr = OrganisasjonDigitalAdresse.builder()
                .withOrgnr(orgnr)
                .build();
        mottaker.withDigitalAdresse(orgAdr);

        PostAdresse postAdr = PostAdresse.builder()
                .withNavn(avsmotType.getAmNavn())
                .withAdresse1(avsmotType.getAmAdresse())
                .withPostnr(avsmotType.getAmPostnr())
                .withPoststed(avsmotType.getAmPoststed())
                .withLand(avsmotType.getAmUtland())
                .build();
        mottaker.withPostAdresse(postAdr);

        return mottaker.build();
    }

    private Adresse mottakerFrom(InfoRecord infoRecord) {
        Adresse.Builder<Void> mottaker = Adresse.builder();

        OrganisasjonDigitalAdresse orgAdr = OrganisasjonDigitalAdresse.builder()
                .withOrgnr(infoRecord.getIdentifier())
                .build();
        mottaker.withDigitalAdresse(orgAdr);

        PostAdresse.Builder<Void> postAdr = PostAdresse.builder()
                .withNavn(infoRecord.getOrganizationName());

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
        mottaker.withPostAdresse(postAdr.build());

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
