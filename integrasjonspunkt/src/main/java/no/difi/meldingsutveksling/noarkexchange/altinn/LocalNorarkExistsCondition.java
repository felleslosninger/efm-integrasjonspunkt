package no.difi.meldingsutveksling.noarkexchange.altinn;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

@Slf4j
public class LocalNorarkExistsCondition implements ConfigurationCondition {

    @Override
    public ConfigurationPhase getConfigurationPhase() {
        return ConfigurationPhase.REGISTER_BEAN;
    }

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String type = context.getEnvironment().getProperty("difi.move.noarkSystem.type", "");
        boolean localNoarkExists = StringUtils.hasText(type);
        log.info("difi.move.noarkSystem.type={} LocalNorarkExistsCondition={}", type, localNoarkExists);
        return localNoarkExists;
    }
}
