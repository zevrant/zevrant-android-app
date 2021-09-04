Feature: Photo Backup
Scenario Outline: I can backup photos
  Given I start the application
  And I am logged in as <username>
  And I add a photo to storage
  And I verify the photo was added
  Then I run the photo backup service
  And I verify the photo was backed up
  Examples:
    | username   |
    | test-admin |