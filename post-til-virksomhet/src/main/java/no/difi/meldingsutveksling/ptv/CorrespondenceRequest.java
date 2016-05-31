package no.difi.meldingsutveksling.ptv;

public class CorrespondenceRequest {
    private String username;
    private String password;
    private Object payload;

    public CorrespondenceRequest() {
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Object getCorrespondence() {
        return payload;
    }

    public static class Builder {
        private String username;
        private String password;
        private Object payload;

        public Builder() {
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withPayload(Object payload) {
            this.payload = payload;
            return this;
        }

        public CorrespondenceRequest build() {
            final CorrespondenceRequest correspondenceRequest = new CorrespondenceRequest();
            correspondenceRequest.username = username;
            correspondenceRequest.password = password;
            correspondenceRequest.payload = payload;
            return correspondenceRequest;
        }
    }
}
