package no.difi.meldingsutveksling.ptv.receipt;

import com.google.common.base.MoreObjects;

/**
 * Object used to hold meta information for retreiving status requests for CorrespondenceAgency messages.
 */
public class CorrespondenceReceiptMeta {

    private String sendersReference;
    private String serviceCode;
    private String serviceEditionCode;

    public CorrespondenceReceiptMeta(String sendersReference, String serviceCode, String serviceEditionCode) {
        this.sendersReference = sendersReference;
        this.serviceCode = serviceCode;
        this.serviceEditionCode = serviceEditionCode;
    }


    public String getSendersReference() {
        return sendersReference;
    }

    public void setSendersReference(String sendersReference) {
        this.sendersReference = sendersReference;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public String getServiceEditionCode() {
        return serviceEditionCode;
    }

    public void setServiceEditionCode(String serviceEditionCode) {
        this.serviceEditionCode = serviceEditionCode;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sendersReference", sendersReference)
                .add("serviceCode", serviceCode)
                .add("serviceEditionCode", serviceEditionCode)
                .toString();
    }
}
