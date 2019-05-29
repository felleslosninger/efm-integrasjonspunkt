Feature: Webhooks

  Background:
    Given the endpoint "/my/test/endpoint" accepts posts
    And I create the following webhook subscription:
    """
    {
      "pushEndpoint": "http://127.0.0.1:9800/my/test/endpoint"
    }
    """
    Then the following ping message is posted to "/my/test/endpoint":
    """
    {
      "createdTs" : "2019-03-25T11:38:23Z",
      "type" : "ping"
    }
    """

  Scenario: As a user I want to register a webhook and receive pushes to it
    Given the following message status is published:
      """
      {
        "status" : "MOTTATT",
        "description" : "some description"
      }
      """
    Then the following message is posted to "/my/test/endpoint":
      """
       {
        "createdTs" : "2019-03-25T11:38:23Z",
        "type" : "message.status",
        "status" : "MOTTATT",
        "description" : "some description"
      }
      """