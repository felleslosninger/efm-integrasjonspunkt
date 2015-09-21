package difi

import wslite.soap.SOAPClient
import wslite.soap.SOAPResponse

/**
 * Eksempel på hvordan man bruker kaller på SOAP tjeneste med SOAPMessageBuilder api
 */
class TestClient {
    def url = "http://localhost:9091/noarkExchange"
    def client = new SOAPClient(url)

    boolean canGetRecieveMessage(int partynumber) {
        def request = {
            soapNamespacePrefix "soapenv"
            envelopeAttributes 'xmlns:typ': "http://www.arkivverket.no/Noark/Exchange/types"
            body {
                "typ:GetCanReceiveMessageRequest" {
                    receiver {
                        orgnr(partynumber)
                    }
                }
            }
        }
        return client.send(request).body.text().toBoolean()
    }

    SOAPResponse putMessage(String senderPartynumber, String recieverPartyNumber, byte[] bestedu) {
        def request = {
            soapNamespacePrefix "soapenv"
            envelopeAttributes 'xmlns:typ': "http://www.arkivverket.no/Noark/Exchange/types"
            body {
                "typ:PutMessageRequest" {
                    envelope {
                        sender {
                            orgnr(senderPartynumber)
                        }
                        reciever(recieverPartyNumber)
                    }

                    mkp.yieldUnescaped(bestedu)
                }
            }
        }
        def response = client.send(request)
    }
}
