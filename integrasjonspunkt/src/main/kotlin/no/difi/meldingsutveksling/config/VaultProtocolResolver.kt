package no.difi.meldingsutveksling.config

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.env.Environment
import org.springframework.core.io.ByteArrayResource
import org.springframework.vault.authentication.TokenAuthentication
import org.springframework.vault.client.VaultEndpoint
import org.springframework.vault.core.VaultTemplate
import org.springframework.vault.core.util.KeyValueDelegate
import java.net.URI
import java.util.*

class VaultProtocolResolver : ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    val log = LoggerFactory.getLogger(VaultProtocolResolver::class.java)
    private val vaultResources = mutableMapOf<String, ByteArrayResource>()

    override fun initialize(c: ConfigurableApplicationContext) {
        val prefix = "vault:"
        c.addProtocolResolver { location, _ ->
            if (location.startsWith(prefix)) {
                if (vaultResources.isEmpty()) {
                    val props = c.getBean(Environment::class.java)
                    val vt = VaultTemplate(VaultEndpoint.from(URI.create(props.getProperty("vault.uri", ""))),
                            TokenAuthentication(props.getProperty("vault.token", "")))
                    val del = KeyValueDelegate(vt)
                    val path = props.getProperty("vault.resource-path", "")
                    del.isVersioned(path)
                    del.getSecret(path)?.data?.entries?.map {
                        log.info("Loaded resource with key \'${it.key}\' from vault")
                        val decode = Base64.getDecoder().decode((it.value as String).replace("\n", ""))
                        vaultResources[it.key] = ByteArrayResource(decode)
                    }
                }
                vaultResources[location.removePrefix(prefix)]
                        ?: throw RuntimeException("Vault resource \"$location\" defined, but not located in vault")
            } else {
                null
            }
        }
    }

    override fun getOrder(): Int {
        return 1
    }

}