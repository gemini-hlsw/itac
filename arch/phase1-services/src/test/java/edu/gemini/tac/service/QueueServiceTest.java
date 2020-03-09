package edu.gemini.tac.service;

import com.google.common.collect.ImmutableMap;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.JointBanding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.persistence.queues.partnerCharges.ExchangePartnerCharge;
import edu.gemini.tac.persistence.queues.partnerCharges.RolloverPartnerCharge;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class QueueServiceTest extends FastHibernateFixture.WithQueuesLoadOnce {
    private Logger LOGGER = Logger.getLogger(QueueServiceTest.class.toString());

    @Resource(name = "queueService")
    private IQueueService queueService;

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "partnerService")
    private IPartnerService partnerService;

    @Resource(name = "rolloverService")
    private IRolloverService rolloverService;

    @Test
    public void getQueue() {
        final Queue queue = queueService.getQueue(getQueue(0).getId());
        assertNotNull(queue);
        assertEquals("Test Queue 0", queue.getName());
    }

    @Test
    public void getQueueRankings() {
        final Queue queue = queueService.getQueue(getQueue(0).getId());
        assertNotNull(queue);
        final SortedSet<Banding> bandings = queue.getBandings();
        assertNotNull(bandings);
        assertEquals(48, bandings.size());
    }

    private Long getRolloverSetId(String siteName){
        Session s = sessionFactory.openSession();
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
    public void createQueue() {
        Long committeeId = getCommittee(0).getId();
        String site = "North";
        int totalTimeAvailable = 500;
        Long partnerInitalPick = 70L;
        Long conditionBinsId = 260L;
        Long binConfigurationsId = 250L;
        int band1Threshold = 0;
        int band2Threshold = 50;
        Integer band3Threshold = 100;
        Integer band3ConditionsThreshold = 70;
        Boolean band3ForAllOverThreshold = Boolean.TRUE;
        String queueName = "unit test queue " + RandomStringUtils.randomAscii(10);
        String queueNotes = "unit test notes " + RandomStringUtils.randomAscii(100);
        Long[] bandRestrictionIds = {4L, 5L};
        ImmutableMap<Long, Integer> restrictedBin = ImmutableMap.of(200L, 200);
        Map<String,Double> keckAllocations = new HashMap<String,Double>();
        Map<String,Double> subaruAllocations = new HashMap<String,Double>();
        Boolean subaruStrategyUseQueue = false;
        Long[] proposalsAccepted = {};
        Long[] classicalsAccepted = {};
        Long rolloverSetId = getRolloverSetId(site);

        final Map<String, Float> adjustments = new HashMap<String, Float>();
        final Map<String, Float> exchanges = new HashMap<String, Float>();

        final Queue queue = queueService.createQueue(committeeId, site, totalTimeAvailable, partnerInitalPick,
                conditionBinsId, binConfigurationsId, band1Threshold, band2Threshold, band3Threshold, band3ConditionsThreshold, band3ForAllOverThreshold,
                queueName, queueNotes,
                bandRestrictionIds, restrictedBin, keckAllocations, subaruAllocations, subaruStrategyUseQueue,
                proposalsAccepted, classicalsAccepted, rolloverSetId, adjustments, exchanges, 5);

        assertNotNull(queue);
        assertNotNull(queue.getId());

        final Queue readQueue = queueService.getQueue(queue.getId());
        assertEquals(queue.getId(), readQueue.getId());

    }

    private Long getExchangeInCommittee(String exchangePartner, Long committeeId){
        final List<Proposal> proposalList = committeeService.getAllProposalsForCommittee(committeeId);
        for(Proposal proposal : proposalList){
            if(proposal.isExchange() && proposal.getPartner().getPartnerCountryKey().equalsIgnoreCase(exchangePartner)){
                return proposal.getId();
            }
        }
        fail("Could not find any exchange proposals in committee");
        throw new RuntimeException();
    }

    @Test
    public void canCreateQueueWithSubaruScheduledViaQueueEngine(){


        Map<String,Double> subaruAllocations = new HashMap<String,Double>();
        subaruAllocations.put("US", 20.0);
        Boolean subaruStrategyUseQueue = true;
        Long[] exchangeProposalsAccepted = { getExchangeInCommittee("SUBARU", getCommittee(0).getId())  };

        final Map<String, Float> exchanges = new HashMap<String, Float>();
        exchanges.put("US", new Float(20.0));

        Long committeeId = getCommittee(0).getId();
        String site = "North";
        int totalTimeAvailable = 500;
        Long partnerInitalPick = 70L;
        Long conditionBinsId = 260L;
        Long binConfigurationsId = 250L;
        int band1Threshold = 0;
        int band2Threshold = 50;
        Integer band3Threshold = 100;
        Integer band3ConditionsThreshold = 70;
        Boolean band3ForAllOverThreshold = Boolean.TRUE;
        String queueName = "unit test queue " + RandomStringUtils.randomAscii(10);
        String queueNotes = "unit test notes " + RandomStringUtils.randomAscii(100);
        Long[] bandRestrictionIds = {4L, 5L};
        ImmutableMap<Long, Integer> restrictedBin = ImmutableMap.of(200L, 200);
        Map<String,Double> keckAllocations = new HashMap<String,Double>();
        Long[] classicalsAccepted = {};
               Long rolloverSetId = getRolloverSetId(site);

        final Map<String, Float> adjustments = new HashMap<String, Float>();
        final Queue queue = queueService.createQueue(committeeId, site, totalTimeAvailable, partnerInitalPick,
                conditionBinsId, binConfigurationsId, band1Threshold, band2Threshold, band3Threshold, band3ConditionsThreshold, band3ForAllOverThreshold,
                queueName, queueNotes,
                bandRestrictionIds, restrictedBin, keckAllocations, subaruAllocations, subaruStrategyUseQueue,
                exchangeProposalsAccepted, classicalsAccepted, rolloverSetId, adjustments, exchanges, 5);

        assertNotNull(queue);
        assertNotNull(queue.getId());

        final Queue readQueue = queueService.getQueue(queue.getId());
        assertEquals(queue.getId(), readQueue.getId());

        //Additional tests to make sure the queue engine was used to schedule the Subaru exchanges
        assertTrue(queue.getSubaruScheduledByQueueEngine());
        final Set<PartnerPercentage> partnerPercentages = queue.getPartnerPercentages();
        boolean usIsCorrect = false;
        boolean subaruIsCorrect = false;
        for(PartnerPercentage pp : partnerPercentages){
            if(pp.getPartner().getPartnerCountryKey().equalsIgnoreCase("US")){
                assertEquals(0.496, pp.percentage(), 0.01);
                usIsCorrect = true;
            }
            if(pp.getPartner().getPartnerCountryKey().equalsIgnoreCase("SUBARU")){
                assertEquals(0.04, pp.percentage(), 0.01);
                subaruIsCorrect = true;
            }
        }
        assertTrue(usIsCorrect);
        assertTrue(subaruIsCorrect);
    }

    @Test
    public void testSetBandingBand() {
        final Queue queue = queueService.getQueue(getQueue(0).getId());
        assertNotNull(queue);
        final SortedSet<Banding> bandings = queue.getBandings();
        Long bandingId1 = null;
        Long bandingId2 = null;
        Long bandingId3 = null;
        for (Banding b : bandings) {
            switch ((int) b.getBand().getRank()) {
                case 1:
                    bandingId1 = b.getId();
                    break;
                case 2:
                    bandingId2 = b.getId();
                    break;
                case 3:
                    bandingId3 = b.getId();
                    break;
                default: //Nothing
            }
        }
        //Confirm has all 3 bands
        assertNotNull(bandingId1);
        assertNotNull(bandingId2);
        assertNotNull(bandingId3);

        queueService.setBandingBand(bandingId1, 4);
        queueService.setBandingBand(bandingId2, 3);
        queueService.setBandingBand(bandingId3, 2);

        final Queue newQueue = queueService.getQueue(queue.getId());
        assertNotNull(newQueue);
        final SortedSet<Banding> newBandings = newQueue.getBandings();
        for (Banding b : newBandings) {
            if (b.getId() == bandingId1) {
                assertEquals(4, b.getBand().getRank());
            }
            if (b.getId() == bandingId2) {
                assertEquals(3, b.getBand().getRank());
            }
            if (b.getId() == bandingId3) {
                assertEquals(2, b.getBand().getRank());
            }
        }
    }

    @Test
    public void hasQueuesIndexPageData() {
        final Queue q = queueService.getQueue(getQueue(0).getId());

        //Fields
        Assert.assertEquals(getQueue(0).getId().longValue(), q.getId().longValue());
        Assert.assertTrue(q.getName().length() > 0);
        Assert.assertEquals(200, q.getTotalTimeAvailable().longValue());
        Assert.assertEquals(30, q.getBand1Cutoff().longValue());
        Assert.assertEquals(60, q.getBand2Cutoff().longValue());
        Assert.assertEquals(100, q.getBand3Cutoff().longValue());
        Assert.assertEquals(60, q.getBand3ConditionsThreshold().longValue());
        Assert.assertEquals(false, q.getUseBand3AfterThresholdCrossed());
        Assert.assertEquals(false, q.getFinalized());

        //Committee Data
        Assert.assertEquals("Test committee 0", q.getCommittee().getName());

        //Site Data
        Assert.assertEquals("North", q.getSite().getDisplayName());

        //Bin config data
        Assert.assertEquals("Bin Configuration 1", q.getBinConfiguration().getName());

        //Partner times
        Map m = q.getAvailablePartnerTimes();
        Assert.assertEquals(0, m.keySet().size());


        //Bandings
        Assert.assertEquals(48, q.getBandings().size());

        //Band restriction rules
        Assert.assertEquals(0, q.getBandRestrictionRules().size());
    }

    @Test
    public void canAddProposalToExistingQueue() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        final long initialBandingCount = q.getBandings().size();

        //Create a duplicate and add it
        Proposal orig = q.getBandings().first().getProposal();
        Proposal p = proposalService.duplicate(orig, new File("/tmp/"));
        //IMPORTANT: Receipt id must be different! Service checks that a proposal is not banded twice!
        p.getPhaseIProposal().getPrimary().getReceipt().setReceiptId("something-else-id");
        Banding b = new Banding(p, ScienceBand.BAND_ONE);

        queueService.addBandingToQueue(q.getId(), b);
        final Queue postQ = queueService.getQueue(q.getId());
        Assert.assertEquals(initialBandingCount + 1, postQ.getBandings().size());

        //Can not add if already banded
        Banding invalid = new Banding(p, ScienceBand.BAND_TWO);
        try {
            queueService.addBandingToQueue(q.getId(), invalid);
            Assert.fail("Execution should not reach here");
        } catch (IllegalArgumentException iae) {
            //As expected
        } catch (Throwable t) {
            Assert.fail("Unexpected exception thrown " + t);
        }
    }

    @Test
    public void canAddClassicalProposalToExistingQueue() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        final Committee committee = q.getCommittee();
        final long initialClassicalCount = q.getClassicalProposals().size();

        final Long proposalId = makeClassicalProposal(committee, getPartner(0)).getId();
        final Proposal p = proposalService.getProposal(committee.getId(), proposalId);
        queueService.addClassicalProposalToQueue(q.getId(), p);

        final Queue postQ = queueService.getQueue(q.getId());
        final long postClassicalCount = postQ.getClassicalProposals().size();
        Assert.assertEquals(initialClassicalCount + 1, postClassicalCount);
    }

    @Test
    public void canDeleteBanding() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        SortedSet<Banding> bandings = q.getBandings();
        final int originalSize = bandings.size();
        final Banding head = bandings.first();
        queueService.deleteBandingFromQueue(q.getId(), head);

        final Queue post = queueService.getQueue(q.getId());
        final int size = post.getBandings().size();
        Assert.assertEquals(originalSize - 1, size);
    }

    @Test
    public void canRearrangeBandingToBanding() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        SortedSet<Banding> bandings = q.getBandingsFor(ScienceBand.BAND_ONE);
        final int bandingsSize = q.getBandings().size();
        final Banding head = bandings.first();
        final Proposal originalProposal = head.getProposal();
        final ScienceBand originalBand = head.getBand();
        Assert.assertEquals(1, originalBand.getRank());
        queueService.setBandingBand(head.getId(), (int) ScienceBand.BAND_TWO.getRank());

        final Queue post = queueService.getQueue(q.getId());
        Assert.assertEquals(bandingsSize, post.getBandings().size());
        Banding postBanding = post.getBandingFor(originalProposal);
        Assert.assertEquals(2, postBanding.getBand().getRank());

    }

    @Test
    public void canRearrangeBandingToClassical() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        final int bandingsSize = q.getBandings().size();
        final int classicalSize = q.getClassicalProposals().size();
        final Banding originalBanding = q.getBandingsFor(ScienceBand.BAND_ONE).first();

        queueService.convertBandingToClassical(q.getId(), originalBanding.getId());

        final Queue post = queueService.getQueue(q.getId());
        Assert.assertEquals(bandingsSize - 1, post.getBandings().size());
        Assert.assertEquals(classicalSize + 1, post.getClassicalProposals().size());
        Assert.assertEquals(0, queueService.getOrphanedBandings().size());
    }

    @Test
    public void canRearrangeClassicalInQueueToBanding() {
        canRearrangeClassicalToBanding(true);
    }

    @Test
    public void canRearrangeClassicalNotInQueueToBanding() {
        canRearrangeClassicalToBanding(false);
    }

    private void canRearrangeClassicalToBanding(boolean inQueue) {
        // prepare queue
        final Long queueId = getQueue(0).getId();
        final Queue q = queueService.getQueue(queueId);
        final Proposal p = makeClassicalProposal(q.getCommittee(), getPartner(0));
        // if needed add new classical proposal to queue
        if (inQueue) {
            queueService.addClassicalProposalToQueue(queueId, p);
        }

        // reread and store before state
        final Queue before = queueService.getQueue(queueId);
        final int beforeBandingsSize = before.getBandings().size();
        final int beforeClassicalSize = before.getClassicalProposals().size();

        // do it!
        queueService.convertClassicalToBanding(queueId, p, ScienceBand.BAND_ONE);

        // reread and check post state
        final Queue after = queueService.getQueue(q.getId());
        Assert.assertEquals(beforeBandingsSize + 1, after.getBandings().size());
        if (inQueue) {
            Assert.assertEquals(beforeClassicalSize - 1, after.getClassicalProposals().size());
        } else {
            Assert.assertEquals(beforeClassicalSize, after.getClassicalProposals().size());
        }
        Assert.assertEquals(0, queueService.getOrphanedBandings().size());
        validateBandings(after);
    }

    private JointProposal makeJointProposal(Committee committee, Partner primaryPartner, Partner secondaryPartner) {
        Proposal primary = makeQueueProposal(committee, primaryPartner);
        Proposal secondary = makeQueueProposal(committee, secondaryPartner);
        primary.getPhaseIProposal().setSubmissionsKey("foo");
        secondary.getPhaseIProposal().setSubmissionsKey("foo");

        primary.getPhaseIProposal().getPrimary().getReceipt().setReceiptId("foo");
        secondary.getPhaseIProposal().getPrimary().getReceipt().setReceiptId("foo");

        JointProposal joint = new JointProposalBuilder().
                addProposal(primary).
                addProposal(secondary).
                setPrimaryProposal(primary).
                create();


        return (JointProposal) joint;
    }

    private Proposal makeQueueProposal(Committee committee, Partner partner) {
        return new QueueProposalBuilder().
                setCommittee(committee).
                setPartner(partner).
                create(getSessionFactory());
    }


    @Test
    public void canRearrangeBandingToExchange() {
        final Queue q = queueService.getQueue(getQueue(0).getId());

        // in contrast to canRearrangeBandingToClassical there is no conversion from any proposals to exchange proposals
        // therefore for this test to work we must make sure we have an exchange proposal that is banded
        Partner gs = getPartnerByCountryKey("GS");
        final Proposal exchangeProposal = makeExchangeProposal(q.getCommittee(), gs);
        final Banding originalBanding = new Banding(exchangeProposal, ScienceBand.BAND_ONE);
        queueService.addBandingToQueue(q.getId(), originalBanding);

        final Queue q0 = queueService.getQueue(getQueue(0).getId());
        final int bandingsSize = q0.getBandings().size();
        final int exchangeSize = q0.getExchangeProposals().size();

        queueService.convertBandingToExchange(q.getId(), originalBanding);

        final Queue post = queueService.getQueue(q.getId());
        Assert.assertEquals(bandingsSize - 1, post.getBandings().size());
        Assert.assertEquals(exchangeSize + 1, post.getExchangeProposals().size());
        Assert.assertEquals(0, queueService.getOrphanedBandings().size());
        validateBandings(post);
    }

    private Partner getPartnerByCountryKey(String key) {
        if (partners == null) {
            partners = partnerService.findAllPartners();
        }
        for (Partner p : partners) {
            final String partnerCountryKey = p.getPartnerCountryKey();
            if (partnerCountryKey.equalsIgnoreCase(key)) {
                return p;
            }
        }
        fail("Could not find partner with country key " + key);
        throw new RuntimeException();
    }

    @Test
    public void canRearrangeExchangeInQueueToBanding() {
        canRearrangeExchangeToBanding(true);
    }

    @Test
    public void canRearrangeExchangeNotInQueueToBanding() {
        canRearrangeExchangeToBanding(false);
    }

    private void canRearrangeExchangeToBanding(boolean inQueue) {
        // prepare queue
        final Long queueId = getQueue(0).getId();
        final Queue q = queueService.getQueue(queueId);
        final Proposal p = makeExchangeProposal(q.getCommittee(), getPartner(0));
        // if needed add new exchange proposal to queue
        if (inQueue) {
            queueService.addExchangeProposalToQueue(queueId, p);
        }

        // reread and store before state
        final Queue before = queueService.getQueue(queueId);
        final int beforeBandingsSize = before.getBandings().size();
        final int beforeExchangeSize = before.getExchangeProposals().size();

        // do it!
        queueService.convertExchangeProposalToBanding(queueId, p, ScienceBand.BAND_ONE);

        // reread and check post state
        final Queue after = queueService.getQueue(queueId);
        Assert.assertEquals(beforeBandingsSize + 1, after.getBandings().size());
        if (inQueue) {
            Assert.assertEquals(beforeExchangeSize - 1, after.getExchangeProposals().size());
        } else {
            Assert.assertEquals(beforeExchangeSize, after.getExchangeProposals().size());
        }
        Assert.assertEquals(0, queueService.getOrphanedBandings().size());
        validateBandings(after);
    }

    private void validateBandings(Queue queue) {
        for (Banding b : queue.getBandings()) {
            Assert.assertNotNull(b.getAwardedTime());
            Assert.assertNotNull(b.getBand());
            Assert.assertNotNull(b.getProposal());
        }
    }

    private Proposal makeClassicalProposal(Committee committee, Partner partner) {
        return new ClassicalProposalBuilder().
                setCommittee(committee).
                setPartner(partner).
                create(getSessionFactory());
    }

    private Proposal makeExchangeProposal(Committee committee, Partner partner) {
        return new ExchangeProposalBuilder().
                setCommittee(committee).
                setPartner(partner).
                setBlueprint(ProposalBuilder.SUBARU).
                create(getSessionFactory());
    }

    @Test
    public void canRearrangeJointToClassical() {
        final Queue q = queueService.getQueue(getQueue(0).getId());
        JointProposal jp = makeJointProposal(q.getCommittee(), getPartner("US"), getPartner("AU"));
        JointBanding b = new JointBanding(q, jp, ScienceBand.BAND_ONE);
        final int initialClassicalSize = q.getClassicalProposals().size();

        queueService.convertBandingToClassical(q.getId(), b);
        final Queue post = queueService.getQueue(q.getId());
        final SortedSet<Banding> postBandings = post.getBandings();
        Assert.assertTrue(!postBandings.contains(b));
        Assert.assertTrue(initialClassicalSize < post.getClassicalProposals().size());
    }

    @Test
    public void canCalculatePartnerPercentages(){
        Queue q = queueService.getQueue(getQueue(0).getId());
        final int rolloverHours = 10;
        Map<Partner,RolloverPartnerCharge> rolloverCharges = new HashMap<Partner,RolloverPartnerCharge>();
        final RolloverPartnerCharge rolloverPartnerCharge = new RolloverPartnerCharge(q, getPartner("US"), new TimeAmount(10.0, TimeUnit.HR));
        rolloverCharges.put(getPartner("US"), rolloverPartnerCharge);
        q.setRolloverPartnerCharges(rolloverCharges);
        final Integer timeAvailable = q.getTotalTimeAvailable();
        Assert.assertEquals(200, timeAvailable.intValue());
        final Map<Partner,ExchangePartnerCharge> exchangePartnerCharges = q.getExchangePartnerCharges();
        Assert.assertEquals(0, exchangePartnerCharges.size());
        final Double subaruTime = timeAvailable / 10.0;
        TimeAmount exchangeTime = new TimeAmount(subaruTime, TimeUnit.HR);
        exchangePartnerCharges.put(getPartner("US"), new ExchangePartnerCharge(q, getPartner("US"), exchangeTime));
        q.setExchangePartnerCharges(exchangePartnerCharges);
        partners = getPartners();
        final Set<PartnerPercentage> ppsByExchange = queueService.calculatePartnerPercentages(false, partners, q, subaruTime, rolloverHours, 5);
        validatePartnerPercentage("US", 0.54, ppsByExchange);
        validatePartnerPercentage("SUBARU", 0.0, ppsByExchange);
        validatePartnerPercentage("GS", 0.03, ppsByExchange);
        validatePartnerPercentage("LP", 0.02, ppsByExchange);
        validatePartnerPercentage("CL", 0.0, ppsByExchange);
        validatePartnerPercentage("BR", 0.05, ppsByExchange);
        validatePartnerPercentage("AR", 0.03, ppsByExchange);
        validatePartnerPercentage("CA", 0.17, ppsByExchange);
        validatePartnerPercentage("UH", 0.10, ppsByExchange);
        validatePartnerPercentage("AU", 0.06, ppsByExchange);
        validatePartnerPercentage("KECK", 0.0, ppsByExchange);
        double totalHoursConsumed = 0;
        int totalQueueHours = q.getTotalTimeAvailable() - rolloverHours;
        for(PartnerPercentage pp : ppsByExchange){
            final double partnerConsumption = pp.percentage() * totalQueueHours;
            totalHoursConsumed += partnerConsumption;
            final String message = pp.getPartner().getPartnerCountryKey() + " Nominal: " + pp.getPartner().getPercentageShare() + " Actual: " + pp.percentage() + " Consumes: " + partnerConsumption;
            LOGGER.log(Level.INFO, message);
        }
        Assert.assertEquals(totalHoursConsumed, totalQueueHours, 0.01);

        final Set<PartnerPercentage> ppsByQueue = queueService.calculatePartnerPercentages(true, partners, q, subaruTime, rolloverHours, 5);
        validatePartnerPercentage("SUBARU", 0.127, ppsByQueue);
        validatePartnerPercentage("US", 0.424, ppsByQueue);
        validatePartnerPercentage("GS", 0.03, ppsByExchange);
        validatePartnerPercentage("LP", 0.02, ppsByExchange);
        validatePartnerPercentage("CL", 0.0, ppsByExchange);
        validatePartnerPercentage("BR", 0.048, ppsByExchange);
        validatePartnerPercentage("AR", 0.029, ppsByExchange);
        validatePartnerPercentage("CA", 0.166, ppsByExchange);
        validatePartnerPercentage("UH", 0.098, ppsByExchange);
        validatePartnerPercentage("AU", 0.059, ppsByExchange);
        validatePartnerPercentage("KECK", 0.0, ppsByExchange);

         totalHoursConsumed = 0;
         totalQueueHours = q.getTotalTimeAvailable() - rolloverHours;
        for(PartnerPercentage pp : ppsByQueue){
            final double partnerConsumption = pp.percentage() * totalQueueHours;
            totalHoursConsumed += partnerConsumption;
            final String message = pp.getPartner().getPartnerCountryKey() + " Nominal: " + pp.getPartner().getPercentageShare() + " Actual: " + pp.percentage() + " Consumes: " + partnerConsumption;
            LOGGER.log(Level.ERROR, message);
        }
        Assert.assertEquals(totalHoursConsumed, totalQueueHours, 0.01);
    }

    private void validatePartnerPercentage(String key, Double pct, Set<PartnerPercentage> pps){
        for(PartnerPercentage pp : pps){
            if(pp.getPartner().getPartnerCountryKey().equals(key)){
                if(Math.abs(pct - pp.percentage()) < 0.01){
                    return;
                }else{
                    Assert.fail("Could not validate partner percentage for " + key + ". Expected: " + pct + " Got: " + pp.percentage());
                }
            }
        }
        throw new RuntimeException("Could not partner " + key);
    }
}
