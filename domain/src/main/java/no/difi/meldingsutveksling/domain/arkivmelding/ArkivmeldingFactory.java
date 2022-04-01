package no.difi.meldingsutveksling.domain.arkivmelding;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.arkivverket.standarder.noark5.arkivmelding.*;
import no.arkivverket.standarder.noark5.metadatakatalog.Korrespondanseparttype;
import no.difi.meldingsutveksling.DateTimeUtil;
import no.difi.meldingsutveksling.config.IntegrasjonspunktProperties;
import no.difi.meldingsutveksling.core.BestEduConverter;
import no.difi.meldingsutveksling.domain.MeldingsUtvekslingRuntimeException;
import no.difi.meldingsutveksling.noarkexchange.PutMessageRequestWrapper;
import no.difi.meldingsutveksling.noarkexchange.schema.core.MeldingType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.TimeZone;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArkivmeldingFactory {
    private final IntegrasjonspunktProperties properties;

    public Arkivmelding from(PutMessageRequestWrapper putMessage) {
        MeldingType mt = BestEduConverter.payloadAsMeldingType(putMessage.getPayload());
        no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory amOf = new no.arkivverket.standarder.noark5.arkivmelding.ObjectFactory();
        Arkivmelding am = amOf.createArkivmelding();

        if (mt.getNoarksak() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Noarksak in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Saksmappe sm = amOf.createSaksmappe();
        ofNullable(mt.getNoarksak().getSaSaar()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(sm::setSaksaar);
        ofNullable(mt.getNoarksak().getSaSeknr()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(sm::setSakssekvensnummer);
        ofNullable(mt.getNoarksak().getSaAnsvinit()).ifPresent(sm::setSaksansvarlig);
        ofNullable(mt.getNoarksak().getSaAdmkort()).ifPresent(sm::setAdministrativEnhet);
        ofNullable(mt.getNoarksak().getSaOfftittel()).ifPresent(sm::setOffentligTittel);
        ofNullable(mt.getNoarksak().getSaId()).ifPresent(sm::setSystemID);
        ofNullable(mt.getNoarksak().getSaDato()).map(DateTimeUtil::toXMLGregorianCalendar).ifPresent(sm::setSaksdato);
        ofNullable(mt.getNoarksak().getSaTittel()).ifPresent(sm::setTittel);
        ofNullable(mt.getNoarksak().getSaStatus()).map(SaksstatusMapper::getArkivmeldingType).ifPresent(sm::setSaksstatus);
        ofNullable(mt.getNoarksak().getSaArkdel()).ifPresent(sa -> sm.getReferanseArkivdel().add(sa));
        ofNullable(mt.getNoarksak().getSaJenhet()).ifPresent(sm::setJournalenhet);

        if (mt.getJournpost() == null) {
            throw new MeldingsUtvekslingRuntimeException(format("No Journpost in MeldingType for message %s, aborting conversion", putMessage.getConversationId()));
        }
        Journalpost jp = amOf.createJournalpost();

        ofNullable(mt.getJournpost().getJpId()).ifPresent(jp::setSystemID);
        ofNullable(mt.getJournpost().getJpInnhold()).ifPresent(jp::setTittel);
        ofNullable(mt.getJournpost().getJpJaar()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(jp::setJournalaar);
        ofNullable(mt.getJournpost().getJpForfdato()).map(DateTimeUtil::toXMLGregorianCalendar).ifPresent(jp::setForfallsdato);
        ofNullable(mt.getJournpost().getJpSeknr()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(jp::setJournalsekvensnummer);
        ofNullable(mt.getJournpost().getJpJpostnr()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(jp::setJournalpostnummer);
        ofNullable(mt.getJournpost().getJpNdoktype()).map(JournalposttypeMapper::getArkivmeldingType).ifPresent(jp::setJournalposttype);
        ofNullable(mt.getJournpost().getJpStatus()).map(JournalstatusMapper::getArkivmeldingType).ifPresent(jp::setJournalstatus);
        ofNullable(mt.getJournpost().getJpArkdel()).ifPresent(jp::setReferanseArkivdel);
        ofNullable(mt.getJournpost().getJpAntved()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(jp::setAntallVedlegg);
        ofNullable(mt.getJournpost().getJpOffinnhold()).ifPresent(jp::setOffentligTittel);


        Skjerming skjerming = amOf.createSkjerming();
        ofNullable(mt.getJournpost().getJpUoff()).ifPresent(skjerming::setSkjermingshjemmel);
        sm.setSkjerming(skjerming);

        // expecting date in format yyyy-MM-dd
        Optional<String> jpDato = ofNullable(mt.getJournpost().getJpJdato());
        if (jpDato.isPresent()) {
            jp.setJournaldato(DateTimeUtil.toXMLGregorianCalendar(mt.getJournpost().getJpJdato()));
        }
        Optional<String> jpDokdato = ofNullable(mt.getJournpost().getJpDokdato());
        if (jpDokdato.isPresent()) {
            jp.setDokumentetsDato(DateTimeUtil.toXMLGregorianCalendar(mt.getJournpost().getJpDokdato()));
        }

        mt.getJournpost().getAvsmot().forEach(a -> {
            Korrespondansepart kp = amOf.createKorrespondansepart();
            ofNullable(a.getAmNavn()).ifPresent(kp::setKorrespondansepartNavn);
            ofNullable(a.getAmAdmkort()).ifPresent(kp::setAdministrativEnhet);
            ofNullable(a.getAmSbhinit()).ifPresent(kp::setSaksbehandler);

            ofNullable(a.getAmAdresse()).ifPresent(adr -> kp.getPostadresse().add(adr));
            ofNullable(a.getAmPostnr()).ifPresent(kp::setPostnummer);
            ofNullable(a.getAmPoststed()).ifPresent(kp::setPoststed);
            ofNullable(a.getAmUtland()).ifPresent(kp::setLand);

            if ("0".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.AVSENDER);
            }
            if ("1".equals(a.getAmIhtype())) {
                kp.setKorrespondanseparttype(Korrespondanseparttype.MOTTAKER);
            }

            Avskrivning avs = amOf.createAvskrivning();
            ofNullable(a.getAmAvskm()).filter(s -> !s.isEmpty()).map(AvskrivningsmaateMapper::getArkivmeldingType).ifPresent(avs::setAvskrivningsmaate);
            ofNullable(a.getAmAvsavdok()).ifPresent(avs::setReferanseAvskrivesAvJournalpost);
            if (StringUtils.hasText(a.getAmAvskdato())) {
                avs.setAvskrivningsdato(DateTimeUtil.toXMLGregorianCalendar(a.getAmAvskdato()));
            }

            jp.getAvskrivning().add(avs);
            jp.getKorrespondansepart().add(kp);

        });

        mt.getJournpost().getDokument().forEach(d -> {
            Dokumentbeskrivelse dbeskr = amOf.createDokumentbeskrivelse();
            dbeskr.setTittel(d.getDbTittel());
            ofNullable(d.getDlRnr()).filter(s -> !isNullOrEmpty(s)).map(BigInteger::new).ifPresent(dbeskr::setDokumentnummer);
            ofNullable(d.getDlType()).map(TilknyttetRegistreringSomMapper::getArkivmeldingType).ifPresent(dbeskr::setTilknyttetRegistreringSom);

            Dokumentobjekt dobj = amOf.createDokumentobjekt();
            dobj.setReferanseDokumentfil(d.getVeFilnavn());
            ofNullable(d.getVeVariant()).map(VariantformatMapper::getArkivmeldingType).ifPresent(dobj::setVariantformat);

            dbeskr.getDokumentobjekt().add(dobj);
            jp.getDokumentbeskrivelseAndDokumentobjekt().add(dbeskr);
        });

        sm.getBasisregistrering().add(jp);
        am.getMappe().add(sm);
        am.setSystem(properties.getNoarkSystem().getType());
        am.setMeldingId(putMessage.getConversationId());
        am.setTidspunkt(DateTimeUtil.toXMLGregorianCalendar(mt.getNoarksak().getSaDato()));
        am.setAntallFiler(Integer.parseInt(mt.getJournpost().getJpAntved()));
        return am;
    }

}