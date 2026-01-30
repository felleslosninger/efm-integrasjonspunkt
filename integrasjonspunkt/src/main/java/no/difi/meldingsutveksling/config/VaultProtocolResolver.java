package no.difi.meldingsutveksling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.util.KeyValueDelegate;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class VaultProtocolResolver implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private final Map<String, ByteArrayResource> vaultResources = new HashMap<>();

    @Override
    public void initialize(ConfigurableApplicationContext c) {
        final String prefix = "vault:";
        c.addProtocolResolver((String location, ResourceLoader rl) -> {
            if (location.startsWith(prefix)) {
                if (vaultResources.isEmpty()) {
                    Environment props = c.getBean(Environment.class);
                    VaultTemplate vt = new VaultTemplate(
                            VaultEndpoint.from(URI.create(props.getProperty("vault.uri", ""))),
                            new TokenAuthentication(props.getProperty("vault.token", ""))
                    );
                    KeyValueDelegate del = new KeyValueDelegate(vt);
                    String path = props.getProperty("vault.resource-path", "");
                    del.isVersioned(path);
                    var secret = del.getSecret(path);
                    if (secret != null) {
                        try {
                            var data = secret.getData();
                            if (data != null) {
                                for (Map.Entry<String, Object> it : data.entrySet()) {
                                    log.info("Loaded resource with key '{}' from vault", it.getKey());
                                    String value = String.valueOf(it.getValue());
                                    byte[] decode = Base64.getDecoder().decode(value.replace("\n", ""));
                                    vaultResources.put(it.getKey(), new ByteArrayResource(decode));
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Unable to access Vault secret data via KeyValueDelegate: {}", e.getMessage());
                        }
                    }
                }
                Resource res = vaultResources.get(location.substring(prefix.length()));
                if (res == null) {
                    throw new RuntimeException("Vault resource \"" + location + "\" defined, but not located in vault");
                }
                return res;
            } else {
                return null;
            }
        });
    }

    @Override
    public int getOrder() {
        return 1;
    }

}
