Feature: Receiving an invalid DPO file

  Scenario: As a user I want an empty file to be discarded instead of being downloaded on every poll

    Given Altinn sends an empty file
    And the application checks for new DPO messages
    Then the file is confirmed downloaded from Altinn

  Scenario: As a user I want a file that is not a zip file to be discarded instead of being downloaded on every poll

    Given Altinn sends a file that is not a zip file
    And the application checks for new DPO messages
    Then the file is confirmed downloaded from Altinn
