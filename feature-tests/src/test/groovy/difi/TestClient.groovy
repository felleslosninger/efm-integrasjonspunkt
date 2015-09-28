package difi
import wslite.soap.SOAPClient

/**
 * Test client for integration tests
 */
class TestClient {
    def client

    TestClient(String url) {
        this.client = new SOAPClient(url)
    }
/**
     * Calls the Integrasjonspunkt service to check whether an organization identified by partynumber can recieve messages.
     * Ie. if you can use PutMessage service on the Integrasjonspunkt
     * @param partynumber
     * @return true if they can recieve messages
     */
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

    /**
     * Invokes the Integrasjonspunkt service that takes BEST/EDU messages and sends it to a Transport
     * @param senderPartynumber
     * @param recieverPartyNumber
     * @param bestedu - a String representation of the BES/EDU message. Make sure it includes a jpId and a data element
     * @return the status type of the PutMessage operation
     */
    String putMessage(String senderPartynumber, String recieverPartyNumber, String bestedu) {
        def request = {
            soapNamespacePrefix "soapenv"
            envelopeAttributes 'xmlns:typ': "http://www.arkivverket.no/Noark/Exchange/types"
            body {
                "typ:PutMessageRequest" {
                    envelope {
                        sender {
                            orgnr(senderPartynumber)
                        }
                        receiver {
                            orgnr(recieverPartyNumber)
                        }
                    }
                    mkp.yieldUnescaped(bestedu)
                }
            }
        }
        def response = client.send(request)
        return response.body.PutMessageResponse.result.@type

    }
}
