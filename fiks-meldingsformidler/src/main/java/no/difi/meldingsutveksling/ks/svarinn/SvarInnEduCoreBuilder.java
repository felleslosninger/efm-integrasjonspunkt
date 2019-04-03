package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.ServiceIdentifier;
import no.difi.meldingsutveksling.core.EDUCore;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
@Getter
public class SvarInnEduCoreBuilder {

    private static final PayloadConverter<MeldingType> payloadConverter = new PayloadConverterImpl<>(MeldingType.class,
            "http://www.arkivverket.no/Noark4-1-WS-WD/types", "Melding");
    private static final ObjectFactory objectFactory = new ObjectFactory();

    private final Forsendelse forsendelse;
    @Setter private String fallbackSenderOrgNr;

    private final List<DokumentType> dokumentTypeList = new ArrayList<>();

    public SvarInnEduCoreBuilder streamedFile(SvarInnStreamedFile streamedFile) {
        final DokumentType dokumentType = objectFactory.createDokumentType();

        String mimeType = streamedFile.getMimeType();
        dokumentType.setVeMimeType(mimeType);
        dokumentType.setVeDokformat(MimeTypeExtensionMapper.getExtension(mimeType));

        dokumentType.setDbTittel(streamedFile.getFileName());
        dokumentType.setVeFilnavn(streamedFile.getFileName());
        // Value does not exist in FIKS, must be hard coded
        dokumentType.setVeVariant("P");
        final FilType fil = objectFactory.createFilType();
        fil.setBase64(getContent(streamedFile));
        dokumentType.setFil(fil);
        dokumentTypeList.add(dokumentType);
        return this;
    }

    private byte[] getContent(SvarInnStreamedFile streamedFile) {
        try {
            return IOUtils.toByteArray(streamedFile.getInputStream());
        } catch (IOException e) {
            throw new SvarInnForsendelseException(
                    String.format("Couldn't get content of file %s", streamedFile.getFileName()), e);
        }
    }

    public EDUCore build() {
        final EDUCore eduCore = new EDUCore();
        // this is done because of a bug in when sending messages via NoarkClient:
        // The payload util doesn't correctly handle payloads that are MeldingType.
        // And I am afraid of fixing that bug might lead to trouble in the archive system.
        eduCore.setPayload(getPayload());
        eduCore.setId(forsendelse.getId());
        eduCore.setSender(createSender());
        eduCore.setReceiver(createReceiver());
        eduCore.setServiceIdentifier(ServiceIdentifier.DPF);
        return eduCore;
    }

    private String getPayload() {
        final MeldingType meldingType = objectFactory.createMeldingType();
        JournpostType journpostType = createJournpostType();
        journpostType.getDokument().addAll(dokumentTypeList);
        NoarksakType noarkSak = createNoarkSak();
        meldingType.setNoarksak(noarkSak);
        meldingType.setJournpost(journpostType);
        return payloadConverter.marshallToString(meldingType);
    }

    private Sender createSender() {
        if (isNullOrEmpty(forsendelse.getSvarSendesTil().getOrgnr()) && !isNullOrEmpty(fallbackSenderOrgNr)) {
            return Sender.of(fallbackSenderOrgNr, forsendelse.getSvarSendesTil().getNavn(), forsendelse.getId());
        }
        return Sender.of(forsendelse.getSvarSendesTil().getOrgnr(), forsendelse.getSvarSendesTil().getNavn(), forsendelse.getId());
    }

    private Receiver createReceiver() {
        return Receiver.of(forsendelse.getMottaker().getOrgnr(), forsendelse.getMottaker().getNavn(), forsendelse.getSvarPaForsendelse());
    }

    private NoarksakType createNoarkSak() {
        final NoarksakType noarksakType = objectFactory.createNoarksakType();
        final Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        noarksakType.setSaSeknr(String.valueOf(metadata.getSakssekvensnummer()));
        noarksakType.setSaSaar(String.valueOf(metadata.getSaksaar()));
        noarksakType.setSaTittel(getForsendelseTittel());
        return noarksakType;
    }

    private JournpostType createJournpostType() {
        final JournpostType journpostType = objectFactory.createJournpostType();
        final Forsendelse.MetadataFraAvleverendeSystem metadata = forsendelse.getMetadataFraAvleverendeSystem();
        journpostType.setJpDokdato(metadata.getDokumentetsDato());
        journpostType.setJpNdoktype(metadata.getJournalposttype());
        journpostType.setJpStatus(metadata.getJournalstatus());
        journpostType.setJpJaar(metadata.getJournalaar());
        journpostType.setJpSeknr(metadata.getJournalsekvensnummer());
        journpostType.setJpJpostnr(metadata.getJournalpostnummer());
        journpostType.setJpOffinnhold(getForsendelseTittel());
        journpostType.setJpInnhold(getForsendelseTittel());
        journpostType.setJpJdato(metadata.getJournaldato());
        journpostType.getAvsmot().add(createSaksbehandlerAvsender(metadata));
        journpostType.getAvsmot().add(createAvsender());
        return journpostType;
    }

    private String getForsendelseTittel() {
        if (!isNullOrEmpty(forsendelse.getMetadataFraAvleverendeSystem().getTittel())) {
            return forsendelse.getMetadataFraAvleverendeSystem().getTittel();
        }
        if (!isNullOrEmpty(forsendelse.getTittel())) {
            return forsendelse.getTittel();
        }
        return "Dokumentet mangler tittel";
    }

    private AvsmotType createAvsender() {
        Forsendelse.SvarSendesTil sst = forsendelse.getSvarSendesTil();
        AvsmotType avsender = new AvsmotType();
        avsender.setAmIhtype("1");
        avsender.setAmAdresse(sst.getAdresse1());
        avsender.setAmPostnr(sst.getPostnr());
        avsender.setAmPoststed(sst.getPoststed());
        avsender.setAmNavn(sst.getNavn());
        avsender.setAmUtland(sst.getLand());
        avsender.setAmOrgnr(sst.getOrgnr());
        return avsender;
    }

    private AvsmotType createSaksbehandlerAvsender(Forsendelse.MetadataFraAvleverendeSystem metadata) {
        AvsmotType avsender = new AvsmotType();
        avsender.setAmIhtype("0");
        avsender.setAmNavn(metadata.getSaksBehandler());
        return avsender;
    }
}
