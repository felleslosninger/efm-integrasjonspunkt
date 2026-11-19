package no.difi.meldingsutveksling.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies which variant of the Logstash appenders the production logback-spring.xml selects.
 * The file is loaded from src/main/resources, since src/test/resources has its own logback-spring.xml.
 * The springProperty elements are unknown to plain Joran and are skipped, so the properties are set on the context.
 */
class LogbackLogstashConfigTest {

    private static final Path CONFIG = Path.of("src/main/resources/logback-spring.xml");

    @TempDir
    Path tempDir;

    private final LoggerContext context = new LoggerContext();

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void stashUsesTlsWhenSecureLogstashIsTrue() throws Exception {
        configure("true", "true", null);

        assertThat(appender("stash").getSsl()).isNotNull();
    }

    @Test
    void stashDoesNotUseTlsWhenSecureLogstashIsFalse() throws Exception {
        configure("true", "false", null);

        assertThat(appender("stash")).isNotNull();
        assertThat(appender("stash").getSsl()).isNull();
    }

    @Test
    void stashDoesNotUseTlsWhenSecureLogstashIsNotSet() throws Exception {
        configure("true", null, null);

        assertThat(appender("stash")).isNotNull();
        assertThat(appender("stash").getSsl()).isNull();
    }

    @Test
    void stash2IsAddedWithoutTlsWhenDestination2IsSet() throws Exception {
        configure("true", "true", "localhost:1");

        assertThat(appender("stash").getSsl()).isNotNull();
        assertThat(appender("stash2")).isNotNull();
        assertThat(appender("stash2").getSsl()).isNull();
    }

    @Test
    void noLogstashAppendersWhenLogstashIsDisabled() throws Exception {
        configure("false", "true", "localhost:1");

        assertThat(appender("stash")).isNull();
        assertThat(appender("stash2")).isNull();
    }

    private void configure(String enableLogstash, String secureLogstash, String destination2) throws Exception {
        context.putProperty("LOG_FILE", tempDir.resolve("test.log").toString());
        context.putProperty("destination", "localhost:1");
        putIfSet("enableLogstash", enableLogstash);
        putIfSet("secureLogstash", secureLogstash);
        putIfSet("destination2", destination2);

        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(CONFIG.toFile());
    }

    private void putIfSet(String key, String value) {
        if (value != null) {
            context.putProperty(key, value);
        }
    }

    private LogstashTcpSocketAppender appender(String name) {
        return (LogstashTcpSocketAppender) context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender(name);
    }
}
