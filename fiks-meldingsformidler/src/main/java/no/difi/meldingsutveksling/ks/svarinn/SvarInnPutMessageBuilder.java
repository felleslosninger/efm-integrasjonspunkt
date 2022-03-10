package no.difi.meldingsutveksling.ks.svarinn;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import no.difi.meldingsutveksling.MimeTypeExtensionMapper;
import no.difi.meldingsutveksling.bestedu.PutMessageRequestFactory;
import no.difi.meldingsutveksling.core.Receiver;
import no.difi.meldingsutveksling.core.Sender;
import no.difi.meldingsutveksling.dokumentpakking.domain.Document;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverter;
import no.difi.meldingsutveksling.noarkexchange.receive.PayloadConverterImpl;
import no.difi.meldingsutveksling.noarkexchange.schema.PutMessageRequestType;
import no.difi.meldingsutveksling.noarkexchange.schema.core.*;
import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiredArgsConstructor
@Getter
public class SvarInnPutMessageBuilder {

    private static final PayloadConverter<MeldingType> payloadConverter = new PayloadConverterImpl<>(MeldingType.class,
            "http://www.arkivverket.no/Noark4-1-WS-WD/types", "Melding");
    private static final ObjectFactory objectFactory = new ObjectFactory();

    private final Forsendelse forsendelse;
    private final Clock clock;
    private final PutMessageRequestFactory putMessageRequestFactory;
    @Setter private String fallbackSenderOrgNr;

    private final List<DokumentType> dokumentTypeList = new ArrayList<>();

    public SvarInnPutMessageBuilder streamedFile(Document document) {
        final DokumentType dokumentType = objectFactory.createDokumentType();

        String mimeType = document.getMimeType().toString();
        dokumentType.setVeMimeType(mimeType);
        dokumentType.setVeDokformat(MimeTypeExtensionMapper.getExtension(mimeType));

        dokumentType.setDbTittel(document.getFilename());
        dokumentType.setVeFilnavn(document.getFilename());
        // Value does not exist in FIKS, must be hard coded
        dokumentType.setVeVariant("P");
        final FilType fil = objectFactory.createFilType();
        fil.setBase64(getContent(document));
        dokumentType.setFil(fil);
        dokumentTypeList.add(dokumentType);
        return this;
    }

    private byte[] getContent(Document document) {
        try (InputStream inputStream = document.getResource().getInputStream()) {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            throw new SvarInnForsendelseException(
                    String.format("Couldn't get content of file %s", document.getFilename()), e);
        }
    }

    public PutMessageRequestType build() {
        Sender sender = createSender();
        Receiver receiver = createReceiver();

        return putMessageRequestFactory.create(forsendelse.getId(),
                sender,
                receiver,
                getPayload());
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
        if (!StringUtils.hasText(forsendelse.getSvarSendesTil().getOrgnr()) && StringUtils.hasText(fallbackSenderOrgNr)) {
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
        if (!isNullOrEmpty(metadata.getDokumentetsDato())) {
            journpostType.setJpDokdato(Instant.ofEpochMilli(Long.parseLong(metadata.getDokumentetsDato())).atZone(clock.getZone()).toLocalDate().toString());
        }
        journpostType.setJpNdoktype(metadata.getJournalposttype());
        journpostType.setJpStatus(metadata.getJournalstatus());
        journpostType.setJpJaar(metadata.getJournalaar());
        journpostType.setJpSeknr(metadata.getJournalsekvensnummer());
        journpostType.setJpJpostnr(metadata.getJournalpostnummer());
        journpostType.setJpOffinnhold(getForsendelseTittel());
        journpostType.setJpInnhold(getForsendelseTittel());
        if (!isNullOrEmpty(metadata.getJournaldato())) {
            journpostType.setJpJdato(Instant.ofEpochMilli(Long.parseLong(metadata.getJournaldato())).atZone(clock.getZone()).toLocalDate().toString());
        }
        journpostType.getAvsmot().add(createSaksbehandlerAvsender(metadata));
        journpostType.getAvsmot().add(createAvsender());
        return journpostType;
    }

    private String getForsendelseTittel() {
        if (StringUtils.hasText(forsendelse.getMetadataFraAvleverendeSystem().getTittel())) {
            return forsendelse.getMetadataFraAvleverendeSystem().getTittel();
        }
        if (StringUtils.hasText(forsendelse.getTittel())) {
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
