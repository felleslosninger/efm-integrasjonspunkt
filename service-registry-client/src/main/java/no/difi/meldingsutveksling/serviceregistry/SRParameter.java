package no.difi.meldingsutveksling.serviceregistry;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.StringJoiner;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

@Data
@Builder
public class SRParameter {

    @NonNull
    private String identifier;
    private String process;
    private Integer securityLevel;
    // Used as correlation id - no need for this to affect caching
    @EqualsAndHashCode.Exclude
    private String conversationId;

    public String getQuery() {
        StringJoiner query = new StringJoiner("&");

        if (securityLevel != null) {
            query.add(format("securityLevel=%s", securityLevel));
        }
        if (!isNullOrEmpty(conversationId)) {
            query.add(format("conversationId=%s", conversationId));
        }

        return query.toString();
    }

    private static SRParameterBuilder builder() {
        return new SRParameterBuilder();
    }

    public static SRParameterBuilder builder(String identifier) {
        return builder().identifier(identifier);
    }

}
