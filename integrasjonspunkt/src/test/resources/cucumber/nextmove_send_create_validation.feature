Feature: Validation when attempting to send a Next Move DPO message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/910075918?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910075918.json"
    And a "GET" request to "http://localhost:9099/identifier/910077473?notification=obligated" will respond with status "200" and the following "application/json" in "/restmocks/identifier/910077473.json"

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
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-04-11T15:29:58.753+02:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b916",
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
        "arkivmelding": {
          "sikkerhetsnivaa": 3,
          "primaerDokumentNavn": "arkivmelding.xml"
        }
    }
    """
    Then the response status is "<status>"

    Examples:
      | receiver         | sender           | process                                                  | standard                                     | type           | expectedResponseDateTime | status |
      | "0192:910075918" | "0192:910077473" | "urn:no:difi:profile:arkivmelding:administrasjon:ver1.0" | "urn:no:difi.arkivmelding:xsd::arkivmelding" | "arkivmelding" | "2019-05-10T00:31:52Z"   | OK     |