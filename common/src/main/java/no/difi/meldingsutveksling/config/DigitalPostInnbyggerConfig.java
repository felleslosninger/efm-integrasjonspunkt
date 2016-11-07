/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

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

    /**
     * ID for queue messages are sent to and their corresponding receipts can be retrieved from.
     * This is to avoid reading receipts from other applications that use the same service
     */
    @NotNull
    private String mpcId;

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
