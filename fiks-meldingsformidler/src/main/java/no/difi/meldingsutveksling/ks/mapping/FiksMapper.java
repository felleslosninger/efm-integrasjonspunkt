package no.difi.meldingsutveksling.ks.mapping;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.InputStreamDataSource;
import no.difi.meldingsutveksling.api.OptionalCryptoMessagePersister;
import no.difi.meldingsutveksling.arkivmelding.ArkivmeldingUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.dokumentpakking.service.CmsUtil;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalposttypeMapper;
import no.difi.meldingsutveksling.domain.arkivmelding.JournalstatusMapper;
import no.difi.meldingsutveksling.domain.sbdh.Scope;
import no.difi.meldingsutveksling.domain.sbdh.ScopeType;
import no.difi.meldingsutveksling.ks.svarut.*;
import no.difi.meldingsutveksling.nextmove.*;
import no.difi.meldingsutveksling.nextmove.message.FileEntryStream;
import no.difi.meldingsutveksling.pipes.Plumber;
import no.difi.meldingsutveksling.pipes.Reject;
import no.difi.meldingsutveksling.serviceregistry.ServiceRegistryLookup;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.InfoRecord;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.ARKIVMELDING_FILE;

@Slf4j
@Component
public class FiksMapper {

    private final IntegrasjonspunktProperties properties;
    private final ServiceRegistryLookup serviceRegistry;
    private final OptionalCryptoMessagePersister optionalCryptoMessagePersister;
    private final ObjectProvider<CmsUtil> cmsUtilProvider;
    private final Plumber plumber;
    private final ArkivmeldingUtil arkivmeldingUtil;

    public FiksMapper(IntegrasjonspunktProperties properties,
                      ServiceRegistryLookup serviceRegistry,
                      OptionalCryptoMessagePersister optionalCryptoMessagePersister,
                      ObjectProvider<CmsUtil> cmsUtilProvider,
                      Plumber plumber, ArkivmeldingUtil arkivmeldingUtil) {
        this.properties = properties;
        this.serviceRegistry = serviceRegistry;
        this.optionalCryptoMessagePersister = optionalCryptoMessagePersister;
        this.cmsUtilProvider = cmsUtilProvider;
        this.plumber = plumber;
        this.arkivmeldingUtil = arkivmeldingUtil;
    }

    public SendForsendelseMedId mapFrom(NextMoveOutMessage message, X509Certificate certificate, Reject reject) throws NextMoveException {
        Optional<String> senderRef = message.getSbd().findScope(ScopeType.SENDER_REF).map(Scope::getInstanceIdentifier);
        // Confirm that SenderRef is a valid UUID, else use messageId
        if (senderRef.isPresent()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(senderRef.get());
            } catch (IllegalArgumentException e) {
                senderRef = Optional.empty();
            }
        }
        String forsendelsesid = senderRef.orElse(message.getMessageId());
        return SendForsendelseMedId.builder()
                .withForsendelse(getForsendelse(message, certificate, reject))
                .withForsendelsesid(forsendelsesid)
                .build();
    }

    private Forsendelse getForsendelse(NextMoveOutMessage message, X509Certificate certificate, Reject reject) throws NextMoveException {
        Arkivmelding am = getArkivmelding(message);
        Saksmappe saksmappe = arkivmeldingUtil.getSaksmappe(am);
        Journalpost journalpost = arkivmeldingUtil.getJournalpost(am);

        Optional<String> receiverRef = message.getSbd().findScope(ScopeType.RECEIVER_REF).map(Scope::getInstanceIdentifier);
        if (receiverRef.isPresent()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                UUID.fromString(receiverRef.get());
            } catch (IllegalArgumentException e) {
                receiverRef = Optional.empty();
            }
        }

        return Forsendelse.builder()
                .withEksternref(message.getMessageId())
                .withForsendelseType(getForsendelseType(message))
                .withKunDigitalLevering(false)
                .withSvarPaForsendelse(receiverRef.orElse(null))
                .withTittel(journalpost.getOffentligTittel())
                .withKrevNiva4Innlogging(kreverNiva4Innlogging(message))
                .withKonteringskode(properties.getFiks().getUt().getKonteringsKode())
                .withKryptert(properties.getFiks().isKryptert())
                .withAvgivendeSystem(properties.getNoarkSystem().getType())
                .withPrintkonfigurasjon(getPrintkonfigurasjon())
                .withMottaker(getMottaker(message))
                .withSvarSendesTil(getSvarSendesTil(message, journalpost))
                .withMetadataFraAvleverendeSystem(metaDataFrom(saksmappe, journalpost))
                .withDokumenter(mapArkivmeldingDokumenter(message, getDokumentbeskrivelser(journalpost), certificate, reject))
                .build();
    }

    private String getForsendelseType(NextMoveOutMessage message) {
        return getDpfSettings(message)
                .map(DpfSettings::getForsendelseType)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private Optional<DpfSettings> getDpfSettings(NextMoveOutMessage message) {
        if (message.getBusinessMessage() instanceof ArkivmeldingMessage) {
            ArkivmeldingMessage arkivmeldingMessage = (ArkivmeldingMessage) message.getBusinessMessage();
            return Optional.ofNullable(arkivmeldingMessage.getDpf());
        }
        return Optional.empty();
    }

    private Printkonfigurasjon getPrintkonfigurasjon() {
        return Printkonfigurasjon.builder()
                .withTosidig(true)
                .withFargePrint(false)
                .withBrevtype(Brevtype.BPOST).build();
    }

    private Adresse getSvarSendesTil(NextMoveOutMessage message, Journalpost journalpost) {
        return journalpost.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .map(a -> mottakerFrom(a, message.getSenderIdentifier()))
                .findFirst()
                .orElseGet(() -> mottakerFrom(serviceRegistry.getInfoRecord(message.getSenderIdentifier())));
    }

    private boolean kreverNiva4Innlogging(NextMoveOutMessage message) {
        Integer sikkerhetsnivaa = message.getBusinessMessage().getSikkerhetsnivaa();
        return sikkerhetsnivaa != null && sikkerhetsnivaa == 4;
    }

    private Set<Dokumentbeskrivelse> getDokumentbeskrivelser(Journalpost journalpost) {
        return journalpost.getDokumentbeskrivelseAndDokumentobjekt().stream()
                .filter(Dokumentbeskrivelse.class::isInstance)
                .map(Dokumentbeskrivelse.class::cast)
                .collect(Collectors.toSet());
    }

    private Adresse getMottaker(NextMoveOutMessage message) {
        final InfoRecord receiverInfo = serviceRegistry.getInfoRecord(message.getReceiverIdentifier());
        return mottakerFrom(receiverInfo);
    }

    private Arkivmelding getArkivmelding(NextMoveOutMessage message) throws NextMoveException {
        String identifier = getArkivmeldingIdentifier(message);

        try (InputStream is = new ByteArrayInputStream(optionalCryptoMessagePersister.read(message.getMessageId(), identifier))) {
            return arkivmeldingUtil.unmarshalArkivmelding(is);
        } catch (JAXBException | IOException e) {
            throw new NextMoveRuntimeException("Failed to get Arkivmelding", e);
        }
    }

    private String getArkivmeldingIdentifier(NextMoveOutMessage message) throws NextMoveException {
        return message.getFiles().stream()
                .filter(f -> ARKIVMELDING_FILE.equals(f.getFilename()))
                .findAny()
                .map(BusinessMessageFile::getIdentifier)
                .orElseThrow(() -> new NextMoveException(format("No attachement \"%s\" found", ARKIVMELDING_FILE)));
    }

    private Set<Dokument> mapArkivmeldingDokumenter(NextMoveOutMessage message, Set<Dokumentbeskrivelse> docs, X509Certificate cert, Reject reject) {
        return docs.stream()
                .flatMap(p -> p.getDokumentobjekt().stream())
                .map(d -> getBusinessMessageFile(message, d.getReferanseDokumentfil()))
                .map(file -> getDocument(message.getMessageId(), file, cert, reject))
                .collect(Collectors.toSet());
    }

    private BusinessMessageFile getBusinessMessageFile(NextMoveOutMessage message, String referanseDokumentfil) {
        return message.getFiles().stream()
                .filter(bmf -> bmf.getFilename().equals(referanseDokumentfil))
                .findFirst()
                .orElseThrow(() -> new NextMoveRuntimeException(
                        String.format("File '%s' referenced in '%s' not found", referanseDokumentfil, message.getMessageId())));
    }

    private Dokument getDocument(String messageId, BusinessMessageFile file, X509Certificate cert, Reject reject) {
        FileEntryStream fileEntryStream = optionalCryptoMessagePersister.readStream(messageId, file.getIdentifier(), reject);

        return Dokument.builder()
                .withData(getDataHandler(cert, fileEntryStream.getInputStream(), reject))
                .withFilnavn(file.getFilename())
                .withMimetype(file.getMimetype())
                .build();
    }

    private DataHandler getDataHandler(X509Certificate cert, InputStream is, Reject reject) {
        PipedInputStream encrypted = plumber.pipe("encrypt attachment for FIKS forsendelse",
                        inlet -> cmsUtilProvider.getIfAvailable().createCMSStreamed(is, inlet, cert), reject)
                .outlet();

        return new DataHandler(InputStreamDataSource.of(encrypted));
    }

    private NoarkMetadataFraAvleverendeSakssystem metaDataFrom(Saksmappe sm, Journalpost jp) {
        return NoarkMetadataFraAvleverendeSakssystem.builder()
                .withSakssekvensnummer(toInt(sm.getSakssekvensnummer()))
                .withSaksaar(toInt(sm.getSaksaar()))
                .withJournalaar(toInt(jp.getJournalaar()))
                .withJournalsekvensnummer(toInt(jp.getJournalsekvensnummer()))
                .withJournalpostnummer(toInt(jp.getJournalpostnummer()))
                .withJournalposttype(JournalposttypeMapper.getNoarkType(jp.getJournalposttype()))
                .withJournalstatus(JournalstatusMapper.getNoarkType(jp.getJournalstatus()))
                .withJournaldato(DateTimeUtil.atStartOfDay(jp.getJournaldato()))
                .withDokumentetsDato(DateTimeUtil.atStartOfDay(jp.getDokumentetsDato()))
                .withTittel(jp.getOffentligTittel())
                .withSaksbehandler(getSaksbehandler(jp).orElse(null))
                .build();
    }

    private Optional<String> getSaksbehandler(Journalpost jp) {
        return jp.getKorrespondansepart().stream()
                .filter(k -> k.getKorrespondanseparttype().equals(Korrespondanseparttype.AVSENDER))
                .findFirst()
                .map(Korrespondansepart::getSaksbehandler);
    }

    private int toInt(BigInteger x) {
        return x == null ? 0 : x.intValueExact();
    }

    private Adresse mottakerFrom(Korrespondansepart kp, String orgnr) {
        return Adresse.builder()
                .withDigitalAdresse(OrganisasjonDigitalAdresse.builder().withOrgnr(orgnr).build())
                .withPostAdresse(PostAdresse.builder()
                        .withNavn(kp.getKorrespondansepartNavn())
                        .withAdresse1(String.join(" ", kp.getPostadresse()))
                        .withPostnr(kp.getPostnummer())
                        .withPoststed(kp.getPoststed())
                        .withLand(kp.getLand())
                        .build())
                .build();
    }

    private Adresse mottakerFrom(InfoRecord infoRecord) {
        return Adresse.builder()
                .withDigitalAdresse(OrganisasjonDigitalAdresse.builder()
                        .withOrgnr(infoRecord.getIdentifier())
                        .build())
                .withPostAdresse(getPostAdresse(infoRecord))
                .build();
    }

    private PostAdresse getPostAdresse(InfoRecord infoRecord) {
        PostAdresse.Builder<Void> builder = PostAdresse.builder()
                .withNavn(infoRecord.getOrganizationName());

        if (infoRecord.getPostadresse() != null) {
            builder.withAdresse1(infoRecord.getPostadresse().getAdresse())
                    .withPostnr(infoRecord.getPostadresse().getPostnummer())
                    .withPoststed(infoRecord.getPostadresse().getPoststed())
                    .withLand(infoRecord.getPostadresse().getLand());
        } else {
            builder.withPostnr("0192")
                    .withPoststed("Oslo")
                    .withLand("Norge");
        }

        return builder.build();
    }
}
