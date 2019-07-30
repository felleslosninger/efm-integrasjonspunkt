package no.difi.meldingsutveksling.serviceregistry;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@ToString
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @JsonProperty(value = "error_code")
    public String errorCode;
    @JsonProperty(value = "error_description")
    public String errorDescription;
}
