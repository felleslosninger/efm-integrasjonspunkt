/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.meldingsutveksling.config.dpi.dpi.PrintSettings;
import no.difi.sdp.client2.domain.Prioritet;
import org.springframework.util.unit.DataSize;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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
    private PrintSettings printSettings;

    @NotNull
    private DataSize uploadSizeLimit;

}
