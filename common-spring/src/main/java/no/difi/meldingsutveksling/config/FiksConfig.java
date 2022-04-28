package no.difi.meldingsutveksling.config;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;
import lombok.ToString;
import no.difi.move.common.config.KeystoreProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.unit.DataSize;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.net.URL;
import java.util.Map;
import java.util.Set;

@Data
public class FiksConfig {
    private boolean kryptert = true;

    @Valid
    @NotNull(message = "Certificate properties for FIKS not set.")
    @NestedConfigurationProperty
    private KeystoreProperties keystore;

    SvarUt ut = new SvarUt();
    SvarInn inn = new SvarInn();
    FiksIO io = new FiksIO();

    @Data
    @ToString(exclude = "password")
    public static class FiksCredentials {
        @NotEmpty
        private String username;
        @NotEmpty
        private String password;
    }

    @Data
    @ToString(exclude = "integrasjonsPassord")
    public static class FiksIO {
        private String senderOrgnr;
        private String host;
        private String apiHost;
        private String kontoId;
        private String integrasjonsId;
        private String integrasjonsPassord;
        private DataSize uploadSizeLimit;
    }

    @Data
    @ToString(exclude = "password")
    public static class SvarUt {
        @Valid()
        @Pattern(regexp = "^[a-zA-Z0-9\\-\\.øæåØÆÅ]{0,20}$")
        private String konteringsKode;
        private String username;
        private String password;
        private boolean kunDigitalLevering;
        private URL endpointUrl;
        private DataSize uploadSizeLimit;
        private Set<String> ekskluderesFraPrint = Sets.newHashSet();
        private Map<String, FiksCredentials> paaVegneAv = Maps.newHashMap();
    }

    @Data
    @ToString(exclude = "password")
    public static class SvarInn {
        private boolean enable;
        @NotNull
        private String baseUrl;
        private String username;
        private String password;
        private boolean mailOnError;
        private String mailSubject;
        private String fallbackSenderOrgNr;
        @NotNull
        private String process;
        @NotNull
        private String documentType;
        @NotNull
        private Integer connectTimeout;
        @NotNull
        private Integer readTimeout;
        private String orgnr;
        private Map<String, FiksCredentials> paaVegneAv = Maps.newHashMap();
    }
}
