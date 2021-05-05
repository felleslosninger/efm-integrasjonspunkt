package no.difi.meldingsutveksling.nextmove;

import com.google.common.base.Strings;
import no.difi.meldingsutveksling.serviceregistry.externalmodel.ServiceRecord;
import no.difi.sdp.client2.domain.fysisk_post.Posttype;
import no.difi.sdp.client2.domain.fysisk_post.Returhaandtering;
import no.difi.sdp.client2.domain.fysisk_post.Utskriftsfarge;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationTargetException;

@Component
public class DpiPrintMessageFiller implements BusinessMessageFiller<DpiPrintMessage> {

    @Override
    public void setDefaults(BusinessMessage<?> message, ServiceRecord serviceRecord) {
        DpiPrintMessage printMessage = (DpiPrintMessage) message;
        if (printMessage.getMottaker() == null) {
            printMessage.setMottaker(new PostAddress());
        }
        setReceiverDefaults(printMessage.getMottaker(), serviceRecord.getPostAddress());
        if (printMessage.getRetur() == null) {
            printMessage.setRetur(new MailReturn()
                .setMottaker(new PostAddress())
                .setReturhaandtering(Returhaandtering.DIREKTE_RETUR));
        }
        setReceiverDefaults(printMessage.getRetur().getMottaker(), serviceRecord.getReturnAddress());

        if (printMessage.getUtskriftsfarge() == null) {
            printMessage.setUtskriftsfarge(Utskriftsfarge.SORT_HVIT);
        }

        if (printMessage.getPosttype() == null) {
            printMessage.setPosttype(Posttype.B_OEKONOMI);
        }
    }

    @Override
    public Class<DpiPrintMessage> getType() {
        return DpiPrintMessage.class;
    }

    private void setReceiverDefaults(PostAddress receiver, no.difi.meldingsutveksling.serviceregistry.externalmodel.PostAddress srPostAddress) {
        if (!StringUtils.hasText(receiver.getNavn())) {
            receiver.setNavn(srPostAddress.getName());
        }
        if (Strings.isNullOrEmpty(receiver.getAdresselinje1())) {
            String[] addressLines = srPostAddress.getStreet().split(";");
            for (int i=0; i < Math.min(addressLines.length, 4); i++) {
                try {
                    PropertyUtils.setProperty(receiver, "adresselinje"+(i+1), addressLines[i]);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new NextMoveRuntimeException(e);
                }
            }
        }
        if (!StringUtils.hasText(receiver.getPostnummer())) {
            receiver.setPostnummer(srPostAddress.getPostalCode());
        }
        if (!StringUtils.hasText(receiver.getPoststed())) {
            receiver.setPoststed(srPostAddress.getPostalArea());
        }
        if (!StringUtils.hasText(receiver.getLand())) {
            receiver.setLand(srPostAddress.getCountry());
        }
    }

}
