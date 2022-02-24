/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package no.difi.meldingsutveksling.config;

import lombok.Data;
import no.difi.meldingsutveksling.config.dpi.dpi.PrintSettings;
import no.difi.meldingsutveksling.properties.LoggedProperty;
import no.difi.move.common.config.KeystoreProperties;
import no.difi.sdp.client2.domain.Prioritet;
import org.springframework.util.unit.DataSize;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Data
public class DigitalPostInnbyggerConfig {

    private String endpoint;

    @Valid
    private KeystoreProperties keystore;

    @Valid
    KeystoreProperties trustStore;

    /**
     * ID for queue messages are sent to and their corresponding receipts can be retrieved from.
     * This is to avoid reading receipts from other applications that use the same service
     */
    @NotNull
    @LoggedProperty
    private String mpcId;

    /**
     * The number of concurrent message partition channels (MPCs) to send messages to and consume receipts from.
     * An integer increment is postfixed to the MPC id if the MPC concurrency is greater than 1.
     *
     * MPC concurrency of 3 will use the following MPCs:
     * - {mpcId}-0
     * - {mpcId}-1
     * - {mpcId}-2
     */
    @NotNull
    private Integer mpcConcurrency;

    @NotNull
    private String language;

    @NotNull
    private Prioritet priority;

    @NotNull
    private PrintSettings printSettings;

    @NotNull
    private DataSize uploadSizeLimit;

    @NotNull
    private int clientMaxConnectionPoolSize;

    @NotNull
    private String krrPrintUrl;
}
