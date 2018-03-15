/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

import no.difi.meldingsutveksling.config.dpi.PrintSettings;
import no.difi.meldingsutveksling.config.dpi.securitylevel.SecurityLevel;
import no.difi.meldingsutveksling.config.dpi.securitylevel.ValidSecurityLevel;
import no.difi.sdp.client2.domain.Prioritet;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static no.difi.meldingsutveksling.config.dpi.securitylevel.SecurityLevel.INVALID;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
public class DigitalPostInnbyggerConfig {

    private String endpoint;

    @Valid
    private KeyStoreProperties keystore;

    private FeatureToggle feature = new FeatureToggle();

    @Valid
    private IntegrasjonspunktProperties.Sms sms = new IntegrasjonspunktProperties.Sms();

    @Valid
    private Email email = new Email();

    /**
     * ID for queue messages are sent to and their corresponding receipts can be retrieved from.
     * This is to avoid reading receipts from other applications that use the same service
     */
    @NotNull
    private String mpcId;

    @NotNull
    private String language;

    @NotNull
    private Prioritet priority;

    @Valid
    @ValidSecurityLevel(message = "Gyldig verdi er 3 eller 4", invalidValues = INVALID)
    private SecurityLevel securityLevel;

    @NotNull
    private PrintSettings printSettings;

    private boolean forcePrint;

    public PrintSettings getPrintSettings() {
        return printSettings;
    }

    public void setPrintSettings(PrintSettings printSettings) {
        this.printSettings = printSettings;
    }

    public String getMpcId() {
        return mpcId;
    }

    public void setMpcId(String mpcId) {
        this.mpcId = mpcId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public KeyStoreProperties getKeystore() {
        return keystore;
    }

    public void setKeystore(KeyStoreProperties keystore) {
        this.keystore = keystore;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Prioritet getPriority() {
        return priority;
    }

    public void setPriority(Prioritet priority) {
        this.priority = priority;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void setFeature(FeatureToggle feature) {
        this.feature = feature;
    }

    public FeatureToggle getFeature() {
        return feature;
    }

    public boolean isEnableEmailNotification() {
        return getFeature().isEnableEmailNotification();
    }

    public boolean isEnableSmsNotification() {
        return getFeature().isEnableSmsNotification();
    }

    public IntegrasjonspunktProperties.Sms getSms() {
        return sms;
    }

    public void setSms(IntegrasjonspunktProperties.Sms sms) {
        this.sms = sms;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public boolean isForcePrint() {
        return forcePrint;
    }

    public void setForcePrint(boolean forcePrint) {
        this.forcePrint = forcePrint;
    }

    public static class FeatureToggle {
        private boolean enableEmailNotification = false;
        private boolean enableSmsNotification = false;
        private boolean enablePrint = false;

        boolean isEnableEmailNotification() {
            return enableEmailNotification;
        }

        boolean isEnableSmsNotification() {
            return enableSmsNotification;
        }

        public void setEnableEmailNotification(boolean enableEmailNotification) {
            this.enableEmailNotification = enableEmailNotification;
        }

        public void setEnableSmsNotification(boolean enableSmsNotification) {
            this.enableSmsNotification = enableSmsNotification;
        }

        public boolean isEnablePrint() {
            return enablePrint;
        }

        public void setEnablePrint(boolean enablePrint) {
            this.enablePrint = enablePrint;
        }
    }

    public static class Email {
        @Size(max=500)
        private String varslingstekst;

        public String getVarslingstekst() {
            return varslingstekst;
        }

        public void setVarslingstekst(String varslingstekst) {
            this.varslingstekst = varslingstekst;
        }
    }


}
