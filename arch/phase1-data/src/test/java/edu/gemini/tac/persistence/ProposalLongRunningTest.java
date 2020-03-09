package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ProposalLongRunningTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(ProposalLongRunningTest.class.getName());

    @Resource(name = "sessionFactory")
    SessionFactory sessionFactory;

    @Before
    public void before(){
        super.teardown(); //Tear down db if it exists
        super.before();
        proposals = getProposals();
    }

    @After
    public void teardown(){
        super.teardown();
    }

    @Test
    @Ignore
    public void canDeleteDuplicatedProposals(){
        Assert.assertTrue(proposals.size() > 0);
        for (Proposal proposal : proposals) {
            if (!proposal.isJointComponent()) {
               canDeleteDuplicatedProposal(proposal.getId());
            }
        }
    }
    
    private void canDeleteDuplicatedProposal(final Long proposalId){
        final Session session = sessionFactory.openSession();
        try{
            final Proposal initial = getProposalById(session, proposalId);

            LOGGER.log(Level.DEBUG, "checking " + (initial.isJoint() ? "joint" : "") + " proposal " + initial.getId());

            /// CHECK SOME WEIRD CONDITIONS
            /// WE WANT TO BE SURE ALL OBSERVATIONS IN PROPOSALS HAVE CONSTRAINTS AND RESOURCES
            final Set<Observation> observations = initial.getPhaseIProposal().getObservations();
            for(Observation o : observations){
                Assert.assertTrue(o.getCondition() != null);
                Assert.assertTrue(o.getBlueprint() != null);
                Assert.assertTrue(o.getBlueprint().getInstrument() != null);
            }
            /////
            
            //If it has an ITAC, grab it
            Itac oldItac = null;
            String oldSubmissionKey = null;
            try{
                final List<Submission> submissions = initial.getPhaseIProposal().getSubmissions();
                oldSubmissionKey = initial.getPhaseIProposal().getSubmissionsKey();
                oldItac  = initial.getItac();
            }catch(Throwable t){} //Some null along the way
                
            Assert.assertNotNull(initial);
            Assert.assertNotNull(initial.getPhaseIProposal());
            Assert.assertNotNull(initial.getPhaseIProposal().getMeta());
           
            final Proposal dup = initial.duplicate();
            Assert.assertNotNull(dup);
            Assert.assertNotNull(dup.getPhaseIProposal());
            //Ensure object distinctness
            Assert.assertNotSame(initial, dup);
            Assert.assertNotSame(initial.getPhaseIProposal(), dup.getPhaseIProposal());
            
            //But the Partner and Committee will be the same
            Assert.assertSame(initial.getCommittee(), dup.getCommittee());
            Assert.assertSame(initial.getPartner(), dup.getPartner());

            //Confirm special aspects of copy logic

            //New submission key
            final String newKey = dup.getPhaseIProposal().getSubmissionsKey();
            Assert.assertNotSame(newKey, oldSubmissionKey);
            //New ITAC
            final Itac itac = dup.getItac();
            Assert.assertNotNull(itac);
            Assert.assertNotSame(itac, oldItac);

            //Now delete the duplicate
            session.flush();
            session.delete(dup);
            session.flush();

            //Make sure the initial is still okay
            Assert.assertNotNull(initial.getPhaseIProposal());
        }finally{
            session.close();
        }
    }
}
