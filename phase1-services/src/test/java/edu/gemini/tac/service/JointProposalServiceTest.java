package edu.gemini.tac.service;

import com.google.common.collect.ImmutableMap;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.CoInvestigator;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.JointBanding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * Test the functionality of the JointProposalService
 * <p/>
 * Author: lobrien
 * Date: 1/31/11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class JointProposalServiceTest extends FastHibernateFixture.WithProposals {
    private static final Logger LOGGER = Logger.getLogger(JointProposalServiceTest.class.getName());

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Resource(name = "jointProposalService")
    private IJointProposalService jointProposalService;

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "queueService")
    private IQueueService queueService;

    @Resource(name = "rolloverService")
    private IRolloverService rolloverService;

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void canMergeProposals() {
        LOGGER.log(Level.DEBUG, "jointWithManyProposals");

        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");
        Proposal us = getQueueProposalFromPartner("US");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        Assert.assertNotNull(merged);
        merged = jointProposalService.add(merged, us);

        Assert.assertEquals(3, merged.getProposals().size());
    }

    /**
     * Test is materially differnt because now we do normalize investigators.
     */

    @Test
    public void mergesDoStealCois() {
        LOGGER.log(Level.DEBUG, "mergesDoStealCois");

        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("CA");
        Proposal us = getQueueProposalFromPartner("US");

        final Set<CoInvestigator> arCois = ar.getPhaseIProposal().getInvestigators().getCoi();
        final Set<CoInvestigator> auCois = au.getPhaseIProposal().getInvestigators().getCoi();
        final Set<CoInvestigator> usCois = us.getPhaseIProposal().getInvestigators().getCoi();

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        merged = jointProposalService.add(merged, us);

        Set<CoInvestigator> p0CoisAfter = ar.getPhaseIProposal().getInvestigators().getCoi();
        Assert.assertEquals(arCois.size(), p0CoisAfter.size());
        Set<CoInvestigator> p1CoisAfter = au.getPhaseIProposal().getInvestigators().getCoi();
        Assert.assertEquals(auCois.size(), p1CoisAfter.size());
        Set<CoInvestigator> p2CoisAfter = us.getPhaseIProposal().getInvestigators().getCoi();
        Assert.assertEquals(auCois.size(), p2CoisAfter.size());

        merged = (JointProposal) proposalService.getProposal(merged.getCommittee().getId(), merged.getId());

        Set<CoInvestigator> mergedCois = merged.getPhaseIProposal().getInvestigators().getCoi();

        Assert.assertEquals(2, mergedCois.size());
        assertTrue(mergedCois.contains(p0CoisAfter.iterator().next()));
        assertTrue(mergedCois.contains(p1CoisAfter.iterator().next()));
        assertTrue(mergedCois.contains(p2CoisAfter.iterator().next()));
    }

    private void noneTheSame(Set<CoInvestigator> s1, Set<CoInvestigator> s2) {
        Set<CoInvestigator> smaller = s1.size() < s2.size() ? s1 : s2;
        Set<CoInvestigator> larger = (smaller == s1) ? s2 : s1;
        for (CoInvestigator m : larger) {
            Assert.assertFalse("CoInvestigator " + m + " duplicated between " + larger + " / " + smaller, smaller.contains(m));
        }
    }

    @Test
    public void canDeleteMergedProposal() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");
        Proposal us = getQueueProposalFromPartner("US");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        merged = jointProposalService.add(merged, us);
        long mergedId = merged.getId();
        proposalService.saveProposal(merged);

        final Long committeeId = merged.getCommittee().getId();
        Proposal existsNow = proposalService.getProposal(committeeId, mergedId);
        Assert.assertNotNull(existsNow);

        jointProposalService.delete(merged);
        Proposal oughtNotToExist = proposalService.getProposal(committeeId, mergedId);
        Assert.assertNull(oughtNotToExist);
        Assert.assertNull(proposalService.getProposal(committeeId, ar.getId()));
        Assert.assertNull(proposalService.getProposal(committeeId, au.getId()));
    }

    public Long getRolloverSetId(String siteName){
        org.hibernate.Session s = sessionFactory.openSession();
        Site site = null;
        try {
            site = (Site) (s.getNamedQuery("site.findSiteByDisplayName").setString("displayName", siteName).uniqueResult());
            s.flush();
        } finally {
            s.close();
        }
        final RolloverSet test = rolloverService.createRolloverSet(site, "Test", new String[]{}, new String[]{});
        return test.getId();
    }

    @Test
    public void canDuplicateWithNewMaster() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");
        Proposal us = getQueueProposalFromPartner("US");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        merged = jointProposalService.add(merged, us);

        long mergedId = merged.getId();
        merged = (JointProposal) proposalService.getProposal(merged.getCommittee().getId(), merged.getId());
        List<BlueprintBase> rlOrig = merged.getPhaseIProposal().getBlueprints();
        Assert.assertNotNull(rlOrig);

        JointProposal newMerged = jointProposalService.duplicateWithNewMaster(merged, au);
        long newMergedId = newMerged.getId();
        Assert.assertNotSame(mergedId, newMergedId);

        Assert.assertNotSame(merged.getPhaseIProposal().getInvestigators().getPi().getLastName(),
                newMerged.getPhaseIProposal().getInvestigators().getPi().getLastName());

        List<BlueprintBase> rlDup = newMerged.getPhaseIProposal().getBlueprints();
        Assert.assertNotNull(rlDup);
        Assert.assertEquals(merged.getSite().equals(Site.NORTH), newMerged.getSite().equals(Site.NORTH));
        Assert.assertEquals(merged.getSite().equals(Site.SOUTH), newMerged.getSite().equals(Site.SOUTH));
    }

    @Test
    public void canRemoveProposalFromJoint() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");
        Proposal us = getQueueProposalFromPartner("US");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        merged = jointProposalService.add(merged, us);

        Proposal complete = proposalService.getProposal(merged.getCommittee().getId(), merged.getId());
        Assert.assertEquals(3, complete.getProposals().size());
        Proposal lesser = jointProposalService.recreateWithoutComponent(merged.getCommittee().getId(), merged.getId(), au.getId());
        Assert.assertEquals(2, lesser.getProposals().size());
    }

    @Test
    public void canRemoveProposalAndMakeAJointAStraightProposal() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        long mergedId = merged.getId();

        Proposal complete = proposalService.getProposal(merged.getCommittee().getId(), mergedId);
        Assert.assertEquals(2, complete.getProposals().size());
        Proposal lesser = jointProposalService.recreateWithoutComponent(merged.getCommittee().getId(), mergedId, au.getId());
        Assert.assertFalse(lesser instanceof JointProposal);

        Proposal shouldBeNull = proposalService.getProposal(10L, mergedId);
        Assert.assertNull(shouldBeNull);
    }


    @Test
    public void cleanupAfterRemoveProposalAndMakeAJointAStraightProposal() {
        // Test for bug REL-1037, ITAC-643
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        long mergedId = merged.getId();

        final Long committeeId = merged.getCommittee().getId();

        String site = "North";

        // Create queue
        final Queue queue = queueService.createQueue(committeeId, site, 500, 70L,
                260L, 250L, 0, 50, 100, 70, Boolean.TRUE,
                "unit test queue " + RandomStringUtils.randomAscii(10), "unit test notes " + RandomStringUtils.randomAscii(100),
                new Long[]{4L, 5L}, ImmutableMap.of(200L, 200), new HashMap<String,Double>(), new HashMap<String,Double>(), false,
                new Long[]{}, new Long[]{}, getRolloverSetId(site), new HashMap<String, Float>(), new HashMap<String, Float>(), 5);

        Queue readQueue = queueService.getQueue(queue.getId());

        // Add banding
        Banding regularBanding = new Banding(readQueue, au  , ScienceBand.BAND_ONE);
        readQueue.addBanding(regularBanding);

        Set<Banding> componentBandings = new TreeSet<Banding>();
        componentBandings.add(regularBanding);

        // Add joint banding
        JointBanding jointBanding = new JointBanding(readQueue, merged, ScienceBand.BAND_ONE);
        jointBanding.setBandings(componentBandings);
        readQueue.addBanding(jointBanding);
        queueService.saveQueue(readQueue);

        // Read the queue
        readQueue = queueService.getQueue(queue.getId());
        // Verify the banding is there
        Assert.assertEquals(2, readQueue.getBandings().size());
        List<Long> originalBandings = new ArrayList<Long>();
        for (Banding b:readQueue.getBandings()) {
            originalBandings.add(b.getId());
        }

        jointProposalService.recreateWithoutComponent(merged.getCommittee().getId(), mergedId, au.getId());

        readQueue = queueService.getQueue(queue.getId());
        // Verify the bandings are gone
        for (Banding b:readQueue.getBandings()) {
            Assert.assertFalse(originalBandings.contains(b.getId()));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotRemoveMasterFromJointProposal() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        long mergedId = merged.getId();

        final Long committeeId = merged.getCommittee().getId();
        Proposal complete = proposalService.getProposal(committeeId, mergedId);
        Assert.assertEquals(2, complete.getProposals().size());
        jointProposalService.recreateWithoutComponent(committeeId, mergedId, ar.getId());
    }

    @Test
    public void newMasterDoesNotLeadToDuplicateComponentProposals() {
        Proposal ar = getQueueProposalFromPartner("AR");
        Proposal au = getQueueProposalFromPartner("AU");
        Proposal us = getQueueProposalFromPartner("US");

        JointProposal merged = jointProposalService.mergeProposals(ar, au);
        final Set<Proposal> originalProposals = merged.getProposals();
        merged = jointProposalService.add(merged, us);
        JointProposal newMerged = jointProposalService.duplicateWithNewMaster(merged, au);

        //Confirm the originals are gone
        final Session session = sessionFactory.openSession();
        for (Proposal p : originalProposals) {
            Query q = session.createQuery("from Proposal p where p.id = " + p.getId());
            Object shouldBeNull = q.uniqueResult();
            Assert.assertNull(shouldBeNull);
        }
    }

    protected Proposal getQueueProposalFromPartner(final String partnerAbbreviation) {
        final Session session = sessionFactory.openSession();
        final List<Proposal> ars = session.createQuery("from Proposal p join fetch p.committee join fetch p.phaseIProposal p1p where p.partner.abbreviation = '" + partnerAbbreviation + "'").list();

        Proposal partnerProposal = null;

        for (Proposal p : ars) {
            p = proposalService.getProposal(p.getCommittee().getId(), p.getId());
            if (!p.isExchange() && !p.isClassical()) {
                partnerProposal = p;
                break;
            }
        }

        session.close();
        Validate.notNull(partnerProposal);

        return partnerProposal;
    }
}