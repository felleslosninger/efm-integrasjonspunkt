package difi

import wslite.soap.SOAPClient

/**
 * Eksempel på hvordan man bruker Groovy med SOAP request som xml
 */
class GroovyExample1TestClient {
    def url = "http://localhost:9091/integrasjonspunkt/noarkExchange"
    def client = new SOAPClient(url)

    def canGetReceiveMessage(int orgnr) {
        def request = """
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://www.arkivverket.no/Noark/Exchange/types">
   <soapenv:Header/>
   <soapenv:Body>
      <typ:GetCanReceiveMessageRequest>
         <receiver>
            <orgnr>${orgnr}</orgnr>
            <!--Optional:-->
            <name>?</name>
            <!--Optional:-->
            <email>?</email>
            <!--Optional:-->
            <ref>?</ref>
         </receiver>
      </typ:GetCanReceiveMessageRequest>
   </soapenv:Body>
</soapenv:Envelope>"""
        return client.send().body.toString().toBoolean()
    }
}
