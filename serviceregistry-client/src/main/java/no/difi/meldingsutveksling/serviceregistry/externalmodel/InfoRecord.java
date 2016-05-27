package no.difi.meldingsutveksling.serviceregistry.externalmodel;

import java.io.Serializable;

/**
 *
 */
public class InfoRecord implements Serializable {

    private boolean smpRecordAvailable;
    private boolean certificateAvailable;

    public InfoRecord(boolean hasSmpRecord, boolean hasCertificateRecord) {
        this.smpRecordAvailable = hasSmpRecord;
        this.certificateAvailable = hasCertificateRecord;
    }

    public InfoRecord() {
    }

    public boolean isSmpRecordAvailable() {
        return smpRecordAvailable;
    }

    public void setSmpRecordAvailable(boolean smpRecordAvailable) {
        this.smpRecordAvailable = smpRecordAvailable;
    }

    public boolean isCertificateAvailable() {
        return certificateAvailable;
    }

    public void setCertificateAvailable(boolean certificateAvailable) {
        this.certificateAvailable = certificateAvailable;
    }

    @Override
    public String toString() {
        return "InfoRecord{" +
                "smpRecordAvailable=" + smpRecordAvailable +
                ", certificateAvailable=" + certificateAvailable +
                '}';
    }
}
