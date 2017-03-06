/*
	Probably doesn't need user review -- basics
*/
Feature: Basic JP queuing
	In order to schedule joint proposals
	as the ITAC Secretary
	I want queue generation to accept, reject, or partially accept joint proposals

	Scenario: Important JP, successfully scheduled
		Given a joint proposal
		And ranking of each component proposal is high enough
		And there is sufficient time in the semester queue
		When queuing algorithm is run
		Then the joint proposal is fully scheduled

	Scenario: Unimportant JP, rejected
		Given a joint proposal
		And ranking of each component proposal is low enough
		And there is insufficient time in the semester queue
		When queuing algorithm is run
		Then the joint proposal is not scheduled

	//Is this right?
	Scenario: Partially scheduled, time constraint
		Given a joint proposal
		And the ranking of one component proposal is high enough
		And the ranking of another component proposal is low enough
		And the semester queue is constrained
		When queuing algorithm is run
		Then the joint proposal is partially scheduled

    /*
    A joint proposal can be partially accepted because we run out of time for a particular part or because the addition of the part will likely make the combined joint violate a band restriction.
     */
    Scenario: Partially scheduled, component proposal constraint
        Given a joint proposal
        And it has a component proposal from Country A
        And that component proposal's acceptance would over-schedule Country A
        When queuing algorithm is run
        Then the joint proposal is partially scheduled
        And the component from Country A is not scheduled

    Scenario: Partially scheduled, component proposal band violation
        Given a joint proposal
        And it has a component proposal with a band requirement
        And that component proposal's acceptance would cause a band violation
        Then the join proposal is partially scheduled
        And that component proposal is not scheduled

    Scenario: Partially-joined UI
        Given a partially-scheduled joint proposal
        When I view the queue list
        Then the partially-scheduled proposal should stand out


    /*
    A joint proposal component could be quite highly ranked, even ranked #1 for its country and still fail to be scheduled if it needs a resource that is scarce and already reserved for some other highly ranked proposal(s)
     */
    Scenario: Important JP, but unscheduled
        Given a joint proposal (JP)
        And it is from Country A
        And it has its countries highest ranking
        And it needs a scarce resource
        And there is another proposal (P)
        And it is from country B
        And //How express constraint that causes P to be picked prior to JP?
        When queuing algorithm is run
        Then P is scheduled
        And JP is not scheduled

    Scenario: Invisible target
        Given a proposal
        And there is available time
        And the proposal requires a target that is not visible in the semester
        When queuing algorithm is run
        Then the proposal is not scheduled