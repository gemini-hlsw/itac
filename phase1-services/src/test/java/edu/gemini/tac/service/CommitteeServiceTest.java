package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssue;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.queues.Queue;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Puts specific methods of the committee service under test.
 * All tests are read only so it's safe to use a "load once" fixture.
 *
 * @author ddawson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class CommitteeServiceTest extends FastHibernateFixture.WithQueuesLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(CommitteeServiceTest.class.getName());

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Test
    @Transactional
    public void testGetAllCommittees() {
        final List<Committee> allCommittees = committeeService.getAllCommittees();
        assertNotNull(allCommittees);
        assertEquals(4, allCommittees.size());
    }

    @Test
    @Transactional
    public void testGetCommittee() {
        final Committee committee = committeeService.getCommittee(getCommittee(0).getId());
        assertNotNull(committee);
    }

    @Test
    @Transactional
    public void getAllQueues() {
        final List<Queue> queues = committeeService.getAllQueuesForCommittee(getCommittee(0).getId());
        assertNotNull(queues);
        assertEquals(1, queues.size());
        assertEquals("Test Queue 0", queues.get(0).getName());
    }

    @Test
    @Transactional
    public void getNonExistentCommittee() {
        final Committee committee = committeeService.getCommittee(-1L);
        assertNull(committee);
    }

    @Test
    @Transactional
    public void getsAllProposalsForCommittee() {
        final List<Proposal> allProposalsForCommittee = committeeService.getAllProposalsForCommittee(getCommittee(0).getId());
        Assert.assertTrue(allProposalsForCommittee.size() > 0);
        for(Proposal p : allProposalsForCommittee){
            p.getPhaseIProposal().validate();
        }
    }

    @Test
    @Transactional
    public void getsCommitteeForExchangeAnalysisHasValidProposals(){
        final Committee committee = committeeService.getCommitteeForExchangeAnalysis(getCommittee(0).getId());
        assertNotNull(committee.getExchangeTimeProposals());
    }

    @Test
    public void getsTheDataUsedForTheProposalListingsPage(){
        final List<Proposal> ps = committeeService.getAllProposalsForCommittee(getCommittee(0).getId());
        Assert.assertTrue(ps.size() > 0);
        committeeService.populateProposalsWithListingInformation(ps);
        for(Proposal p : ps){
            validateListingInformation(p);
        }
    }

    private void validateListingInformation(Proposal p){
        p.getPhaseIProposal().getPrimary().getReceipt();
        p.getPhaseIProposal().getInvestigators().getPi().getLastName();
        p.getPhaseIProposal().getItac().getAccept();
        final Set<ProposalIssue> issues = p.getIssues();
        for(ProposalIssue pi: issues){
            pi.getMessage();
        }
        final Set<Observation> observations = p.getAllObservations();
        for(Observation o : observations){
            o.getTarget();
            final BlueprintBase blueprint = o.getBlueprint();
            final String blueprintId = blueprint.getBlueprintId();
        }
    }

    @Test
    public void settingProportionalPartnerSequenceSetsSequenceValueToNull(){
        Long committeeId = committeeService.getAllActiveCommittees().get(0).getId();

        committeeService.setPartnerSequenceProportional(committeeId);
        Committee committee = committeeService.getCommittee(committeeId);
        Assert.assertNull(committee.getPartnerSequence());
    }

    @Test
    public void canSetCustomPartnerSequence(){
        Long committeeId = committeeService.getAllActiveCommittees().get(0).getId();
        Committee committee = committeeService.getCommittee(committeeId);
        PartnerSequence ps = new PartnerSequence(committee,  "foo", "US,BR,CL", true);
        committeeService.setPartnerSequence(committeeId, ps);
        Committee retrieveAgain = committeeService.getCommittee(committeeId);
        PartnerSequence retrieved = retrieveAgain.getPartnerSequence();
        Assert.assertEquals("foo", retrieved.getName());
    }


    @Test
    public void proportionalPartnerSequenceIsRetrievedAsNull(){
        Long committeeId = committeeService.getAllActiveCommittees().get(0).getId();
        committeeService.setPartnerSequenceProportional(committeeId);
        PartnerSequence partnerSequence = committeeService.getPartnerSequenceOrNull(committeeId);
        Assert.assertNull(partnerSequence);
    }

    @Test
    public void customPartnerSequenceIsRetrievable(){
        Long committeeId = committeeService.getAllActiveCommittees().get(0).getId();
        Committee committee = committeeService.getCommittee(committeeId);
        PartnerSequence ps = new PartnerSequence(committee,  "foo", "US,BR,CL", true);
        committeeService.setPartnerSequence(committeeId, ps);
        final PartnerSequence retrieved = committeeService.getPartnerSequenceOrNull(committeeId);
        Assert.assertEquals("foo", retrieved.getName());
    }
}