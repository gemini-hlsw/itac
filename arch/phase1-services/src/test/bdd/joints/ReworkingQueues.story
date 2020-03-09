Feature: Reworking queues
    In order to improve the semester queue
    As a committee member
    I want to be able to explore "what if" scenarios

    Scenario: Force schedule
      Given a queue
      And given an unscheduled proposal P1
      And given a proposal P2 that is scheduled in Band 2
      And I ask 'what if p1 were scheduled in Band 1?'
      And I ask 'what if p2 were in Band 3?'
      When queuing algorithm is run
      Then proposal P1 is placed in Band 1
      And its time and resources are reserved / removed from queue algorithm consideration
      And P2 is placed in Band 3
      And its time and resources are reserved
      And the queue algorithm runs normally

    /*
    if they see a partially accepted joint, which we should make standout somehow, then they might be compelled to rejigger things to get it all the way accepted or they might just let it be.
     */
    Scenario: Tweaking time
      Given a queue
      And a partially-scheduled proposal
      And I wish to see if it can be fully-scheduled
      And I reduce its time requirements sufficiently
      When queuing algorithm is run
      Then the joint proposal is fully scheduled


    Scenario: Tweaking resources
      Given a queue
      And a partially-scheduled proposal
      And I wish to see if it can be fully-scheduled
      And I reduce its resources sufficiently
      When queueing algorithm is run
      Then the joint proposal is fully scheduled





Scenario: Reassign the current master joint proposal component
    Given a joint proposal with a master and a non-master component C
    And component C  should be used as the master
    When I click 'Use as Master' on the component C
    Then the joint proposal's justification, target list, etc switches to match that of component C