package difi

import groovy.json.JsonSlurper
import wslite.rest.RESTClient

/**
 * Client to check and/or create new entries in the Adresseregister service
 */
class CertificatesClient {
    def client

    CertificatesClient(String url) {
        client = new RESTClient(url)
    }

    public void addOrganization(String partynumber, String certificate) {
        client.post(path: '/certificates', headers: ['Content-Type':"application/json"]) {
            type "application/json"
            json organizationNumber : partynumber, orgName : "DIFI", pem : certificate
        }
    }

    public boolean exists(String reciever) {
        def result = client.get(path: '/certificates/search/findByorganizationNumber',
                headers: ['Content-Type': 'application/json'],
                query: [organizationNumber: reciever])
        return new JsonSlurper().parseText(result.text).size()
    }
}
