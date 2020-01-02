package no.difi.meldingsutveksling.config.dpi.dpi;

public class PrintSettings {
    private ReturnType returnType;
    private InkType inkType;
    private ShippingType shippingType;

    public PrintSettings(ReturnType returnType, InkType inkType, ShippingType shippingType) {
        this.returnType = returnType;
        this.inkType = inkType;
        this.shippingType = shippingType;
    }

    public PrintSettings() {
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public void setReturnType(ReturnType returnType) {
        this.returnType = returnType;
    }

    public InkType getInkType() {
        return inkType;
    }

    public void setInkType(InkType inkType) {
        this.inkType = inkType;
    }

    public ShippingType getShippingType() {
        return shippingType;
    }

    public void setShippingType(ShippingType shippingType) {
        this.shippingType = shippingType;
    }
}
