package no.difi.meldingsutveksling.ks.svarinn;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class SvarInnFieldValidator {

    private List<String> missingFields = new ArrayList<>();

    public static SvarInnFieldValidator validator() {
        return new SvarInnFieldValidator();
    }

    public SvarInnFieldValidator addField(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            missingFields.add(fieldName);
        }

        return this;
    }

    public List<String> getMissing() {
        return missingFields;
    }
}
