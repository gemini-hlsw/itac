

Feature: Splitting Proposal
	In order to allow flexibility
	As the ITAC secretary
	I want to be able to remove a component proposal from a joint proposal

	Scenario: Non-master proposal
		Given a joint proposal (JP1)
		And its component proposals (C1, C2, ... )
		And C2 is not the master proposal
		When I use the Web interface to remove C2 from the joint proposal
		Then JP1 continues to exist
		And no further action is needed to run the queue algorithm

	Scenario: Master proposal
		Given a joint proposal (JP1)
		And its component proposals (C1, C2, ... )
		And C1 is the master proposal
		When I use the Web interface to remove C1 from the joint proposal
		Then JP1 is no longer available
		And I will need to take further steps (e.g., re-import or force merge) to recreate joint proposal
/*

Here I would have guessed differently.  I would say if you've got only 2 components (C1 and C2) it would work that way.  Otherwise, if you remove C1 (and C1 is the master), then we use whatever algorithm we use to select a new default master for JP1 among C2, C3, ...  I'm just guessing though and perhaps what is written above is what they would prefer.

I'm making stuff up here but perhaps ideally there would be an option to detach any single component (including the master) or else to completely split up the joint into individual components as described above.
*/
