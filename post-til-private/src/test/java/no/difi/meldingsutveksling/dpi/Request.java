package no.difi.meldingsutveksling.dpi;


import no.difi.meldingsutveksling.domain.sbdh.StandardBusinessDocumentHeader;
import no.difi.meldingsutveksling.nextmove.PostAddress;
import no.difi.meldingsutveksling.nextmove.PostalCategory;
import no.difi.meldingsutveksling.nextmove.PrintColor;
import no.difi.meldingsutveksling.nextmove.ReturnHandling;
import no.digdir.dpi.client.domain.Document;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public class Request implements MeldingsformidlerRequest {
    private boolean notifiable;
    private String mobileNumber;
    private String email;
    private String sms;

    Request() {
    }

    @Override
    public Document getDocument() {
        return null;
    }

    @Override
    public List<Document> getAttachments() {
        return null;
    }

    @Override
    public StandardBusinessDocumentHeader getStandardBusinessDocumentHeader() {
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
    public Integer getSecurityLevel() {
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
    public PrintColor getPrintColor() {
        return PrintColor.SORT_HVIT;
    }

    @Override
    public PostalCategory getPostalCategory() {
        return PostalCategory.B_OEKONOMI;
    }

    @Override
    public ReturnHandling getReturnHandling() {
        return ReturnHandling.DIREKTE_RETUR;
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
