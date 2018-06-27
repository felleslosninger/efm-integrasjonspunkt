package no.difi.meldingsutveksling.arkivmelding;

import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.EDUCoreConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.nextmove.message.MessagePersister;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
public class ArkivmeldingFactory {

    private IntegrasjonspunktProperties props;

    private MessagePersister persister;

    @Autowired
    public ArkivmeldingFactory(IntegrasjonspunktProperties props,
                               ObjectProvider<MessagePersister> persister) {
        this.props = props;
        this.persister = persister.getIfUnique();
    }

    public Arkivmelding createArkivmeldingAndWriteFiles(PutMessageRequestWrapper putMessage) {
        MeldingType mt = EDUCoreConverter.payloadAsMeldingType(putMessage.getPayload());
        no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory amOf = new no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory();
        Arkivmelding am = amOf.createArkivmelding();

        if (mt.getNoarksak() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Noarksak in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Saksmappe sm = amOf.createSaksmappe();
        ofNullable(mt.getNoarksak().getSaSaar()).map(BigInteger::new).ifPresent(sm::setSaksaar);
        ofNullable(mt.getNoarksak().getSaSeknr()).map(BigInteger::new).ifPresent(sm::setSakssekvensnummer);
        ofNullable(mt.getNoarksak().getSaAnsvinit()).ifPresent(sm::setSaksansvarlig);
        ofNullable(mt.getNoarksak().getSaAdmkort()).ifPresent(sm::setAdministrativEnhet);
        ofNullable(mt.getNoarksak().getSaOfftittel()).ifPresent(sm::setOffentligTittel);

        if (mt.getJournpost() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Journpost in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Journalpost jp = amOf.createJournalpost();
        ofNullable(mt.getJournpost().getJpJaar()).map(BigInteger::new).ifPresent(jp::setJournalaar);
        ofNullable(mt.getJournpost().getJpSeknr()).map(BigInteger::new).ifPresent(jp::setJournalsekvensnummer);
        ofNullable(mt.getJournpost().getJpJpostnr()).map(BigInteger::new).ifPresent(jp::setJournalpostnummer);
        ofNullable(mt.getJournpost().getJpNdoktype()).map(JournalposttypeMapper::getArkivmeldingType).ifPresent(jp::setJournalposttype);
        Skjerming skjerming = amOf.createSkjerming();
        ofNullable(mt.getJournpost().getJpUoff()).ifPresent(skjerming::setSkjermingshjemmel);
        sm.setSkjerming(skjerming);

        // expecting date in format yyyy-MM-dd
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Optional<String> jpDato = ofNullable(mt.getJournpost().getJpJdato());
        if (jpDato.isPresent()) {
            LocalDate localDate = LocalDate.parse(jpDato.get(), formatter);
            GregorianCalendar gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
            XMLGregorianCalendar xgcal = null;
            try {
                xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
            } catch (DatatypeConfigurationException e) {
                log.error("Could not convert JpJdato in message {}", putMessage.getConversationId(), e);
            }
            ofNullable(xgcal).ifPresent(jp::setJournaldato);
        }
        Optional<String> jpDokdato = ofNullable(mt.getJournpost().getJpDokdato());
        if (jpDokdato.isPresent()) {
            LocalDate localDate = LocalDate.parse(jpDokdato.get(), formatter);
            GregorianCalendar gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
            XMLGregorianCalendar xgcal = null;
            try {
                xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
            } catch (DatatypeConfigurationException e) {
                log.error("Could not convert JpJdato in message {}", putMessage.getConversationId(), e);
            }
            ofNullable(xgcal).ifPresent(jp::setDokumentetsDato);
        }

        mt.getJournpost().getAvsmot().forEach(a -> {
            Korrespondansepart kp = amOf.createKorrespondansepart();
            ofNullable(a.getAmNavn()).ifPresent(kp::setKorrespondansepartNavn);
            ofNullable(a.getAmAdmkort()).ifPresent(kp::setAdministrativEnhet);
            ofNullable(a.getAmSbhinit()).ifPresent(kp::setSaksbehandler);

            if ("0".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.AVSENDER);
            }
            if ("1".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.MOTTAKER);
            }

            Avskrivning avs = amOf.createAvskrivning();
            ofNullable(a.getAmAvskm()).filter(s -> !s.isEmpty()).map(AvskrivningsmaateMapper::getArkivmeldingType).ifPresent(avs::setAvskrivningsmaate);
            ofNullable(a.getAmAvsavdok()).ifPresent(avs::setReferanseAvskrivesAvJournalpost);
            if (!isNullOrEmpty(a.getAmAvskdato())) {
                LocalDate localDate = LocalDate.parse(a.getAmAvskdato(), formatter);
                GregorianCalendar gcal = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
                XMLGregorianCalendar xgcal = null;
                try {
                    xgcal = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
                } catch (DatatypeConfigurationException e) {
                    log.error("Could not convert AmAvskdato in message {}", putMessage.getConversationId(), e);
                }
                ofNullable(xgcal).ifPresent(avs::setAvskrivningsdato);
            }

            jp.getAvskrivning().add(avs);
            jp.getKorrespondansepart().add(kp);

        });

        mt.getJournpost().getDokument().forEach(d -> {
            Dokumentbeskrivelse dbeskr = amOf.createDokumentbeskrivelse();
            dbeskr.setTittel(d.getDbTittel());
            ofNullable(d.getDlRnr()).map(BigInteger::new).ifPresent(dbeskr::setDokumentnummer);
            ofNullable(d.getDlType()).map(TilknyttetRegistreringSomMapper::getArkivmeldingType).ifPresent(dbeskr::setTilknyttetRegistreringSom);

            Dokumentobjekt dobj = amOf.createDokumentobjekt();
            dobj.setReferanseDokumentfil(d.getVeFilnavn());
            ofNullable(d.getVeVariant()).map(VariantformatMapper::getArkivmeldingType).ifPresent(dobj::setVariantformat);

            dbeskr.getDokumentobjekt().add(dobj);
            jp.getDokumentbeskrivelseAndDokumentobjekt().add(dbeskr);

            try {
                writeDokument(putMessage.getConversationId(), d.getVeFilnavn(), d.getFil().getBase64());
            } catch (IOException e) {
                log.error("Could not write file {} from message {}", d.getVeFilnavn(),  putMessage.getConversationId());
                throw new MeldingsUtvekslingRuntimeException(e);
            }
        });

        sm.getBasisregistrering().add(jp);
        am.getMappe().add(sm);
        return am;
    }

    private void writeDokument(String convId, String filename, byte[] content) throws IOException {
        persister.write(convId, filename, content);
    }
}