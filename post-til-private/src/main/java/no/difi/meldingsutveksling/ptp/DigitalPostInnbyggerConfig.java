/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.ptp;

import no.difi.sdp.client2.domain.Prioritet;
import no.difi.sdp.client2.domain.digital_post.Sikkerhetsnivaa;
import org.springframework.core.io.Resource;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Nikolai Luthman <nikolai dot luthman at inmeta dot no>
 */
public class DigitalPostInnbyggerConfig {

    private String endpoint;

    @Valid
    private Keystore keystore;

    private FeatureToggle feature;

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

    @NotNull
    private Sikkerhetsnivaa securityLevel;

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

    public Keystore getKeystore() {
        return keystore;
    }

    public void setKeystore(Keystore keystore) {
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

    public Sikkerhetsnivaa getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(Sikkerhetsnivaa securityLevel) {
        this.securityLevel = securityLevel;
    }

    public void setFeature(FeatureToggle feature) {
        this.feature = feature;
    }

    public FeatureToggle getFeature() {
        return feature != null ? feature : new FeatureToggle();
    }

    public boolean isEnableEmailNotification() {
        return getFeature().isEnableEmailNotification();
    }

    public boolean isEnableSmsNotification() {
        return getFeature().isEnableSmsNotification();
    }

    public static class FeatureToggle {
        private boolean enableEmailNotification = false;
        private boolean enableSmsNotification = false;

        public boolean isEnableEmailNotification() {
            return enableEmailNotification;
        }

        public boolean isEnableSmsNotification() {
            return enableSmsNotification;
        }

        public void setEnableEmailNotification(boolean enableEmailNotification) {
            this.enableEmailNotification = enableEmailNotification;
        }

        public void setEnableSmsNotification(boolean enableSmsNotification) {
            this.enableSmsNotification = enableSmsNotification;
        }
    }

    public static class Keystore {

        /**
         * Keystore alias for key.
         */
        @NotNull
        private String alias;
        /**
         * Path of jks file.
         */

        @NotNull
        private Resource path;
        /**
         * Password of keystore and entry.
         */
        @NotNull
        private String password;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Resource getPath() {
            return path;
        }

        public void setPath(Resource path) {
            this.path = path;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

    }

}
