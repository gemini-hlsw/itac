package edu.gemini.tac.qservice;

import com.google.common.collect.ImmutableMap;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.partnerCharges.*;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import edu.gemini.tac.service.*;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A set of integration tests for the queue engine based on a database dump containing a partial set of the 2012B data.
 * All values here are taken from the GUI assuming that the queue creation at the point these tests have been
 * written was running correctly (Larry and Sandy spent quite some time making sure that the data makes sense).
 * The tests are therefore not so much aiming at checking that the actual queue algorithm is correct but at
 * detecting unwanted changes introduced by refactorings, bug fixes etc.
 * <p/>
 * NOTE: The outcome of the queue engine runs depends heavily on a lot of settings in the database (like
 * partner shares, bin configurations etc). If this data in base.xml is changed the tests here will break!!
 * <p/>
 * NOTE2: Marking this test as "LongRunningTest" in the name will keep it from being executed with the default
 * maven settings. Use mvn test -Pdevelopment,longRunningTest to run this particular test (and possibly other
 * long running tests) locally or mvn test -Pdevelopment,allTest to run all (short and long running) tests.
 * The profile allTest is also the default for the build server.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class QueueCreationLongRunningTest extends FastHibernateFixture.With2012BProposalsLoadOnce {

    // Some hard coded ids, need to change these in case base.xml is changed!!
    private static final long CONDITION_SET_SEMESTER_A_ID = 260;
    private static final long CONDITION_SET_SEMESTER_B_ID = 261;
    // --
    private static final long BIN_CONFIGURATION_1H10D = 257;
    private static final long BIN_CONFIGURATION_1H20D = 256;
    private static final long BIN_CONFIGURATION_1H30D = 255;
    private static final long BIN_CONFIGURATION_2H10D = 260;
    private static final long BIN_CONFIGURATION_2H20D = 259;
    private static final long BIN_CONFIGURATION_2H30D = 258;
    // --
    private static final long RESTRICTED_BIN_WATER_VAPOR = 200;
    private static final long RESTRICTED_BIN_LGS_OBSERVATIONS = 201;


    private Committee committee;
    private List<Proposal> proposals;
    private Set<Long> classicalNorthIds;
    private Set<Long> classicalSouthIds;
    private Set<Long> northExchangeIds;
    private Set<Long> southExchangeIds;
    private Partner AR;
    private Partner AU;
    private Partner BR;
    private Partner CA;
    private Partner CL;
    private Partner GS;
    private Partner UH;
    private Partner US;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Resource(name = "queueService")
    private IQueueService queueService;

    @Resource(name = "partnerService")
    private IPartnerService partnerService;

    @Resource(name = "logService")
    private AbstractLogService logService;

    @Resource(name="rolloverService")
    private IRolloverService rolloverService;

    @Before
    public void before() {
        // setup fixture (load dump)
        super.before();
        // prepare values
        prepareValues();
    }

    /**
     * Creates a basic queue for Gemini North without exchanges and 600 hours.
     */
    @Test
    public void createBasicQueueNorthWith600Hrs() {
        final int totalTimeAvailable = 600;
        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "North",                                    // the site
                totalTimeAvailable,                                        // totalTimeAvailable
                US.getId(),                                 // partnerInitalPick id     (US)
                CONDITION_SET_SEMESTER_A_ID,                // conditionBins id         (Semester A)
                BIN_CONFIGURATION_2H30D,                    // binConfigurations id     (2h30)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createBasicQueueWith600Hrs",               // queueName,
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                new HashMap<String, Double>(),  // subaruExchangeParticipants - all chargeable partners
                false,                                      // schedule Subaru in Band1
                null, //new Long[0],                        // exchangeProposalsAccepted,
                getAllClassicalIds(),                       // classicalProposalsAccepted,
                getRolloverSetId("North"), //0l,                                 // rolloverId,
                getEmptyAdjustmentsMap(),                   // adjustments per partner
                getEmptyExchangesMap(),                     // exchanges per partner
                5);                                         // queueOverfillLimit %

        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        final int size = queue.getBandings().size();
        assertTrue(size > 25); //All that can be said since non-deterministic with tied rankings ; select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalNorthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from north should be queued
        assertEquals(0, queue.getExchangeProposals().size());   // no exchanges

        // ============ CHECK TIMES =============

        // ---- available hours

        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(0, getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(.01 * totalTimeAvailable * UH.getPercentageShare(), getAvailableHours(queue, UH), 0.5);
//        assertEquals(114, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        for (ExchangePartnerCharge c : queue.getExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(0, getClassicalHours(queue, CL), 0.5);
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0 , getClassicalHours(queue, CA), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(110, getClassicalHours(queue, UK), 0.5);
        assertEquals(110, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        for (AdjustmentPartnerCharge c : queue.getAdjustmentPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- partner exchange hours
        for (PartnerExchangePartnerCharge c : queue.getPartnerExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
    }


    /**
     * Creates a basic queue for Gemini North without exchanges and 1200 hours.
     */
    @Test
    public void createBasicQueueNorthWith1200Hrs() {
        final int totalTimeAvailable = 1200;
        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "North",                                    // the site
                totalTimeAvailable,                                       // totalTimeAvailable
                US.getId(),                                 // partnerInitalPick id     (US)
                CONDITION_SET_SEMESTER_A_ID,                // conditionBins id         (Semester A)
                BIN_CONFIGURATION_2H30D,                    // binConfigurations id     (2h30)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createBasicQueueWith1200Hrs",              // queueName (must be unique!)
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                new HashMap<String, Double>(),  // subaruExchangeParticipants - all chargeable partners
                false,                                      // schedule Subaru in Band1
                null, //new Long[0],                        // exchangeProposalsAccepted,
                getAllClassicalIds(),                       // classicalProposalsAccepted,
                getRolloverSetId("North"), //0l,                                 // rolloverId,
                getEmptyAdjustmentsMap(),                   // adjustments per partner
                getEmptyExchangesMap(),                     // exchanges per partner
                5);                                         // queueOverfillLimit %

        Validate.noNullElements(queue.getCommittee().getShutdowns());
        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        assertTrue(queue.getBandings().size() >= 40);            // select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalNorthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from north should be queued
        assertEquals(0, queue.getExchangeProposals().size());   // no exchanges

        // ============ CHECK TIMES =============

        // ---- available hours
        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(0, getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(.01 * totalTimeAvailable * UH.getPercentageShare(), getAvailableHours(queue, UH), 0.5);
//        assertEquals(228, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        for (ExchangePartnerCharge c : queue.getExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(0, getClassicalHours(queue, CL), 0.5);
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0, getClassicalHours(queue, CA), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(110, getClassicalHours(queue, UK), 0.5);
        assertEquals(110, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        for (AdjustmentPartnerCharge c : queue.getAdjustmentPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- partner exchange hours
        for (PartnerExchangePartnerCharge c : queue.getPartnerExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
    }

    private Long[] getExchangeProposals(int count, Site site) {
        Set<Long> activeSet = site == Site.NORTH ? northExchangeIds : southExchangeIds;
        if (activeSet.size() < count) {
            throw new RuntimeException("There are only " + activeSet.size() + " Exchanges for " + site);
        }
        Long[] ids = new Long[count];
        int n = 0;
        for (Long id : activeSet) {
            ids[n++] = id;
            if (n == count) {
                break;
            }
        }
        return ids;
    }

    private Long getExchangeProposal(int i) {
        int n = 0;
        for (Proposal p : proposals) {
            if (p.isExchange()) {
                if (++n == i) {
                    return p.getId();
                }
            }
        }
        throw new RuntimeException("There are fewer than " + i + " exchange proposals");
    }

    /**
     * Creates a basic queue North without exchanges and 1800 hours.
     */
    @Ignore
    @Test
    public void createBasicQueueWith1800Hrs() {
        final int totalTimeAvailable = 1800;
        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "North",                                    // the site
                totalTimeAvailable,                                       // totalTimeAvailable
                US.getId(),                                 // partnerInitalPick id     (US)
                CONDITION_SET_SEMESTER_A_ID,                // conditionBins id         (Semester A)
                BIN_CONFIGURATION_2H30D,                    // binConfigurations id     (2h30)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createBasicQueueWith1800Hrs",              // queueName (must be unique),
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                new HashMap<String, Double>(),  // subaruExchangeParticipants - all chargeable partners
                false,                                      // schedule Subaru in Band1
                null, //new Long[0],                        // exchangeProposalsAccepted,
                getAllClassicalIds(),                       // classicalProposalsAccepted,
                getRolloverSetId("North"),                                 // rolloverId,
                getEmptyAdjustmentsMap(),                   // adjustments per partner
                getEmptyExchangesMap(),                     // exchanges per partner
                5);                                         // queueOverfillLimit %

        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        assertTrue(queue.getBandings().size() >= 49 && queue.getBandings().size() <= 50);            // select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalNorthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from north should be queued
        assertEquals(0, queue.getExchangeProposals().size());   // no exchanges

        // ============ CHECK TIMES =============

        // ---- available hours
        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(0, getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(.01 * totalTimeAvailable * UH.getPercentageShare(), getAvailableHours(queue, UH), 0.5);
//        assertEquals(342, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        for (ExchangePartnerCharge c : queue.getExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(0, getClassicalHours(queue, CL), 0.5);
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0, getClassicalHours(queue, CA), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(110, getClassicalHours(queue, UK), 0.5);
        assertEquals(110, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        for (AdjustmentPartnerCharge c : queue.getAdjustmentPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- partner exchange hours
        for (PartnerExchangePartnerCharge c : queue.getPartnerExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
    }

    /**
     * Creates a basic queue for Gemini South without exchanges and 1200 hours.
     *
     * IGNORE FOR NOW
     */
    @Ignore
    @Test
    public void createBasicQueueSouthWith1200Hrs() {
        final int totalTimeAvailable = 1200;
        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "South",                                    // the site
                totalTimeAvailable,                                       // totalTimeAvailable
                US.getId(),                                 // partnerInitalPick id     (UK)
                CONDITION_SET_SEMESTER_A_ID,                // conditionBins id         (Semester A)
                BIN_CONFIGURATION_2H10D,                    // binConfigurations id     (2h10)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createBasicQueueSouthWith1200Hrs",         // queueName (must be unique!)
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                new HashMap<String, Double>(),  // subaruExchangeParticipants - all chargeable partners
                false,                                      // schedule Subaru in Band1

                null, //new Long[0],                        // exchangeProposalsAccepted,
                getAllClassicalIds(),                       // classicalProposalsAccepted,
                getRolloverSetId("South"),                                 // rolloverId,
                getEmptyAdjustmentsMap(),                   // adjustments per partner
                getEmptyExchangesMap(),                     // exchanges per partner
                5);                                         // queueOverfillLimit %

        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        assertTrue(queue.getBandings().size() > 30);            // select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalSouthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from south should be queued
        assertEquals(0, queue.getExchangeProposals().size());   // no exchanges

        // ============ CHECK TIMES =============

        // ---- available hours
        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(.01 * totalTimeAvailable * CL.getPercentageShare(), getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(0, getAvailableHours(queue, UH), 0.5);
//        assertEquals(228, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        for (ExchangePartnerCharge c : queue.getExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(10, getClassicalHours(queue, CL), 0.5);
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0, getClassicalHours(queue, CA), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(9, getClassicalHours(queue, UK), 0.5);
        assertEquals(70, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        for (AdjustmentPartnerCharge c : queue.getAdjustmentPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- partner exchange hours
        for (PartnerExchangePartnerCharge c : queue.getPartnerExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
    }

    /**
     * Creates a queue for Gemini South with some exchanges and classical proposals.
     */
    @Test
    @Ignore
    public void createQueueSouthWithExchanges() {
        Long[] exchangeIds = getExchangeProposals(4, Site.SOUTH);
        final int totalTimeAvailable = 1200;

        HashMap<String, Double> subaruExchanges = new HashMap<String, Double>();
        subaruExchanges.put("BR", 5.0);
        subaruExchanges.put("CA", 10.0);
        subaruExchanges.put("GS", 5.0);
//        subaruExchanges.put("UK", 20.0);
        subaruExchanges.put("US", 30.0);

        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "South",                                    // the site
                totalTimeAvailable,                                       // totalTimeAvailable
                US.getId(),                                 // partnerInitalPick id     (UK)
                CONDITION_SET_SEMESTER_A_ID,                // conditionBins id         (Semester A)
                BIN_CONFIGURATION_2H10D,                    // binConfigurations id     (2h10)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createQueueSouthWithExchanges",            // queueName (must be unique),
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                subaruExchanges,
                false,                                      // schedule Subaru in Band1

                exchangeIds,
                getAllClassicalIds(),                       // classicalProposalsAccepted
                getRolloverSetId("South"),                                 // rolloverId,
                getEmptyAdjustmentsMap(),                   // adjustments per partner
                getEmptyExchangesMap(),                     // exchanges per partner
                5);                                         // queueOverfillLimit %

        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        assertTrue(queue.getBandings().size() > 30);            // select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalSouthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from north should be queued
        assertEquals(4, queue.getExchangeProposals().size());

        // ============ CHECK TIMES =============

        // ---- available hours
        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(.01 * totalTimeAvailable * CL.getPercentageShare(), getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(0, getAvailableHours(queue, UH), 0.5);
//        assertEquals(228, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        assertEquals(0, getExchangeHours(queue, AR), 0.5);
        assertEquals(0, getExchangeHours(queue, AU), 0.5);
        assertEquals(5, getExchangeHours(queue, BR), 0.5);
        assertEquals(10, getExchangeHours(queue, CA), 0.5);
        assertEquals(0, getExchangeHours(queue, CL), 0.5);
        assertEquals(5, getExchangeHours(queue, GS), 0.5);
        assertEquals(0, getExchangeHours(queue, UH), 0.5);
//        assertEquals(20, getExchangeHours(queue, UK), 0.5);
        assertEquals(30, getExchangeHours(queue, US), 0.5);
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0, getClassicalHours(queue, CA), 0.5);
        assertEquals(10, getClassicalHours(queue, CL), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(9, getClassicalHours(queue, UK), 0.5);
        assertEquals(70, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        for (AdjustmentPartnerCharge c : queue.getAdjustmentPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- partner exchange hours
        for (PartnerExchangePartnerCharge c : queue.getPartnerExchangePartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
    }

    /**
     * Creates a queue for Gemini North with exchanges, classical proposals, large partner programs hours,
     * adjustments and partner exchange hours and a total of 1200 hours.
     */
    @Test
    public void createComplexQueueNorth() {
        final int totalTimeAvailable = 1200;
        Map<String, Float> adjustments = getEmptyAdjustmentsMap();
//        adjustments.put(UK.getAbbreviation(), 5.0f);
        adjustments.put(UH.getAbbreviation(), 5.0f);
        Map<String, Float> exchanges = getEmptyExchangesMap();
//        exchanges.put(UK.getAbbreviation(), 10.0f);
        exchanges.put(UH.getAbbreviation(), 10.0f);
        Long[] exchangeids = getExchangeProposals(2, Site.NORTH);

        HashMap<String, Double> subaruExchanges = new HashMap<String, Double>();
        subaruExchanges.put("BR", 5.0);
        subaruExchanges.put("CA", 10.0);
        subaruExchanges.put("GS", 5.0);
//        subaruExchanges.put("UK", 20.0);
        subaruExchanges.put("US", 30.0);

        final Queue queue = queueService.createQueue(
                committee.getId(),                          // the committee
                "North",                                    // the site
                totalTimeAvailable,                                       // totalTimeAvailable
                CA.getId(),                                 // partnerInitalPick id     (CA)
                CONDITION_SET_SEMESTER_B_ID,                // conditionBins id         (Semester B)
                BIN_CONFIGURATION_2H20D,                    // binConfigurations id     (2h20)
                30,                                         // band1Threshold %
                60,                                         // band2Threshold %
                80,                                         // band3Threshold %
                60,                                         // band3ConditionsThreshold,
                true,                                       // band3ForAllOverThreshold,
                "createComplexQueueNorth",                  // queueName (must be unique),
                "Notes",                                    // queueNotes,
                new Long[]{                                // bandRestrictionIds,
                        1l, 2l, 3l
                },
                ImmutableMap.of(RESTRICTED_BIN_WATER_VAPOR, 50,
                        RESTRICTED_BIN_LGS_OBSERVATIONS, 200), // restrictedBinIds,
                new HashMap<String, Double>(),  // keckExchangeParticipants - all chargeable partners
                subaruExchanges,
                false,                                      // schedule Subaru in Band1
                exchangeids,
                getAllClassicalIds(),                       // classicalProposalsAccepted,
                getRolloverSetId("North"),                                 // rolloverId,
                adjustments,                                // adjustments per partner
                exchanges,                                  // exchanges per partner
                5);                                         // queueOverfillLimit %

        edu.gemini.tac.qservice.impl.QueueServiceImpl.fill(queue, committeeService, logService);
        queueService.saveQueue(queue);

        // ============ CHECK SOME BASICS =============

        // validate result ...
        assertEquals(false, queue.getDirty());
        assertEquals(false, queue.getFinalized());
        assertTrue(queue.getBandings().size() >= 39);            // select * from bandings where queue_id = <ID> and joint_banding_id is null
        assertEquals(classicalNorthIds.size(), queue.getClassicalProposals().size());  // only classical proposals from north should be queued
        assertEquals(2, queue.getExchangeProposals().size());   // two exchanges

        // ============ CHECK TIMES =============

        // ---- available hours
        assertEquals(.01 * totalTimeAvailable * AR.getPercentageShare(), getAvailableHours(queue, AR), 0.5);
        assertEquals(.01 * totalTimeAvailable * AU.getPercentageShare(), getAvailableHours(queue, AU), 0.5);
        assertEquals(.01 * totalTimeAvailable * BR.getPercentageShare(), getAvailableHours(queue, BR), 0.5);
        assertEquals(.01 * totalTimeAvailable * CA.getPercentageShare(), getAvailableHours(queue, CA), 0.5);
        assertEquals(0, getAvailableHours(queue, CL), 0.5);
        assertEquals(.01 * totalTimeAvailable * GS.getPercentageShare(), getAvailableHours(queue, GS), 0.5);
        assertEquals(.01 * totalTimeAvailable * UH.getPercentageShare(), getAvailableHours(queue, UH), 0.5);
//        assertEquals(228, getAvailableHours(queue, UK), 0.5);
        assertEquals(.01 * totalTimeAvailable * US.getPercentageShare(), getAvailableHours(queue, US), 0.5);
        // ---- exchange hours
        assertEquals(0, getExchangeHours(queue, AR), 0.5);
        assertEquals(0, getExchangeHours(queue, AU), 0.5);
        assertEquals(5, getExchangeHours(queue, BR), 0.5);
        assertEquals(10, getExchangeHours(queue, CA), 0.5);
        assertEquals(0, getExchangeHours(queue, CL), 0.5);
        assertEquals(5, getExchangeHours(queue, GS), 0.5);
        assertEquals(0, getExchangeHours(queue, UH), 0.5);
//        assertEquals(20, getExchangeHours(queue, UK), 0.5);
        assertEquals(30, getExchangeHours(queue, US), 0.5);
        // ---- rollover hours
        for (RolloverPartnerCharge c : queue.getRolloverPartnerCharges().values()) {
            assertEquals(0, c.getCharge().getDoubleValue(), 0.5);
        }
        // ---- classical hours
        assertEquals(0, getClassicalHours(queue, AR), 0.5);
        assertEquals(0, getClassicalHours(queue, AU), 0.5);
        assertEquals(0, getClassicalHours(queue, BR), 0.5);
        assertEquals(0, getClassicalHours(queue, CA), 0.5);
        assertEquals(0, getClassicalHours(queue, CL), 0.5);
        assertEquals(3, getClassicalHours(queue, GS), 0.5);
        assertEquals(0, getClassicalHours(queue, UH), 0.5);
//        assertEquals(110, getClassicalHours(queue, UK), 0.5);
        assertEquals(110, getClassicalHours(queue, US), 0.5);
        // ---- semester reductions (aka adjustments) hours
        assertEquals(0, getAdjustmentHours(queue, AR), 0.5);
        assertEquals(0, getAdjustmentHours(queue, AU), 0.5);
        assertEquals(0, getAdjustmentHours(queue, BR), 0.5);
        assertEquals(0, getAdjustmentHours(queue, CA), 0.5);
        assertTrue(!queue.getAdjustmentPartnerCharges().containsKey(CL)); // no CL adjustments in NORTH queues
        assertEquals(0, getAdjustmentHours(queue, GS), 0.5);
        assertEquals(5, getAdjustmentHours(queue, UH), 0.5);
//        assertEquals(5, getAdjustmentHours(queue, UK), 0.5);
        assertEquals(0, getAdjustmentHours(queue, US), 0.5);
        // ---- partner exchange hours
        assertEquals(0, getPartnerExchangeHours(queue, AR), 0.5);
        assertEquals(0, getPartnerExchangeHours(queue, AU), 0.5);
        assertEquals(0, getPartnerExchangeHours(queue, BR), 0.5);
        assertEquals(0, getPartnerExchangeHours(queue, CA), 0.5);
        assertTrue(!queue.getPartnerExchangePartnerCharges().containsKey(CL)); // no CL partner charges in NORTH queues
        assertEquals(0, getPartnerExchangeHours(queue, GS), 0.5);
        assertEquals(10, getPartnerExchangeHours(queue, UH), 0.5);
//        assertEquals(10, getPartnerExchangeHours(queue, UK), 0.5);
        assertEquals(0, getPartnerExchangeHours(queue, US), 0.5);
    }

    // ====================== HELPERS =============================================================================

    private double getClassicalHours(Queue queue, Partner partner) {
        return queue.getClassicalPartnerCharges().get(partner).getCharge().getDoubleValueInHours();
    }

    private double getExchangeHours(Queue queue, Partner partner) {
        return queue.getExchangePartnerCharges().get(partner).getCharge().getDoubleValueInHours();
    }

    private double getPartnerExchangeHours(Queue queue, Partner partner) {
        return queue.getPartnerExchangePartnerCharges().get(partner).getCharge().getDoubleValueInHours();
    }

    private double getAdjustmentHours(Queue queue, Partner partner) {
        return queue.getAdjustmentPartnerCharges().get(partner).getCharge().getDoubleValueInHours();
    }

    private double getAvailableHours(Queue queue, Partner partner) {
        final Map<Partner, AvailablePartnerTime> availablePartnerTimes = queue.getAvailablePartnerTimes();
        return availablePartnerTimes.get(partner).getCharge().getDoubleValueInHours();
    }

    private void prepareValues() {
        // make all partners available (from database!)
        AR = getPartner("AR");
        AU = getPartner("AU");
        BR = getPartner("BR");
        CA = getPartner("CA");
        CL = getPartner("CL");
        GS = getPartner("GS");
        UH = getPartner("UH");
        US = getPartner("US");

        // get first committee with proposals (assuming that's the one we want to run the tests against)
        List<Committee> committees = committeeService.getAllCommittees();
        for (Committee c : committees) {
            List<Proposal> cProposals = committeeService.getAllProposalsForCommittee(c.getId());
            if (cProposals.size() > 0) {
                // collect some relevant information
                committee = c;
                proposals = cProposals;
                classicalNorthIds = new HashSet<Long>();
                classicalSouthIds = new HashSet<Long>();
                northExchangeIds = new HashSet<Long>();
                southExchangeIds = new HashSet<Long>();
                for (Proposal p : proposals) {
                    if (p.isClassical()) {
                        if (Site.NORTH.equals(p.getSite())) {
                            classicalNorthIds.add(p.getId());
                        } else if (Site.SOUTH.equals(p.getSite())) {
                            classicalSouthIds.add(p.getId());
                        } else {
                            throw new RuntimeException("inconsistent data");
                        }
                    }
                    String partnerKey = p.getPartner().getPartnerCountryKey();
                    if (p.isExchange() && partnerKey != "UH" && partnerKey != "CL") {
                        if(p.belongsToSite(Site.NORTH)){
                            northExchangeIds.add(p.getId());
                        }else{
                            southExchangeIds.add(p.getId());
                        }
                    }
                }

                break;
            }
        }

        // validate assumptions on which the data in these tests is based
        validateAssumptions();
    }

    /**
     * These tests depend heavily on the underlying fixture data, small changes will immediately break these
     * tests (e.g. propsal data, partner shares, bin configurations). Therefore we quickly check here some
     * of the most volatile assumptions. If any of the assertions below breaks the fixture data has changed.
     * In case this happens check
     * phase1-data/src/main/database/datasets/*.xml and
     * phase1-data/src/test/resources/edu/gemini/tac/persistence/fixtures/2012Bproposals.tar.gz
     * for changes.
     */
    private void validateAssumptions() {
        // IF NUMBER OF PROPOSALS IN COMMITTEE CHANGES TESTS WILL BREAK!
        assertEquals(188, proposals.size()); // number of proposals which are not components of joints
        // IF SHARES ARE CHANGED, THE NUMBERS WE ARE TESTING AGAINST BECOME INVALID!
        assertEquals(3.8,  GS.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(8.9,  CL.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(57.7, US.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(8.9,  UH.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(17.3, CA.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(2.1,  AR.getPercentageShare(), Double.MIN_VALUE);
        assertEquals(5.8,  AU.getPercentageShare(), Double.MIN_VALUE);
    }

    private Long[] getAllClassicalIds() {
        Set<Long> allClassical = new HashSet<Long>();
        allClassical.addAll(classicalNorthIds);
        allClassical.addAll(classicalSouthIds);
        return allClassical.toArray(new Long[allClassical.size()]);
    }

    private Map<String, Float> getEmptyAdjustmentsMap() {
        Map<String, Float> adjustments = new HashMap<String, Float>();
        adjustments.put(AR.getAbbreviation(), 0.0f);
        adjustments.put(US.getAbbreviation(), 0.0f);
        adjustments.put(BR.getAbbreviation(), 0.0f);
        adjustments.put(AU.getAbbreviation(), 0.0f);
        adjustments.put(CA.getAbbreviation(), 0.0f);
        adjustments.put(GS.getAbbreviation(), 0.0f);
        adjustments.put(UH.getAbbreviation(), 0.0f);
        return adjustments;
    }

    private Map<String, Float> getEmptyExchangesMap() {
        Map<String, Float> exchanges = new HashMap<String, Float>();
        exchanges.put(AR.getAbbreviation(), 0.0f);
        exchanges.put(US.getAbbreviation(), 0.0f);
        exchanges.put(BR.getAbbreviation(), 0.0f);
        exchanges.put(AU.getAbbreviation(), 0.0f);
        exchanges.put(CA.getAbbreviation(), 0.0f);
        exchanges.put(GS.getAbbreviation(), 0.0f);
        exchanges.put(UH.getAbbreviation(), 0.0f);
        return exchanges;
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

}
