/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

import lombok.Data;
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
@Data
public class DigitalPostInnbyggerConfig {

    private String endpoint;

    @Valid
    private KeyStoreProperties keystore;

    @Valid
    KeyStoreProperties trustStore;

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
