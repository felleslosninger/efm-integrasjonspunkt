package no.difi.meldingsutveksling.dpi.json;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.dpi.MeldingsformidlerRequest;
import no.difi.meldingsutveksling.dpi.client.domain.Shipment;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.BusinessMessage;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Digital;
import no.difi.meldingsutveksling.dpi.client.domain.messagetypes.Utskrift;
import no.difi.meldingsutveksling.dpi.client.domain.sbd.*;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.PostalCategory;
import no.difi.meldingsutveksling.nextmove.PrintColor;
import no.difi.meldingsutveksling.nextmove.ReturnHandling;
import no.difi.move.common.cert.X509CertificateHelper;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Optional;

@RequiredArgsConstructor
public class ShipmentFactory {

    private final ChannelNormalizer channelNormalizer;

    public Shipment getShipment(MeldingsformidlerRequest request) {
        return new Shipment()
                .setSender(request.getSender())
                .setReceiver(request.getPostkasseProvider())
                .setMessageId(request.getMessageId())
                .setConversationId(request.getConversationId())
                .setChannel(channelNormalizer.normalize(request.getMpcId()))
                .setExpectedResponseDateTime(request.getExpectedResponseDateTime())
                .setBusinessMessage(getBusinessMessage(request))
                .setParcel(request.getParcel())
                .setReceiverBusinessCertificate(X509CertificateHelper.createX509Certificate(request.getCertificate()))
                .setLanguage(request.getLanguage());
    }

    private BusinessMessage getBusinessMessage(MeldingsformidlerRequest request) {
        return request.isPrintProvider() ? getUtskrift(request) : getDigital(request);
    }

    private Utskrift getUtskrift(MeldingsformidlerRequest request) {
        return new Utskrift()
                .setAvsender(getAvsender(request))
                .setMottaker(getAddressInformation(request.getPostAddress()))
                .setUtskriftstype(getUtskriftstype(request.getPrintColor()))
                .setRetur(getRetur(request))
                .setPosttype(getPosttype(request.getPostalCategory()));
    }

    private Utskrift.Posttype getPosttype(PostalCategory postalCategory) {
        return (postalCategory == PostalCategory.A_PRIORITERT)
                ? Utskrift.Posttype.A
                : Utskrift.Posttype.B;
    }

    private Retur getRetur(MeldingsformidlerRequest request) {
        return new Retur()
                .setMottaker(getAddressInformation(request.getReturnAddress()))
                .setReturposthaandtering(getReturposthaandtering(request.getReturnHandling()));
    }

    private Retur.Returposthaandtering getReturposthaandtering(ReturnHandling returnHandling) {
        return (returnHandling == ReturnHandling.MAKULERING_MED_MELDING)
                ? Retur.Returposthaandtering.MAKULERING_MED_MELDING
                : Retur.Returposthaandtering.DIREKTE_RETUR;
    }

    private Utskrift.Utskriftstype getUtskriftstype(PrintColor printColor) {
        return (printColor == PrintColor.FARGE)
                ? Utskrift.Utskriftstype.FARGE
                : Utskrift.Utskriftstype.SORT_HVIT;
    }

    private AdresseInformasjon getAddressInformation(PostAddress postAddress) {
        AdresseInformasjon adresseInformasjon = new AdresseInformasjon()
                .setNavn(postAddress.getNavn())
                .setAdresselinje1(postAddress.getAdresselinje1())
                .setAdresselinje2(postAddress.getAdresselinje2())
                .setAdresselinje3(postAddress.getAdresselinje3())
                .setAdresselinje4(postAddress.getAdresselinje4());

        if (isNorway(postAddress)) {
            return adresseInformasjon
                    .setPostnummer(postAddress.getPostnummer())
                    .setPoststed(postAddress.getPoststed());
        }

        getPostal(postAddress).ifPresent(postal -> {
            if (StringUtils.hasText(adresseInformasjon.getAdresselinje2())) {
                if (StringUtils.hasText(adresseInformasjon.getAdresselinje3())) {
                    if (StringUtils.hasText(adresseInformasjon.getAdresselinje4())) {
                        adresseInformasjon.setAdresselinje4(adresseInformasjon.getAdresselinje4() + " " + postal);
                    } else {
                        adresseInformasjon.setAdresselinje4(postal);
                    }
                } else {
                    adresseInformasjon.setAdresselinje3(postal);
                }
            } else {
                adresseInformasjon.setAdresselinje2(postal);
            }
        });

        adresseInformasjon.setLand(postAddress.getLand());
        adresseInformasjon.setLandkode(postAddress.getLandkode());

        return adresseInformasjon;
    }

    private Optional<String> getPostal(PostAddress postAddress) {
        if (StringUtils.hasText(postAddress.getPostnummer())) {
            if (StringUtils.hasText(postAddress.getPoststed())) {
                return Optional.ofNullable(String.format("%s %s", postAddress.getPostnummer(), postAddress.getPoststed()));
            }

            return Optional.of(postAddress.getPostnummer());
        }

        if (StringUtils.hasText(postAddress.getPoststed())) {
            return Optional.ofNullable(postAddress.getPoststed());
        }

        return Optional.empty();
    }

    private boolean isNorway(PostAddress postAddress) {
        boolean isNorwegianLandString = false;
        boolean isNorwegianLandCode = false;
        if (StringUtils.hasText(postAddress.getLand())) {
            isNorwegianLandString = "Norway".equalsIgnoreCase(postAddress.getLand()) || "Norge".equalsIgnoreCase(postAddress.getLand())|| "Noreg".equalsIgnoreCase(postAddress.getLand());
        }
        if (StringUtils.hasText(postAddress.getLandkode())) {
            isNorwegianLandString = "Norway".equalsIgnoreCase(postAddress.getLandkode());
        }
        return isNorwegianLandString || isNorwegianLandCode;
    }

    private Digital getDigital(MeldingsformidlerRequest request) {
        return new Digital()
                .setAvsender(getAvsender(request))
                .setMottaker(new Personmottaker()
                        .setPostkasseadresse(request.getPostkasseAdresse()))
                .setSikkerhetsnivaa(request.getSecurityLevel())
                .setVirkningstidspunkt(request.getVirkningsdato())
                .setAapningskvittering(request.isAapningskvittering())
                .setIkkesensitivtittel(request.getSubject())
                .setSpraak(request.getLanguage())
                .setVarsler(new Varsler()
                        .setSmsvarsel(getSmsvarsel(request))
                        .setEpostvarsel(getEpostvarsel(request)));
    }

    private Epostvarsel getEpostvarsel(MeldingsformidlerRequest request) {
        return StringUtils.hasLength(request.getEmailAddress())
                && StringUtils.hasLength(request.getEmailVarslingstekst())
                ? new Epostvarsel()
                .setEpostadresse(request.getEmailAddress())
                .setVarslingstekst(request.getEmailVarslingstekst())
                .setRepetisjoner(Arrays.asList(0, 7))
                : null;
    }

    private Smsvarsel getSmsvarsel(MeldingsformidlerRequest request) {
        return StringUtils.hasLength(request.getMobileNumber())
                && StringUtils.hasLength(request.getSmsVarslingstekst())
                ? new Smsvarsel()
                .setMobiltelefonnummer(request.getMobileNumber())
                .setVarslingstekst(request.getSmsVarslingstekst())
                .setRepetisjoner(Arrays.asList(0, 7))
                : null;
    }

    private Avsender getAvsender(MeldingsformidlerRequest request) {
        return new Avsender()
                .setAvsenderidentifikator(request.getAvsenderIdentifikator())
                .setFakturaReferanse(request.getFakturaReferanse())
                .setVirksomhetsidentifikator(getVirksomhetsindikator(request));
    }

    private Identifikator getVirksomhetsindikator(MeldingsformidlerRequest request) {
        Iso6523 iso6523 = Optional.ofNullable(request.getOnBehalfOf()).orElseGet(request::getSender);
        return new Identifikator(iso6523.getAuthority(), iso6523.getIdentifier());
    }
}
