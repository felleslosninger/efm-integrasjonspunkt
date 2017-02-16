package no.difi.meldingsutveksling.config.dpi.securitylevel;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class SecurityLevelConverter implements Converter<String, SecurityLevel> {
    @Override
    public SecurityLevel convert(String integer) {
        SecurityLevel securityLevel;
        switch (integer) {
            case "3":
                securityLevel = SecurityLevel.LEVEL_3;
                break;
            case "4":
                securityLevel = SecurityLevel.LEVEL_4;
                break;
            default:
                securityLevel = SecurityLevel.INVALID;
        }
        return securityLevel;
    }
}
