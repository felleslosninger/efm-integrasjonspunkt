package no.difi.meldingsutveksling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.difi.meldingsutveksling.domain.ICD;
import no.difi.meldingsutveksling.domain.Iso6523;
import no.difi.meldingsutveksling.domain.PartnerIdentifier;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@ConfigurationPropertiesBinding
public class Iso6523Converter implements Converter<String, Iso6523> {

    @Override
    public Iso6523 convert(@NotNull String source) {
        if (Pattern.compile("^\\d{9}$").matcher(source).matches()) {
            return Iso6523.of(ICD.NO_ORG, source);
        }
        return Iso6523.parse(source);
    }
}