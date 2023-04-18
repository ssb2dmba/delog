Feature: Onboarding
    As a user, I want to be able to join the network

  Scenario: Generate random identifier
    When I open application
    Then I open drawer
    Then I click 'identities'
    Then I click button 'new_identifier'
    Then "load new identity" text is presented
    Then I click 'Generate random identity'
    Then I submit webview
    Then "save" text is presented
    Then I fill input with testTag "alias" with value "test identifier"
    Then I click 'save'
    Then I open drawer
    Then I click 'identities'
    Then "test identifier" text is presented



