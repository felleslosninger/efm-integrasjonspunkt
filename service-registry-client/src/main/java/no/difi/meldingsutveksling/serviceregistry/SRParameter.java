package no.difi.meldingsutveksling.serviceregistry;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import no.difi.meldingsutveksling.NextMoveConsts;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

@Data
@Builder
public class SRParameter {

    @NonNull
    private String identifier;
    @NonNull
    @Builder.Default
    private Integer securityLevel = NextMoveConsts.DEFAULT_SECURITY_LEVEL;
    // Used as correlation id - no need for this to affect caching
    @EqualsAndHashCode.Exclude
    private String conversationId;

    public String getQuery() {
        StringBuilder query = new StringBuilder();
        query.append(format("securityLevel=%s", securityLevel));
        if (!isNullOrEmpty(conversationId)) {
            query.append(format("&conversationId=%s", conversationId));
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
