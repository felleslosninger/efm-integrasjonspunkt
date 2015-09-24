package difi
import wslite.soap.SOAPClient

/**
 * Test client for integration tests
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

    /**
     *
     * @param senderPartynumber
     * @param recieverPartyNumber
     * @param bestedu
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
