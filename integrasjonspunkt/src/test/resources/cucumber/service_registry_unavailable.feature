Feature: Service Registry is temporarily unavailable

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910077473?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And a "GET" request to "http://localhost:9099/identifier/910076787" will fail with status "503"
    And a "GET" request to "http://localhost:9099/identifier/910076787/process/urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0?securityLevel=3&conversationId=8b2a6e1c-5d1f-4c4e-9c1e-000000000503" will fail with status "503"
    And a "GET" request to "http://localhost:9099/identifier/910076788" will fail with status "404"
    And a "GET" request to "http://localhost:9099/identifier/910076788/process/urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0?securityLevel=3&conversationId=8b2a6e1c-5d1f-4c4e-9c1e-000000000404" will fail with status "404"

  Scenario: Capabilities lookup responds with 503 when Service Registry is temporarily unavailable
    Given I request all capabilities for "910076787" and get a "SERVICE_UNAVAILABLE" response
    Then the lookup error message is "ServiceRegistry is temporarily unavailable. Please try again later."

  Scenario: Capabilities lookup still responds with 404 when the receiver is not found in Service Registry
    Given I request all capabilities for "910076788" and get a "NOT_FOUND" response

  Scenario: Service record lookup responds with 503 when Service Registry is temporarily unavailable
    Given I request the service record for "910076787" and get a "SERVICE_UNAVAILABLE" response
    Then the lookup error message is "ServiceRegistry is temporarily unavailable. Please try again later."

  Scenario Outline: Creating a message responds with <status> when Service Registry responds with <srStatus>
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
                        "identifier": "urn:no:difi:profile:arkivmelding:planByggOgGeodata:ver1.0",
                        "instanceIdentifier": "<conversationId>",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "<messageId>",
                "standard": "urn:no:difi:arkivmelding:xsd::arkivmelding",
                "type": "arkivmelding",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:<receiver>"
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
        "arkivmelding": { "sikkerhetsnivaa": 3, "hoveddokument": "arkivmelding.xml" }
    }
    """
    Then the response status is "<status>"

    Examples:
      | srStatus | receiver  | conversationId                       | messageId                            | status              |
      | 503      | 910076787 | 8b2a6e1c-5d1f-4c4e-9c1e-000000000503 | 0f4b8a52-7a3c-4d0e-8f7e-000000000503 | SERVICE_UNAVAILABLE |
      | 404      | 910076788 | 8b2a6e1c-5d1f-4c4e-9c1e-000000000404 | 0f4b8a52-7a3c-4d0e-8f7e-000000000404 | BAD_REQUEST         |
