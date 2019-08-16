package no.difi.meldingsutveksling.noarkexchange;

import lombok.experimental.UtilityClass;
import no.difi.meldingsutveksling.nextmove.ArkivmeldingKvitteringMessage;
import no.difi.meldingsutveksling.noarkexchange.schema.AppReceiptType;
import no.difi.meldingsutveksling.noarkexchange.schema.StatusMessageType;

@UtilityClass
public class AppReceiptFactory {

    public AppReceiptType from(ArkivmeldingKvitteringMessage receipt) {
        AppReceiptType appReceipt = new AppReceiptType();
        appReceipt.setType(receipt.getReceiptType());
        if (receipt.getMessages() != null) {
            receipt.getMessages().forEach(sm -> {
                StatusMessageType statusMessageType = new StatusMessageType();
                statusMessageType.setText(sm.getText());
                statusMessageType.setCode(sm.getCode());
                appReceipt.getMessage().add(statusMessageType);
            });
        }
        return appReceipt;
    }

    public AppReceiptType from(String type, String code, String text) {
        AppReceiptType appReceipt = new AppReceiptType();
        appReceipt.setType(type);
        StatusMessageType statusMessage = new StatusMessageType();
        statusMessage.setCode(code);
        statusMessage.setText(text);
        appReceipt.getMessage().add(statusMessage);
        return appReceipt;
    }
}
