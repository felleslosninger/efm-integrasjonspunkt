package no.difi.meldingsutveksling.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.util.KeyValueDelegate;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class VaultProtocolResolver implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private final Logger log = LoggerFactory.getLogger(VaultProtocolResolver.class);
    private final Map<String, ByteArrayResource> vaultResources = new HashMap<>();

    @Override
    public void initialize(ConfigurableApplicationContext c) {
        final String prefix = "vault:";
        c.addProtocolResolver((String location, org.springframework.core.io.ResourceLoader rl) -> {
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
                    Object secret = del.getSecret(path);
                    if (secret != null) {
                        try {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) secret.getClass().getMethod("getData").invoke(secret);
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
