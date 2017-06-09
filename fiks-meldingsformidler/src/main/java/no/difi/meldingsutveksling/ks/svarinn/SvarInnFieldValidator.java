package no.difi.meldingsutveksling.ks.svarinn;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;

public class SvarInnFieldValidator {

    private List<String> missingFields;

    private SvarInnFieldValidator() {
        this.missingFields = Lists.newArrayList();
    }

    public static SvarInnFieldValidator validator() {
        return new SvarInnFieldValidator();
    }

    public SvarInnFieldValidator addField(String value, String fieldName) {
        if (Strings.isNullOrEmpty(value)) {
            missingFields.add(fieldName);
        }

        return this;
    }

    public List<String> getMissing() {
        return missingFields;
    }
}
