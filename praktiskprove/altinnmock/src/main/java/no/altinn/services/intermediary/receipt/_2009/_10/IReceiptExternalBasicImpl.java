package no.altinn.services.intermediary.receipt._2009._10;

import no.altinn.schemas.services.intermediary.receipt._2009._10.*;
import no.altinn.schemas.services.intermediary.receipt._2015._06.*;

import javax.jws.WebService;
import javax.xml.datatype.XMLGregorianCalendar;

@WebService(wsdlLocation = "/wsdl/ReceiptExternalBasic.wsdl", endpointInterface = "no.altinn.services.intermediary.receipt._2009._10.IReceiptExternalBasic")
public class IReceiptExternalBasicImpl implements IReceiptExternalBasic {
    @Override
    public void test() throws IReceiptExternalBasicTestAltinnFaultFaultFaultMessage {
    }

    @Override
    public ReceiptExternal getReceiptBasic(String systemUserName, String systemPassword, ReceiptSearchExternal receipt) throws IReceiptExternalBasicGetReceiptBasicAltinnFaultFaultFaultMessage {
        ReceiptExternal receiptExternal = new ReceiptExternal();
        receiptExternal.setReceiptStatusCode(ReceiptStatusEnum.OK);
        receiptExternal.setReceiptId(123);
        return receiptExternal;
    }

    @Override
    public Receipt getReceiptBasicV2(String systemUserName, String systemPassword, ReceiptSearch receipt) throws IReceiptExternalBasicGetReceiptBasicV2AltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public ReceiptExternalList getReceiptListBasic(String systemUserName, String systemPassword, ReceiptTypeEnum receiptTypeName, XMLGregorianCalendar dateFrom, XMLGregorianCalendar dateTo) throws IReceiptExternalBasicGetReceiptListBasicAltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public ReceiptList getReceiptListBasicV2(String systemUserName, String systemPassword, ReceiptType receiptTypeName, XMLGregorianCalendar dateFrom, XMLGregorianCalendar dateTo) throws IReceiptExternalBasicGetReceiptListBasicV2AltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public ReceiptExternal saveReceiptBasic(String systemUserName, String systemPassword, ReceiptSaveExternal receipt) throws IReceiptExternalBasicSaveReceiptBasicAltinnFaultFaultFaultMessage {
        return null;
    }

    @Override
    public Receipt updateReceiptBasic(String systemUserName, String systemPassword, ReceiptSave receipt) throws IReceiptExternalBasicUpdateReceiptBasicAltinnFaultFaultFaultMessage {
        return null;
    }
}
