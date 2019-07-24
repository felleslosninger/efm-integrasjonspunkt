package no.difi.meldingsutveksling.serviceregistry;

import lombok.Data;
import no.difi.meldingsutveksling.NextMoveConsts;

import static java.lang.String.format;
import static no.difi.meldingsutveksling.NextMoveConsts.DEFAULT_SECURITY_LEVEL;

@Data
public class Parameters {

    private String identifier;
    private Integer securityLevel;

    public Parameters(String identifier) {
        this.identifier = identifier;
        this.securityLevel = DEFAULT_SECURITY_LEVEL;
    }

    public Parameters(String identifier, Integer securityLevel) {
        this.identifier = identifier;
        if (securityLevel == null) {
            this.securityLevel = NextMoveConsts.DEFAULT_SECURITY_LEVEL;
        } else {
            this.securityLevel = securityLevel;
        }
    }

    public String getQuery() {
        StringBuilder query = new StringBuilder();
        if (securityLevel != null) {
            query.append(format("securityLevel=%s", securityLevel));
        }

        return query.toString();
    }

}
