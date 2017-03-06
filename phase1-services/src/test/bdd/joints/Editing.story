Feature: Editing of Joint Proposals
	In order to improve proposals
	As someone with sufficient rights
	I want to be able to edit joint proposals in the same way as an individual proposal

	Scenario: Basic expectations
		Given a joint proposal
		And its master proposal has a mistake in, say, its scientific justification
		When I click 'Edit' on the joint proposal
		And edit the scientific justification
		Then the joint proposal's scientific justification is changed

    Scenario: Export as monolithic, edit at component level
        Given a monolithic 'Joint Proposal' XML
        And my original "component" XMLs
        When I "fine tune" the proposal
        Then I edit the component proposals (particular the 'master' proposal)
        And cut-and-paste Web-edited data from the exported JP XML
        And import the component proposals
        And get a new joint proposal

    Scenario: Default Master Selection
		Given a joint proposal with components C1, C2, ...
		When the components are imported
		(Or when a new joint proposal is created from independent proposals)
		Then a master component is selected from among the components
		Where the selected master is the one awarded the most time
		(And ties are broken by  the partner with the smallest percentage share?)
    /*
Not sure about this.  Maybe it should just be the component with the greatest time relative to partner percentage.  The US awarding 1 hour is not as big as Argentina awarding 1 hour.
     */

	Scenario: Re-import over-writes existing joint proposal
		Given a joint proposal
		And its externally edited component XMLs
		When I import them
		Then the existing joint proposal is replaced
		And the joint proposal reflects the external edits

	Scenario: Re-import does not merge Web vs XML edits
		Given a joint proposal
		And I exported at Time 1
		And I used the Web interface to further edit it at Time 2
		And I edited the exported XML
		And Time 1 < Time 2
		When I reimport the XML
		Then the joint proposal reflects the external edits
		And it does not reflect the Web edits made at Time 2

