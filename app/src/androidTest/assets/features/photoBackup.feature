Feature: Photo Backup
  Scenario Outline: I can backup photos
    Given I have logged in as <username>
    Examples:
      | username   |
      | test-admin |