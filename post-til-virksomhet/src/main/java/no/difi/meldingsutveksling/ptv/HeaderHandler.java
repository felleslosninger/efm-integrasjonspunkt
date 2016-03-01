package no.difi.meldingsutveksling.ptv;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

public class HeaderHandler implements SOAPHandler<SOAPMessageContext> {
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        Boolean outboundProperty = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        if(outboundProperty) {
            SOAPMessage soapMessage = context.getMessage();

            try {
                SOAPHeader soapHeader = soapMessage.getSOAPHeader();
                SOAPElement security = soapHeader.addChildElement("Security", "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd");
                security.addAttribute(new QName("xmlns:wsu"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd");
                SOAPElement userNameToken = security.addChildElement("UsernameToken", "wsse");
                userNameToken.addAttribute(new QName("wsu:Id"), "UsernameToken-1");
                SOAPElement userName = userNameToken.addChildElement("Username", "wsse");
                userName.addTextNode("AAS_TEST");
                SOAPElement password = userNameToken.addChildElement("Password", "wsse");
                password.addAttribute(new QName("Type"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
                password.addTextNode("6GMSx5n8");
                SOAPElement nonce = userNameToken.addChildElement("Nonce", "wsse");
                nonce.addAttribute(new QName("EncodingType"), "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
                nonce.addTextNode("TX5SU6e9hEm4rtNlHOF/iQ==");
                SOAPElement created = userNameToken.addChildElement("Created", "wsu");
                created.addTextNode(new Date().toString());

                soapMessage.writeTo(System.out);
                System.out.println("");
            } catch (SOAPException e) {
                throw new RuntimeException("Failed to add WS-Security to SOAP header", e);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            SOAPMessage soapMessage = context.getMessage();
            try {
                soapMessage.writeTo(System.out);
                System.out.println("");
            } catch (SOAPException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return outboundProperty;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return true;
    }

    @Override
    public void close(MessageContext context) {

    }
}
