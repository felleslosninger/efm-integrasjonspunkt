package no.difi.meldingsutveksling.dpi;


import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Request implements MeldingsformidlerRequest {
    private boolean notifiable;
    private String mobileNumber;
    private String email;
    private String sms;

    Request() {}

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public List<Document> getAttachments() {
        return null;
    }

    @Override
    public String getMottakerPid() {
        return null;
    }

    @Override
    public String getSubject() {
        return null;
    }

    @Override
    public String getSenderOrgnumber() {
        return null;
    }

    @Override
    public Optional<String> getOnBehalfOfOrgnr() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getAvsenderIdentifikator() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getFakturaReferanse() {
        return Optional.empty();
    }

    @Override
    public String getConversationId() {
        return null;
    }

    @Override
    public String getPostkasseAdresse() {
        return null;
    }

    @Override
    public byte[] getCertificate() {
        return new byte[0];
    }

    @Override
    public String getOrgnrPostkasse() {
        return null;
    }

    @Override
    public String getEmailAddress() {
        return email;
    }

    @Override
    public String getSmsVarslingstekst() {
        return sms;
    }

    @Override
    public String getEmailVarslingstekst() {
        return email;
    }

    @Override
    public String getMobileNumber() {
        return mobileNumber;
    }

    @Override
    public boolean isNotifiable() {
        return notifiable;
    }

    @Override
    public boolean isPrintProvider() {
        return false;
    }

    @Override
    public PostAddress getPostAddress() {
        return null;
    }

    @Override
    public PostAddress getReturnAddress() {
        return null;
    }

    @Override
    public Sikkerhetsnivaa getSecurityLevel() {
        return null;
    }

    @Override
    public Date getVirkningsdato() {
        return null;
    }

    @Override
    public String getLanguage() {
        return "NO";
    }

    @Override
    public boolean isAapningskvittering() {
        return false;
    }

    @Override
    public Utskriftsfarge getPrintColor() {
        return Utskriftsfarge.SORT_HVIT;
    }

    @Override
    public Posttype getPosttype() {
        return Posttype.B_OEKONOMI;
    }

    @Override
    public Returhaandtering getReturnHandling() {
        return Returhaandtering.DIREKTE_RETUR;
    }

    Request withNotifiable(boolean notifiable) {
        this.notifiable = notifiable;
        return this;
    }

    Request withMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
        return this;
    }

    Request withEmail(String email) {
        this.email = email;
        return this;
    }

    Request withSms(String sms) {
        this.sms = sms;
        return this;
    }
}
