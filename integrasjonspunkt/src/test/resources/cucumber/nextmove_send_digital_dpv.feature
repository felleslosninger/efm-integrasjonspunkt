Feature: Sending a Next Move Digital DPV message

  Background:
    Given a "GET" request to "http://localhost:9099/identifier/17912099997?securityLevel=3" will respond with status "200" and the following "application/json" in "/restmocks/identifier/17912099997.json"
    And a "GET" request to "http://localhost:9099/identifier/17912099997/process/urn:no:difi:profile:digitalpost:info:ver1.0?conversationId=97efbd4c-413d-4e2c-bbc5-257ef4a61212" will respond with status "200" and the following "application/json" in "/restmocks/identifier/17912099997-info.json"
    And a "GET" request to "http://localhost:9099/identifier/974720760" will respond with status "200" and the following "application/json" in "/restmocks/identifier/974720760.json"
    And a "GET" request to "http://localhost:9099/info/910077473" will respond with status "200" and the following "application/json" in "/restmocks/info/910077473.json"
    And a "GET" request to "http://localhost:9099/virksert/910077473" will respond with status "200" and the following "text/plain" in "/restmocks/virksert/910077473"
    And a CorrespondenceClient request to "/correspondence/api/v1/correspondence/upload" will respond with the following payload:
    """
        {
          "correspondences" : [ {
            "correspondenceId" : "0198408f-979c-75e0-b23e-aef6e907015f",
            "status" : "Initialized",
            "recipient" : "urn:altinn:organization:identifier-no:17912099997",
            "notifications" : null
          } ],
          "attachmentIds" : [ "0198408f-9736-7048-a634-39138f4ccbdd", "0198408f-973c-7ba2-a161-40dec03900fb" ]
        }
    """

  Scenario: As a user I want to send a Digital DPV message
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
                        "identifier": "urn:no:difi:profile:digitalpost:info:ver1.0",
                        "instanceIdentifier": "97efbd4c-413d-4e2c-bbc5-257ef4a61212",
                        "type": "ConversationId"
                    }
                ]
            },
            "documentIdentification": {
                "creationDateAndTime": "2019-03-25T11:35:00+01:00",
                "instanceIdentifier": "ff88849c-e281-4809-8555-7cd54952b922",
                "standard": "urn:no:difi:digitalpost:xsd:digital::digital_dpv",
                "type": "digital_dpv",
                "typeVersion": "2.0"
            },
            "headerVersion": "1.0",
            "receiver": [
                {
                    "identifier": {
                        "authority": "iso6523-actorid-upis",
                        "value": "0192:17912099997"
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
        "digital_dpv": {
            "tittel": "foo",
            "sammendrag": "bar",
            "innhold": "foobar"
        }
    }
    """
    And the response status is "OK"
    And I upload a file named "test.txt" with mimetype "text/plain" and title "Test" with the following body:
    """
    Testing 1 2 3
    """
    And I send the message
    Then the CorrespondenceAgencyClient is called with the following payload:
    """
    {
      "correspondence.content.attachments[0].sendersReference" : "AttachmentReference_as123452",
      "correspondence.notification.sendReminder" : "true",
      "recipients[0]" : "urn:altinn:organization:identifier-no:17912099997",
      "correspondence.content.messageTitle" : "foo",
      "correspondence.notification.notificationChannel" : "EmailAndSms",
      "correspondence.sendersReference" : "ff88849c-e281-4809-8555-7cd54952b922",
      "correspondence.notification.smsBody" : "$reporteeName$: Du har mottatt en melding fra TEST - C4.",
      "correspondence.sender" : "0192:910077473",
      "correspondence.requestedPublishTime" : "2019-03-25T12:38:23+01:00",
      "correspondence.content.attachments[0].isEncrypted" : "false",
      "correspondence.notification.requestedSendTime" : "2019-03-25T12:43:23+01:00",
      "correspondence.isConfirmationNeeded" : "false",
      "correspondence.isConfidential" : "false",
      "correspondence.content.messageSummary" : "bar",
      "correspondence.content.attachments[0].fileName" : "test.txt",
      "correspondence.content.language" : "nb",
      "correspondence.notification.emailContentType" : "Plain",
      "correspondence.content.messageBody" : "foobar",
      "correspondence.content.attachments[0].displayName" : "Test",
      "correspondence.notification.notificationTemplate" : "CustomMessage",
      "correspondence.messageSender" : "TEST - C4",
      "correspondence.notification.emailBody" : "$reporteeName$: Du har mottatt en melding fra TEST - C4.",
      "correspondence.dueDateTime" : "2019-04-01T12:38:23+01:00"
    }
    """
    And the sent message contains the following files:
      | filename | content type |
      | test.txt | text/plain   |
