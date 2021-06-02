package no.difi.meldingsutveksling.serviceregistry;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

@Data
@Builder
public class SRParameter {

    @NonNull
    private String identifier;
    private String process;
    private Integer securityLevel;
    private Boolean print;
    @NonNull
    @Builder.Default
    private Boolean infoOnly = Boolean.FALSE;
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
        if (print == null) {
            return query.toString();
        }
        if (!print) {
            query.add(format("print=%s", print));
        }

            return query.toString();
    }

    public String getUrlTemplate() {
        if (infoOnly) {
            return "info/{identifier}";
        }
        StringBuilder urlTemplateBuilder = new StringBuilder();
        urlTemplateBuilder.append("identifier/{identifier}");
        if (! isNullOrEmpty(process)) {
            urlTemplateBuilder.append(("/process/{process}"));
        }
        List<String> queryParamTemplates = new ArrayList<>();
        if (securityLevel != null) {
            queryParamTemplates.add("securityLevel={securityLevel}");
        }
        if (! isNullOrEmpty(conversationId)) {
            queryParamTemplates.add("conversationId={conversationId}");
        }
        if (! queryParamTemplates.isEmpty()) {
            urlTemplateBuilder.append("?");
            urlTemplateBuilder.append(String.join("&", queryParamTemplates));
        }
        return urlTemplateBuilder.toString();
    }

    public Map<String, String> getUrlVariables() {
        Map<String, String> urlVariables = new HashMap<>();
        urlVariables.put("identifier", identifier);
        if (infoOnly) {
            return urlVariables;
        }
        if (! isNullOrEmpty(process)) {
            urlVariables.put("process", process);
        }
        if (securityLevel != null) {
            urlVariables.put("securityLevel", String.valueOf(securityLevel));
        }
        if (! isNullOrEmpty(conversationId)) {
            urlVariables.put("conversationId", conversationId);
        }
        return urlVariables;
    }

    private static SRParameterBuilder builder() {
        return new SRParameterBuilder();
    }

    public static SRParameterBuilder builder(String identifier) {
        return builder().identifier(identifier);
    }

}
