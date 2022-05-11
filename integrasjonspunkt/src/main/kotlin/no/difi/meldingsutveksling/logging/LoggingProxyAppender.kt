package no.difi.meldingsutveksling.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.encoder.Encoder
import io.rsocket.metadata.WellKnownMimeType
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitFirstOrNull
import no.difi.move.common.oauth.JwtTokenClient
import no.difi.move.common.oauth.JwtTokenConfig
import no.difi.move.common.oauth.JwtTokenResponse
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.security.rsocket.metadata.BearerTokenAuthenticationEncoder
import org.springframework.security.rsocket.metadata.BearerTokenMetadata
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.LinkedBlockingDeque

class LoggingProxyAppender : AppenderBase<ILoggingEvent>() {

    private val authMime = MimeTypeUtils.parseMimeType(WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION.string)

    private var proxyHost: String = ""
    private var proxyRSocketPort: Int = 0
    private var orgnr: String = ""
    private var kspath: String = ""
    private lateinit var encoder: Encoder<ILoggingEvent>
    private lateinit var jwtTokenConfig: JwtTokenConfig

    private lateinit var job: Job
    private lateinit var rsreq: RSocketRequester
    private lateinit var queue: LinkedBlockingDeque<ILoggingEvent>

    override fun start() {
        var errors = 0
        if (proxyHost.isBlank()) {
            addError("Property \"${this::proxyHost.name}\" not set for appender \"$name\"")
            errors++
        }
        if (proxyRSocketPort == 0) {
            addError("Property \"${this::proxyRSocketPort.name}\" not set for appender \"$name\"")
            errors++
        }
        if (!this::encoder.isInitialized) {
            addError("Property \"${this::encoder.name}\" not set for appender \"$name\"")
            errors++
        }
        if (!this::jwtTokenConfig.isInitialized) {
            addError("Property \"${this::jwtTokenConfig.name}\" not set for appender \"$name\"")
            errors++
        }
        if (errors > 0) return

        encoder.start()
        encoder.context = getContext()

        if (!jwtTokenConfig.tokenUri.isNullOrBlank() && jwtTokenConfig.tokenUri != "tokenUri_IS_UNDEFINED") {
            queue = LinkedBlockingDeque(2000)
            rsreq = setupRSocketRequester()

            if (jwtTokenConfig.clientId.isBlank() || jwtTokenConfig.clientId == "clientId_IS_UNDEFINED") {
                jwtTokenConfig.clientId = "MOVE_IP_$orgnr"
            }
            jwtTokenConfig.keystore.path = FileSystemResourceLoader().getResource(kspath)
            // TODO set eformidling scope
            jwtTokenConfig.scopes = listOf("move/dpe.read")

            val jwtTokenClient = JwtTokenClient(jwtTokenConfig)

            job = CoroutineScope(Dispatchers.IO).launch {
                while (this.isActive) {
                    queue.peek()?.let {
                        try {
                            postEvent(it, jwtTokenClient.fetchToken())
                        } catch (e: Exception) {
                            when (e) {
                                is CancellationException -> throw e
                                else -> addWarn("Dropped event, unrecoverable exception: ", e)
                            }
                        }
                        queue.poll()
                    } ?: delay(500L)
                }
            }
        }

        super.start()
    }

    private fun setupRSocketRequester() = RSocketRequester.builder()
        .rsocketStrategies(
            RSocketStrategies.builder()
                .encoder(BearerTokenAuthenticationEncoder())
                .build()
        )
        .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
        .rsocketConnector {
            it.reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2))
                .doBeforeRetry { retry ->
                    addWarn("Connection lost, retrying... $retry")
                }
            )
        }
        .tcp(proxyHost, proxyRSocketPort)

    private suspend fun postEvent(le: ILoggingEvent, token: JwtTokenResponse) {
        if (!encoder.isStarted) {
            encoder.start()
        }

        rsreq.route("log-fire")
            .metadata(BearerTokenMetadata(token.accessToken), authMime)
            .data(encoder.encode(le))
            .retrieveMono(Void::class.java)
            .onErrorResume { Mono.error(it.cause ?: it) }
            .awaitFirstOrNull()
    }

    override fun stop() {
        encoder.stop()
        if (this::job.isInitialized) {
            job.cancel("Appender stopped")
        }
        super.stop()
    }

    override fun append(eventObject: ILoggingEvent) {
        if (this::queue.isInitialized) {
            if (!queue.offer(eventObject)) {
                addWarn("Dropped event, queue at max capacity: ${queue.size} items")
            }
        } else {
            addWarn("Tried to log event to uninitialized appender: $eventObject")
        }
    }

}