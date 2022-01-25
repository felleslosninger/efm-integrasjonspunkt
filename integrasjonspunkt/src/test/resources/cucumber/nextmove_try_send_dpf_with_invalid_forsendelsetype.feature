Feature: Try sending a Next Move DPF message with an invalid forsendelsetype

  Background:

    Given a "GET" request to "http://localhost:9099/identifier/987464291?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/987464291.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924?securityLevel=3&conversationId=22efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924.json"
    And a "GET" request to "http://localhost:9099/identifier/910075924/process/urn:no:difi:profile:arkivmelding:administrasjon:ver1.0?securityLevel=3&conversationId=22efbd4c-413d-4e2c-bbc5-257ef4a65a91" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075924-administrasjon.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/info/910075924" will respond with status "200" and the following "application/json" in "/restmocks/info/910075924.json"
    And a "GET" request to "http://localhost:9099/info/910077473" will respond with status "200" and the following "application/json" in "/restmocks/info/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And the Noark System is disabled
    And a SOAP request to "https://test.svarut.ks.no/tjenester/forsendelseservice/ForsendelsesServiceV9" with element "retreiveForsendelseTyper" will respond with the following payload:
    """
    <ser:retreiveForsendelseTyperResponse xmlns:ser="http://www.ks.no/svarut/servicesV9">
       <!--Optional:-->
       <return>forsendelsetype-1</return>
    </ser:retreiveForsendelseTyperResponse>
    """

  Scenario: As a user I want to send a DPF message with invalid forsendelsetype
    Given I POST the following message:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": "2019-05-10T00:31:52+01:00"
                            }
                        ],
                        "identifier": "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0",
                        "instanceIdentifier": "22efbd4c-413d-4e2c-bbc5-257ef4a65a91",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "d17b3a72-22f4-4269-9743-089637bf87f9",
                "standard": "urn:no:difi:arkivmelding:xsd::arkivmelding",
                "type": "arkivmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910075924"
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:910077473"
                    }
                }
            ]
        },
        "arkivmelding": {
          "sikkerhetsnivaa": 3,
          "dpf": {
              "forsendelseType": "forsendelsetype-2"
              },
          "hoveddokument": "arkivmelding.xml"
        }
    }
    """
    Then the response status is "BAD_REQUEST"