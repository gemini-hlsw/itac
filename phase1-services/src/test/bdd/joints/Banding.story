Feature: Banding of Joint Proposals
	In order to prioritize accepted joint proposals
	As the ITAC secretary
	I want queue generation to place accepted JPs in the same band

/*

One thing I didn't understand in the scenarios below is the idea that one component can have different band constraints than another.  In the model we're talking about, all the components have the same observations and the same constraints as the master.

I struggled mightily with joint proposals because of a few difficult requirements.  Band restrictions + Joint Proposals is the single most difficult/sketchy thing about the entire algorithm.  In fact, I'd say it's the only difficult thing.  The rest is just bookkeeping.

There are band restrictions as you mention below and there is the fact that the components are considered individually.  The problem that introduces is that it can move proposals around in the queue.  The published requirement is that you wait until the end of the algorithm when all proposals have been selected or rejected and then you calculate a single weighted average of the position in the queue of each part of a joint.  It then merges the parts and puts the joint proposal at that weighted index location in the queue.  That obviously shuffles proposals (both joint and normal) around in the queue which can cause them to rise and fall and consequently violate band restrictions that they didn't violate at the time they were accepted.  I call that method of waiting around until the end to do the merge the "lazy merge strategy".

When I brought this up, Sandy and Rosemary said they are aware that band restrictions + joints can cause problems but they are okay with it.  Still I thought it might be a slight improvement (if yielding somewhat different results) to eagerly merge joint components in order to have a better view of where things ultimately are going to fall as the algorithm progresses and have fewer surprises at the end.  So I made an "eager merge strategy" that I use by default but can switch for the lazy merge strategy if need-be.  In fact, we could even run the thing twice and calculate the queue quality metrics on both versions and pick the one that wins.

Either way, at the end in the last step of the queue generation, a proposal (joint or otherwise) that got moved into a band that causes a constraint violation is completely rejected.  It spoils the whole JP as you say and worse it leaves an unallocated hole in the queue time.  On the other hand, if the queue algorithm can see that adding component Cn will cause JP to slide down to an unacceptable band during the execution of the queue algorithm, then it rejects Cn and keeps whatever remaining parts have already made it in.
 */

	Scenario: Single-Band JP
		Given a schedule-able joint proposal
		And each component proposal has the same band constraints
		When queuing algorithm accepts JP
		Then the joint proposal is placed in the same band

	Scenario: Multi-Band JP
		Given a joint proposal
		And the minimum band of one component is Band 1
		And the minimum band of another component is Band 2
		And there is sufficient time in Band 1
		When queuing algorithm accepts JP
		Then the joint proposal is placed solely in Band 1

	Scenario: Demanding component can spoil the whole JP
		Given a joint proposal
		And the minimum band of one component is Band 1
		And the minimum band of another component is Band 2
		And there is insufficient time in Band 1
		When queuing algorithm is run
		Then the joint proposal is not scheduled in any band





