Feature: Validation when attempting to send a Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075918?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918/process/urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0?securityLevel=3&conversationId=37efbd4c-413d-4e2c-bbc5-000000000001" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-planogbygg.json"
    Given a "GET" request to "http://localhost:9099/identifier/910075918/process/urn:no:difi:profile:arkivmelding:helseSosialOgOmsorg:ver1.0?securityLevel=3&conversationId=37efbd4c-413d-4e2c-bbc5-000000000002" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918-helsesosial.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/identifier/09118532322?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/09118532322.json"
    And a "GET" request to "http://localhost:9099/identifier/17912099997?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/17912099997.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"

  Scenario Outline: As a user I want the message service to validate my input
    Given I POST the following message:
    """
    {
        "standardBusinessDocumentHeader": {
            "businessScope": {
                "scope": [
                    {
                        "scopeInformation": [
                            {
                                "expectedResponseDateTime": <expectedResponseDateTime>
                            }
                        ],
                        "identifier": <process>,
                        "instanceIdentifier": <conversationId>,
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": <creationDateAndTime>,
                "instanceIdentifier": <documentId>,
                "standard": <standard>,
                "type": <type>,
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": <receiver>
                    }
                }
            ],
            "sender": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": <sender>
                    }
                }
            ]
        },
        <businessDocument>
    }
    """
    Then the response status is "<status>"

    Examples:
      | conversationId                         | documentId                             | creationDateAndTime         | receiver         | sender           | process                                                       | standard                                           | type           | expectedResponseDateTime    | businessDocument                                                                                                                                                                                                                                                               | status      |
      | "37efbd4c-413d-4e2c-bbc5-000000000001" | "ff88849c-e281-4809-8555-7cd54952b001" | "2019-03-25T11:35:00+01:00" | "0192:910075918" | "0192:910077473" | "urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0"   | "urn:no:difi:arkivmelding:xsd::arkivmelding"       | "arkivmelding" | "2019-05-10T00:31:52+01:00" | "arkivmelding": { "sikkerhetsnivaa": 3, "hoveddokument": "arkivmelding.xml" }                                                                                                                                                                                                  | OK          |
      | "37efbd4c-413d-4e2c-bbc5-000000000002" | "ff88849c-e281-4809-8555-7cd54952b002" | "2019-03-25T11:35:00+01:00" | "0192:910075918" | "0192:910077473" | "urn:no:difi:profile:arkivmelding:helseSosialOgOmsorg:ver1.0" | "urn:no:difi:arkivmelding:xsd::arkivmelding"       | "arkivmelding" | "2019-05-10T00:31:52+01:00" | "arkivmelding": { "sikkerhetsnivaa": 3, "hoveddokument": "arkivmelding.xml" }                                                                                                                                                                                                  | OK          |
      | "37efbd4c-413d-4e2c-bbc5-000000000001" | "ff88849c-e281-4809-8555-7cd54952b001" | "2019-03-25T11:35:00+01:00" | "0192:910075918" | "0192:910075918" | "urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0"   | "urn:no:difi:arkivmelding:xsd::arkivmelding"       | "arkivmelding" | "2019-05-10T00:31:52+01:00" | "arkivmelding": { "sikkerhetsnivaa": 3, "hoveddokument": "arkivmelding.xml" }                                                                                                                                                                                                  | BAD_REQUEST |
