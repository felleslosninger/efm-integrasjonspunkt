package difi

import wslite.soap.SOAPClient

/**
 * Eksempel på hvordan man bruker kaller på SOAP tjeneste med SOAPMessageBuilder api
 */
class GroovyExample2TestClient {
    def url = "http://localhost:8080/noarkExchange"
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
}
