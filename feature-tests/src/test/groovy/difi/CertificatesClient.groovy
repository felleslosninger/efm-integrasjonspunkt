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

    public static void main(String[] args) {
        def adresseRegister = new CertificatesClient("http://localhost:9999")
        def partyNumber = "910077471"

        boolean exists = adresseRegister.exists(partyNumber)
        if(!exists) {
            adresseRegister
                    .addOrganization(
                    partyNumber,
                    '-----BEGIN CERTIFICATE-----\nMIID/DCCAuSgAwIBAgIENA0fKzANBgkqhkiG9w0BAQsFADBeMRIwEAYDVQQKEwlEaWZpIHRlc3Qx\nEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlm\naWF0IGludGVybWVkaWF0ZTAeFw0xNTAxMDEyMzQ2MzNaFw0xNzAyMDEyMzQ2MzNaMD0xEjAQBgNV\nBAUTCTk4NzQ2NDI5MTEnMCUGA1UEAxMeRElGSSB0ZXN0IHZpcmtzb21oZXRzc2VydGlmaWF0MIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAzsS7f45nBO8bL+nQ/oxUHEaSRg+zhwWZ8HVt\nFucoXi+hLt89IFM/OiC9YYaidjGo8P+C4FfDzeUJolsApeQ3c0I94Uhz+MICi0GgPYKbyw9EPMTD\nmuCkjbaMk3e+EbuzDiE2usYNWtIpzdRgqPTxOXToBydD4qFx8rLOfTzRqufTOD85xLTfKm0tlibM\n25z4pf9FMgPnZ8c735EN/Pe7ok2uVpnWDj9YlESGJyhUeQJKZotNsGILAm6o5hNWBUh7bY18rDiG\nZjPjZ36JH0sQRsITRy3Nhc/KpxkDMqXY2LcotMM8XoilI/YKkhJvg/e0qYT6fnFcDaU46hzYVSn9\nwwIDAQABo4HiMIHfMIGLBgNVHSMEgYMwgYCAFBwaROx+jMV17T8nbBo+6Vf0hn38oWKkYDBeMRIw\nEAYDVQQKEwlEaWZpIHRlc3QxEjAQBgNVBAUTCTk5MTgyNTgyNzE0MDIGA1UEAxMrRElGSSB0ZXN0\nIHZpcmtzb21oZXRzc2VydGlmaWF0IGludGVybWVkaWF0ZYIEIbToeDAdBgNVHQ4EFgQU1Nh6rWdb\n8KkNiKfOcQjiAfuGJCEwCQYDVR0TBAIwADAVBgNVHSAEDjAMMAoGCGCEQgEBAQFkMA4GA1UdDwEB\n/wQEAwIEsDANBgkqhkiG9w0BAQsFAAOCAQEAAcMiUkXq1IO3M/wU1YbdGr6+2dhsgxKaGeDt7bl1\nefjyENXI6dM2dspfyVI+/deIqX7VW/ay8AqiNJyFlvA9CMxW51+FivdjGENzRAKGF3pFsvdwNBEw\nFQSZCYoo8/gm59SidmnPNFeziUsE3fbQ22BPxW3l8ScSbYhgLlK9Tkr/ul3h7ByVtUdolP99eyCp\n1/TgC8EBZHZRC1v221+0AQ09A/SI/gyomgCeXVfH1Ll08v7BCTE1nE1aUqMDpDjOeWc73+f2X6vb\nUQdK4QwRU+pl5Oz6QgAFZ2mOD6DmqRfVoibM9sWgCkO5t6lpW86E/wixZBfS9TW/RJgH7461gg==\n-----END CERTIFICATE-----\n'
            )
        } else {
            println "organization number $partyNumber already exists in Adresseregister"
        }
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
