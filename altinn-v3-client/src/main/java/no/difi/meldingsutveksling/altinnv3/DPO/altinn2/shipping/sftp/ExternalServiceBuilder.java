package no.difi.meldingsutveksling.altinnv3.DPO.altinn2.shipping.sftp;

import java.math.BigInteger;

public class ExternalServiceBuilder {
    private String externalServiceCode;
    private BigInteger externalServiceEditionCode;

    public ExternalServiceBuilder withExternalServiceCode(String value) {
        this.externalServiceCode = value;
        return this;
    }

    public ExternalServiceBuilder withExternalServiceEditionCode(BigInteger value) {
        this.externalServiceEditionCode = value;
        return this;
    }

    public ExternalService build() {
        return new ExternalService(externalServiceCode, externalServiceEditionCode);
    }

    public class ExternalService {
        private String externalServiceCode;
        private BigInteger externalServiceEditionCode;

        public String getExternalServiceCode() {
            return externalServiceCode;
        }

        public BigInteger getExternalServiceEditionCode() {
            return externalServiceEditionCode;
        }

        private ExternalService(String externalServiceCode, BigInteger externalServiceEditionCode) {
            this.externalServiceCode = externalServiceCode;
            this.externalServiceEditionCode = externalServiceEditionCode;
        }
    }
}
