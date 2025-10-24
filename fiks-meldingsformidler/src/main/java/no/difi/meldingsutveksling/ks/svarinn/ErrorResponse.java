package no.difi.meldingsutveksling.ks.svarinn;

import java.util.Objects;

class ErrorResponse {

    private String feilmelding;
    private boolean permanent = true;

    public ErrorResponse() {
    }

    public ErrorResponse(String feilmelding) {
        this.feilmelding = feilmelding;
        this.permanent = true;
    }

    public ErrorResponse(String feilmelding, boolean permanent) {
        this.feilmelding = feilmelding;
        this.permanent = permanent;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }

    public boolean isPermanent() {
        return permanent;
    }

    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return permanent == that.permanent && Objects.equals(feilmelding, that.feilmelding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(feilmelding, permanent);
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
            "feilmelding='" + feilmelding + '\'' +
            ", permanent=" + permanent +
            '}';
    }

}
