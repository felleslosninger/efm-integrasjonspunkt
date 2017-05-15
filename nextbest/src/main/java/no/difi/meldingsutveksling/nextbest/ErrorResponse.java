package no.difi.meldingsutveksling.nextbest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("unused")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonProperty(value = "error")
    private String error;
    @JsonProperty(value = "error_description")
    private String errorDescription;

    private ErrorResponse() {
    }

    public static class Builder {

        private ErrorResponse instance;

        public Builder() {
            this.instance = new ErrorResponse();
        }

        public Builder error(String error) {
            instance.error = error;
            return this;
        }

        public Builder errorDescription(String errorDescription) {
            instance.errorDescription = errorDescription;
            return this;
        }

        public ErrorResponse build() {
            return instance;
        }

    }

    public static Builder builder() {
        return new Builder();
    }

}
