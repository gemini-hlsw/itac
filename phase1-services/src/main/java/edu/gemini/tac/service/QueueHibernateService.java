package edu.gemini.tac.service;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.*;
import edu.gemini.tac.exchange.ProposalExporterImpl;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule;
import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.persistence.daterange.*;
import edu.gemini.tac.persistence.emails.Email;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.ItacAccept;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import edu.gemini.tac.persistence.queues.*;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.partnerCharges.AdjustmentPartnerCharge;
import edu.gemini.tac.persistence.queues.partnerCharges.ExchangePartnerCharge;
import edu.gemini.tac.persistence.queues.partnerCharges.PartnerCharge;
import edu.gemini.tac.persistence.queues.partnerCharges.PartnerExchangePartnerCharge;
import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Implementation of queue service interface.
 *
 * @author ddawson
 */
@Service(value = "queueService")
public class QueueHibernateService implements IQueueService {

    @Value("${itac.skeletonService.url.north}")
    private String skeletonServiceUrlNorth;
    @Value("${itac.skeletonService.url.south}")
    private String skeletonServiceUrlSouth;

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Resource(name = "partnerService")
    IPartnerService partnerService;

    @Resource(name = "committeeService")
    ICommitteeService committeeService;

    @Resource(name = "logService")
    AbstractLogService logService;

    @Resource(name = "rolloverService")
    IRolloverService rolloverService;

    @Resource(name = "proposalService")
    IProposalService proposalService;

    @Resource(name = "emailsService")
    IEmailsService emailsService;

    @Resource(name = "restrictedBinsService")
    IRestrictedBinsService restrictedBinsService;

    private static final Logger LOGGER = Logger.getLogger("edu.gemini.tac.service.QueueHibernateService");

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public Queue createQueue(final Long committeeId, final String siteName, final int totalTimeAvailable, final Long partnerInitalPick,
                             final Long conditionBinsId, final Long binConfigurationsId,
                             final int band1Threshold, final int band2Threshold, final Integer band3Threshold,
                             final Integer band3ConditionsThreshold, final Boolean band3ForAllOverThreshold,
                             final String queueName, final String queueNotes, final Long[] bandRestrictionIds,
                             final Map<Long, Integer> restrictedBinIds,
                             final Map<String, Double> keckExchangesByPartnerName,
                             final Map<String, Double> subaruExchangesByPartnerName,
                             final Boolean subaruScheduleViaQueue,
                             final Long[] exchangeProposalsAcccepted,
                             final Long[] classicalProposalsAccepted,
                             final Long rolloverSetId,
                             final Map<String, Float> adjustments,
                             final Map<String, Float> exchanges,
                             final Integer queueOverfillLimit) {
        Validate.notNull(committeeId);
        Validate.notEmpty(siteName);

        final Session session = sessionFactory.getCurrentSession();
        final Query committeeQuery = session.getNamedQuery("committee.findCommitteeById").setLong("id", committeeId);
        final Committee committee = (Committee) committeeQuery.uniqueResult();
        //Bring in the Shutdowns
        final Set<Shutdown> shutdowns = committee.getShutdowns();
        for (Shutdown s : shutdowns) {
            Site site = s.getSite();
        }
        final Site site = (Site) (session.getNamedQuery("site.findSiteByDisplayName").setString("displayName", siteName).uniqueResult());
        final Queue queue = createAndInitializeQueue(totalTimeAvailable, band1Threshold, band2Threshold, band3Threshold, band3ConditionsThreshold, band3ForAllOverThreshold, queueName, queueOverfillLimit, committee, site, subaruScheduleViaQueue);

        setRestrictedBins(restrictedBinIds, session, queue);
        setInitialPick(partnerInitalPick, session, queue);
        setQueueNotes(queueNotes, queue);
        setConditionSet(conditionBinsId, session, queue);
        setBinConfiguration(binConfigurationsId, session, queue);
        setBandRestrictions(bandRestrictionIds, session, queue);
        setClassicalProposals(committeeId, classicalProposalsAccepted, session, queue);
        setRolloverSet(rolloverSetId, queue);

        final List<Partner> partners = partnerService.findAllPartners();
        Map<Partner, Double> keckExchangeParticipants = new HashMap<Partner, Double>();
        Map<Partner, Double> subaruExchangeParticipants = new HashMap<Partner, Double>();
        for (String keckExchangeName : keckExchangesByPartnerName.keySet()) {
            Partner partner = partnerService.findForKey(keckExchangeName);
            keckExchangeParticipants.put(partner, keckExchangesByPartnerName.get(keckExchangeName));
        }
        for (String subaruExchangeName : subaruExchangesByPartnerName.keySet()) {
            Partner partner = partnerService.findForKey(subaruExchangeName);
            subaruExchangeParticipants.put(partner, subaruExchangesByPartnerName.get(subaruExchangeName));
        }
        //Fill out the rest
        for (Partner partner : partners) {
            if (!keckExchangeParticipants.containsKey(partner)) {
                keckExchangeParticipants.put(partner, 0.0);
            }
            if (!subaruExchangeParticipants.containsKey(partner)) {
                subaruExchangeParticipants.put(partner, 0.0);
            }
        }

        final double subaruExchangeChargeSum = setExchangeCharges(keckExchangeParticipants, subaruExchangeParticipants, queue, partners, subaruScheduleViaQueue);

        final Set<Proposal> exchangeProposals = queue.getExchangeProposals();
        final Set<Proposal> subaruQueueEligibleProposals = new HashSet<Proposal>();
        if (exchangeProposalsAcccepted != null) {
            for (Long proposalId : exchangeProposalsAcccepted) {
                final Proposal proposal = (Proposal) session.getNamedQuery("proposal.findProposalInCommittee").setLong("committeeId", committeeId).setLong("proposalId", proposalId).uniqueResult();
                Validate.notNull(proposal, "Proposal[" + proposalId + "] is not in committee [" + committeeId + "]");
                Hibernate.initialize(proposal);
                Hibernate.initialize(proposal.getPhaseIProposal());
                Hibernate.initialize(proposal.getPartner());
                String partnerReferenceNumber = proposal.getPartnerReferenceNumber();
                final Partner partner = proposal.getPartner();
                Validate.notNull(partner, "Partner of proposal[" + proposal.getId() + "] is null");
                final String partnerCountryKey = partner.getPartnerCountryKey();
                final boolean isSubaru = partnerCountryKey.equalsIgnoreCase("SUBARU");
                //For Keck proposals or (Subaru & "as exchange" scheduling), just add 'em as exchange
                if (isSubaru  && subaruScheduleViaQueue){
                    subaruQueueEligibleProposals.add(proposal);
                }else{
                    exchangeProposals.add(proposal);
                }
            }
        } else {
            LOGGER.log(Level.INFO, "No exchange proposals identified in queue proposals");
        }

        final Map<String, Partner> partnerMap = new HashMap<String, Partner>();
        for (Partner p : partners) {
            partnerMap.put(p.getAbbreviation(), p);
        }
        final Map<Partner, AdjustmentPartnerCharge> adjustmentCharges =
                queue.getAdjustmentPartnerCharges();
        final Map<Partner, PartnerExchangePartnerCharge> exchangeCharges =
                queue.getPartnerExchangePartnerCharges();

        queue.setSubaruScheduledByQueueEngine(subaruScheduleViaQueue);
        queue.setPartnerPercentages(calculatePartnerPercentages(subaruScheduleViaQueue, partners, queue, subaruExchangeChargeSum, rolloverHours(rolloverSetId), queueOverfillLimit));
        queue.setSubaruProposalsEligibleForQueue(subaruQueueEligibleProposals);
        queue.setExchangeProposals(exchangeProposals);

        //Make sure the reference numbers are in-session for Subaru calculations
        for(Proposal p : queue.getExchangeProposals()){
            Hibernate.initialize(p);
            p.getPartnerReferenceNumber();
        }

        for (String key : adjustments.keySet()) {
            adjustmentCharges.put(partnerMap.get(key),
                    new AdjustmentPartnerCharge(queue,
                            partnerMap.get(key),
                            new TimeAmount(adjustments.get(key).doubleValue(), TimeUnit.HR)));
        }

        for (String key : exchanges.keySet()) {
            exchangeCharges.put(partnerMap.get(key),
                    new PartnerExchangePartnerCharge(queue,
                            partnerMap.get(key),
                            new TimeAmount(exchanges.get(key).doubleValue(), TimeUnit.HR)));
        }
        LOGGER.log(Level.INFO, "adjustmentCharges:" + adjustmentCharges);
        LOGGER.log(Level.INFO, "exchangeCharges:" + exchangeCharges);
        LOGGER.log(Level.INFO, "partnerMap:" + partnerMap);

        saveAndHibernateInitialize(session, queue);

        return queue;
    }

    private Integer rolloverHours(Long rolloverSetId) {
        if (rolloverSetId == null) {
            return 0;
        }
        final RolloverSet rolloverSet = rolloverService.getRolloverSet(rolloverSetId);
        long hours = rolloverSet.getTotalHours();
        return new Long(hours).intValue();
    }


    private double compensateForQueueOverfill(double initial, int overfillLimit) {
        final int QUEUE_FILL_PERCENTAGE = 80;
        double queueFillPercentage = (1.0 * QUEUE_FILL_PERCENTAGE + overfillLimit) / 100.0;
        return initial / queueFillPercentage;
    }

    /* TODO: Convert back to private after testing */
    public Set<PartnerPercentage> calculatePartnerPercentages(boolean subaruScheduleViaQueue,
                                                              List<Partner> partners,
                                                              Queue queue,
                                                              double subaruExchangeChargeSum,
                                                              Integer rolloverHours,
                                                              Integer queueOverfillLimit) {
        Set<PartnerPercentage> partnerPercentages = new HashSet<PartnerPercentage>();
        if (subaruScheduleViaQueue) {
            //Retrieve the nominal partner percentages
            //Calculate nominal times
            final Integer totalTimeBeforeRollovers = queue.getTotalTimeAvailable();
            final Integer totalMinusRollover = totalTimeBeforeRollovers - rolloverHours;
            Map<Partner, Double> nominalPartnerHours = nominalHours(partners, totalMinusRollover);
            //Give the time to Subaru
            Partner subaru = getPartner(partners, "Subaru");
            //Increase hours to allow them being taken away when partners "pay" for rollover time proportionally
            final double subaruPercentButNotPayingRolloverTime = subaruExchangeChargeSum * totalTimeBeforeRollovers / totalMinusRollover;
            //Increase hours so that 100% of Subaru time is allowed in this queue, despite queue overfill limit (i.e., 85% fill)
            final double subaruBoostedToCompensateForQueueFill = compensateForQueueOverfill(subaruPercentButNotPayingRolloverTime, queueOverfillLimit);
            nominalPartnerHours.put(subaru, subaruBoostedToCompensateForQueueFill);

            //Take out the exchange time
            Map<Partner, Double> queuePartnerHours = new HashMap<Partner, Double>(nominalPartnerHours);
            //The exchange time has already been charged to the partners via exchangeCharges() above
            final Map<Partner, ExchangePartnerCharge> exchangePartnerCharges = queue.getExchangePartnerCharges();
            for (Partner chargedPartner : exchangePartnerCharges.keySet()) {
                double nominalHours = nominalPartnerHours.get(chargedPartner);
                final ExchangePartnerCharge exchangePartnerCharge = exchangePartnerCharges.get(chargedPartner);
                double modifiedHours = nominalHours - exchangePartnerCharge.getCharge().getDoubleValueInHours();
                if(modifiedHours < 0){
                    throw new RuntimeException("Negative hours calculated for " + chargedPartner.getPartnerCountryKey() + " nominal: " + nominalHours + " modified: " + modifiedHours);
                }
                queuePartnerHours.put(chargedPartner, modifiedHours);
            }
            //Take out host from other site
            if (queue.getSite().getDisplayName().compareToIgnoreCase("north") == 0) {
                Partner chile = getPartner(partners, "CL");
                queuePartnerHours.put(chile, 0.0);
            } else {
                Partner uh = getPartner(partners, "UH");
                queuePartnerHours.put(uh, 0.0);
            }
            //Calculate the alteredPartnerPercentages
            List<PartnerPercentage> pps = PartnerPercentage.fromHours(queue, queuePartnerHours);
            for (PartnerPercentage pp : pps) {
                LOGGER.log(Level.DEBUG, "Partner Percentage" + pp.getPartner().getPartnerCountryKey() + "->" + pp.percentage());
            }
            partnerPercentages.addAll(pps);
        } else {
            for (Partner p : partners) {
                PartnerPercentage pp = new PartnerPercentage(queue, p, 1.0 * p.getPercentageShare() / 100.0);
                partnerPercentages.add(pp);
            }
        }

        //Special case for UH and CL in opposite hemisphere
        if (queue.getSite().getDisplayName().equalsIgnoreCase("North")) {
            //Remove CL
            for (PartnerPercentage pp : partnerPercentages) {
                if (pp.getPartner().isNorth() == false) {
                    pp.setPercentage(0);
                }
            }
        } else {
            for (PartnerPercentage pp : partnerPercentages) {
                if (pp.getPartner().isSouth() == false) {
                    pp.setPercentage(0);
                }
            }
        }
        return partnerPercentages;
    }

    private Partner getPartner(List<Partner> partners, String partnerCountryKey) {
        for (Partner partner : partners) {
            if (partner.getPartnerCountryKey().equalsIgnoreCase(partnerCountryKey)) {
                return partner;
            }
        }
        throw new RuntimeException("Did not recognize partner country key " + partnerCountryKey);
    }


    private Map<Partner, Double> nominalHours(List<Partner> partners, int queueHours) {
        Map<Partner, Double> nominalHours = new HashMap<Partner, Double>();
        for (Partner partner : partners) {
            nominalHours.put(partner, nominalHours(partner, queueHours));
        }
        return nominalHours;
    }

    private Double nominalHours(Partner p, int queueHours) {
        return 0.01 * p.getPercentageShare() * queueHours;
    }

    private void setRolloverSet(Long rolloverSetId, Queue queue) {
        if (rolloverSetId != null) {
            RolloverSet rs = rolloverService.getRolloverSet(rolloverSetId);
            queue.setRolloverset(rs);
        }
    }

    private void setClassicalProposals(Long committeeId, Long[] classicalProposalsAccepted, Session session, Queue queue) {
        final Set<Proposal> classicalProposals = queue.getClassicalProposals();
        if (classicalProposalsAccepted != null) {
            for (Long proposalId : classicalProposalsAccepted) {
                final Proposal proposal = (Proposal) session.getNamedQuery("proposal.findProposalInCommittee").setLong("committeeId", committeeId).setLong("proposalId", proposalId).uniqueResult();

                // Only add classical proposals to queue that belong to the site the queue is created for
                // and don't add any components, we are just interested in the joints and the single proposals.
                if (proposal.belongsToSite(queue.getSite())) {
                    // ITAC-270: also don't add classical proposals to queue that don't have any time allocated by the partners
                    TimeAmount t = proposal.getTotalRecommendedTime();
                    if (t.getDoubleValue() > 0.0) {
                        classicalProposals.add(proposal);
                    }
                }
            }
        } else {
            LOGGER.log(Level.INFO, "No classical proposals identified in queue proposals");
        }
    }

    /**
     * @param keckExchangeParticipants
     * @param subaruExchangeParticipants
     * @param queue
     * @param partners
     * @return Returns Subaru *only* sum of charges (to avoid recalculation in the case of Subaru schedule-via-queue-engine
     */
    private double setExchangeCharges(Map<Partner, Double> keckExchangeParticipants, Map<Partner, Double> subaruExchangeParticipants, Queue queue, List<Partner> partners, boolean subaruScheduleViaQueue) {
        final Map<Partner, ExchangePartnerCharge> exchangePartnerCharges = queue.getExchangePartnerCharges();
        exchangeCharges(keckExchangeParticipants, queue, exchangePartnerCharges);
        double subaruExchangeCharges = exchangeCharges(subaruExchangeParticipants, queue, exchangePartnerCharges);
        return subaruExchangeCharges;
    }

    private List<Partner> chargeablePartners(Double[] keckExchangeParticipants, Double[] subaruExchangeParticipants, List<Partner> partners) {
        List<Partner> chargeablePartners = new ArrayList<Partner>();
        for (Partner p : partners) {
            if (p.isChargeable())
                chargeablePartners.add(p);
        }
        Validate.isTrue(chargeablePartners.size() == keckExchangeParticipants.length, "Partners size not matching up with participant charges, possible data corruption.");
        Validate.isTrue(chargeablePartners.size() == subaruExchangeParticipants.length, "Partners size not matching up with participant charges, possible data corruption.");
        return chargeablePartners;
    }

    private void saveAndHibernateInitialize(Session session, Queue queue) {
        session.save(queue);

        //Bring in necessary Lazy objects
        queue.getBandRestrictionRules();
        queue.getBinConfiguration();
        queue.getConditionSet();
    }

    private void setBinConfiguration(Long binConfigurationsId, Session session, Queue queue) {
        final Query binConfigurationQuery = session.getNamedQuery("binConfiguration.findById").setLong("id", binConfigurationsId);
        queue.setBinConfiguration((BinConfiguration) binConfigurationQuery.uniqueResult());
    }

    private void setConditionSet(Long conditionBinsId, Session session, Queue queue) {
        final Query conditionSetQuery = session.getNamedQuery("condition.findById").setLong("id", conditionBinsId);
        queue.setConditionSet((ConditionSet) conditionSetQuery.uniqueResult());
    }

    private void setInitialPick(Long partnerInitalPick, Session session, Queue queue) {
        final Query partnerQuery = session.getNamedQuery("partner.findPartnerById").setLong("id", partnerInitalPick);
        queue.setPartnerWithInitialPick((Partner) partnerQuery.uniqueResult());
    }

    private void setBandRestrictions(Long[] bandRestrictionIds, Session session, Queue queue) {
        if (bandRestrictionIds != null && bandRestrictionIds.length != 0) {
            final Query bandRestrictionRulesQuery = session.getNamedQuery("bandRestriction.findByIds").setParameterList("ids", bandRestrictionIds);
            final List<BandRestrictionRule> bandRestrictionRulesList = bandRestrictionRulesQuery.list();
            final Set<BandRestrictionRule> bandRestrictionRulesSet = new HashSet<BandRestrictionRule>(bandRestrictionRulesList.size());
            bandRestrictionRulesSet.addAll(bandRestrictionRulesList);
            queue.setBandRestrictionRules(bandRestrictionRulesSet);
        }
    }

    private void setQueueNotes(String queueNotes, Queue queue) {
        if (queueNotes != null && !queueNotes.isEmpty()) {
            final Set<QueueNote> notes = new HashSet<QueueNote>(1);
            final QueueNote note = new QueueNote();
            note.setNote(queueNotes);
            note.setQueue(queue);
            notes.add(note);
            queue.setNotes(notes);
        }
    }

    private void setRestrictedBins(Map<Long, Integer> restrictedBinIds, final Session session, Queue queue) {
        if (restrictedBinIds != null && !restrictedBinIds.isEmpty()) {

            Set<Long> ids = restrictedBinIds.keySet();
            Query restrictedBinsQuery = session.getNamedQuery("restrictedBin.findByIds").setParameterList("ids", ids);
            final List<RestrictedBin> storedRestrictedBins = restrictedBinsQuery.list();
            Set<RestrictedBin> restrictedBins = ImmutableSet.copyOf(Collections2.transform(restrictedBinIds.entrySet(), new Function<Map.Entry<Long, Integer>, RestrictedBin>() {
                @Override
                public RestrictedBin apply(java.util.Map.Entry<Long, Integer> entry) {
                    final Long id = entry.getKey();
                    // this is n2 but we only have 2 entries
                    return ImmutableList.copyOf(Collections2.filter(storedRestrictedBins, new Predicate<RestrictedBin>() {
                        @Override
                        public boolean apply(RestrictedBin restrictedBin) {
                            return restrictedBin.getId().equals(id);
                        }
                    })).get(0).copy(entry.getValue());
                }
            }));
            restrictedBins = ImmutableSet.copyOf(Iterables.transform(restrictedBins, new Function<RestrictedBin, RestrictedBin>() {
                @Override
                public RestrictedBin apply(RestrictedBin restrictedBin) {
                    session.persist(restrictedBin);
                    return restrictedBin;
                }
            }));
            System.out.println("SET RESTRICTED BINS " + restrictedBins);

            queue.setRestrictedBins(restrictedBins);
        }
    }

    private Queue createAndInitializeQueue(
            int totalTimeAvailable, int band1Threshold, int band2Threshold, Integer band3Threshold,
            Integer band3ConditionsThreshold, Boolean band3ForAllOverThreshold,
            String queueName, Integer queueOverfillLimit, Committee committee, Site site, Boolean subaruScheduleViaQueue) {
        final Queue queue = new Queue(queueName, committee);

        queue.setFinalized(false);
        queue.setBand1Cutoff(band1Threshold);
        queue.setBand2Cutoff(band2Threshold);
        queue.setBand3Cutoff(band3Threshold);
        queue.setBand3ConditionsThreshold(band3ConditionsThreshold);
        queue.setTotalTimeAvailable(totalTimeAvailable);
        queue.setUseBand3AfterThresholdCrossed(band3ForAllOverThreshold);
        queue.setOverfillLimit(queueOverfillLimit);
        queue.setSubaruScheduledByQueueEngine(subaruScheduleViaQueue);
        queue.setSite(site);
        return queue;
    }

    private double exchangeCharges(Map<Partner, Double> exchangeParticipants, Queue queue, Map<Partner, ExchangePartnerCharge> exchangePartnerCharges) {
        double totalCharge = 0.0;
        for (Partner partner : exchangeParticipants.keySet()) {
            Double charge = exchangeParticipants.get(partner);
            final ExchangePartnerCharge exchangePartnerCharge = new ExchangePartnerCharge(queue, partner, new TimeAmount(charge, TimeUnit.HR)); // Assuming time values are in hours
            exchangePartnerCharges.put(partner, exchangePartnerCharge);
            totalCharge += charge;
        }
        return totalCharge;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Queue getQueue(final Long queueId) {
        final Queue queue = getQueueWithoutData(queueId);

        Hibernate.initialize(queue.getCommittee());
        if (queue != null) {
            final List<Proposal> proposals = new ArrayList<Proposal>();

            // get banded proposals
            for (Banding b : queue.getBandings()) {
                proposals.add(b.getProposal());
                if (b instanceof JointBanding) {
                    proposals.addAll(((JointBanding) b).getRejectedProposals());
                }
            }

            // get classical proposals
            for (Proposal proposal : queue.getCopyOfClassicalProposalsSorted()) {
                proposals.add(proposal);
            }

            // get exchange proposals
            for (Proposal proposal : queue.getExchangeProposals()) {
                proposals.add(proposal);
            }

            //Get partner percentages
            for(PartnerPercentage pp : queue.getPartnerPercentages()){
                //Load them into session
                Partner p = pp.getPartner();
                String countryKey = p.getPartnerCountryKey();
            }

            // load all needed additional information for proposals to avoid lazy initialization exceptions...
            committeeService.populateProposalsWithListingInformation(proposals);
        }

        return queue;
    }

    @Override
    @Transactional(readOnly = true)
    public Queue getQueueWithoutData(final Long queueId) {
        final Session currentSession = sessionFactory.getCurrentSession();

        // Breaking this up in order to track down what's happening with duplicate proposal listings showing up in queue details.
        final Query queueWithBandingsQuery = currentSession.getNamedQuery("queue.findOnlyQueueWithBandingsById").setLong("queueId", queueId);
        final Queue queue = (Queue) queueWithBandingsQuery.uniqueResult();
        final Query queueWithClassicalQuery = currentSession.getNamedQuery("queue.findOnlyQueueWithClassicalProposalsByQueue").setParameter("queue", queue);
        final Queue queueWithClassical = (Queue) queueWithClassicalQuery.uniqueResult();

        return queue;
    }

    @Override
    @Transactional
    public void saveQueue(final Queue queue) {
        final Session session = sessionFactory.getCurrentSession();
        final Queue newQueue = (Queue) session.merge(queue); // reattaching queue from previous service call
        session.saveOrUpdate(newQueue);
        //Retrieve the Committee and update it, due to circular reference and no cascade
        final Committee c = newQueue.getCommittee();
        session.saveOrUpdate(c);
        //Cascade to proposals
        for (Proposal p : c.getProposals()) {
            session.saveOrUpdate(p);
        }

        session.flush();
    }

    @Override
    @Transactional
    public void setBandingBand(final long bandingId, final int band) {
        final Session session = sessionFactory.getCurrentSession();

        final Query query = session.createQuery("from Banding b where b.id = :bandingId");
        query.setParameter("bandingId", bandingId);
        final Banding banding = (Banding) query.uniqueResult();
        final ScienceBand scienceBand = ScienceBand.lookupFromRank(band);
        banding.setBand(scienceBand);
        session.flush();
    }

    @Override
    @Transactional
    public void finalizeQueue(final Long queueId) {
        final Session session = sessionFactory.getCurrentSession();

        final Queue queue = getQueueWithoutData(queueId);
        // no finalized queue for this site may exist already!
        final Queue finalizedQueue = getFinalizedQueue(queue.getCommittee().getId(), queue.getSite());
        Validate.isTrue(finalizedQueue == null);

        // step 1: set finalized to true (wow, that's a lot of hard work here...)
        queue.setFinalized(Boolean.TRUE);
        session.update(queue);

        // step 3: generate program ids
        queue.programIds(queue.getCommittee().getSemester().getName().equals("A") ? false : true, session);

        // step 2: set band information in itac extension
        updateFinalizedProposals(queue);
    }

    private void updateFinalizedProposals(Queue queue) {

        // NOTE: see ITAC-285: this will set the program id, band and awarded time twice for split joint proposals
        // because they are actually banded twice (once in Band 1, 2 or three and once more in Band 4).
        // For those proposals the program id is taken from the banding and the awarded time will be recalculated
        // during skeleton export while creating the temporary partial joints.

        final Session session = sessionFactory.getCurrentSession();

        for (final Banding banding : queue.getBandings()) {
            if (!banding.isJointComponent()) {
                Validate.notNull(banding.getProposal());
                Validate.notNull(banding.getAwardedTime());
                Validate.notNull(banding.getBand());
                final Proposal proposal = banding.getProposal();
                final Itac itac = proposal.getItac();
                final ItacAccept accept = (itac.getAccept() == null ? new ItacAccept() : itac.getAccept());
                accept.setBand((int) banding.getBand().getRank());
                accept.setProgramId(banding.getProgramId());
                accept.setAward(new TimeAmount(banding.getAwardedTime()));
//                Validate.isTrue(accept.getAward().getDoubleValue() > 0.0);
                itac.setAccept(accept);
                fixupNgoEmailDueToSchemaBug(proposal, session);

                session.update(proposal.getPhaseIProposal().getPrimary());
                session.update(itac);
            }
        }

        int ix = 0;
        for (final Proposal classicalProposal : queue.getClassicalProposals()) {
            if (!classicalProposal.isJointComponent()) {
                // set awarded time for classical proposals
                final Itac itac = classicalProposal.getItac();
                final ItacAccept accept = (itac.getAccept() == null ? new ItacAccept() : itac.getAccept());
                accept.setBand((int) ScienceBand.BAND_ONE.getRank());
                accept.setProgramId(classicalProposal.createProgramId(++ix));
                accept.setAward(new TimeAmount(classicalProposal.getTotalRecommendedTime()));
//                Validate.isTrue(accept.getAward().getDoubleValue() > 0.0);
                itac.setAccept(accept);
                fixupNgoEmailDueToSchemaBug(classicalProposal, session);

                session.update(classicalProposal.getPhaseIProposal().getPrimary());
                session.update(itac);
            }
        }
    }

    /**
     * Schema error means that the ITAC accept email field is meaningless.  However, it is likely that some
     * NGOs entered their contact information here instead of in the NGO Submission Accept.  The business rule
     * agreeed upon as a fixup is use the itac email (by copying it into the ngo field) only if it is empty.
     * The idea of merging was considered and rejected.  This code can safely go away when the schema is updated.
     *
     * @param proposal
     */
    private void fixupNgoEmailDueToSchemaBug(final Proposal proposal, final Session session) {
        Validate.notNull(proposal);

        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        final Itac itac = proposal.getItac();

        final Submission primarySubmission = phaseIProposal.getPrimary();
        final SubmissionAccept ngoAccept = primarySubmission.getAccept();
        final boolean ngoEmailPresent = !StringUtils.isBlank(ngoAccept.getEmail());
        final boolean itacEmailPresent = !StringUtils.isBlank(itac.getAccept().getEmail());
        if (!ngoEmailPresent && itacEmailPresent) {
            ngoAccept.setEmail(itac.getAccept().getEmail());
            session.update(primarySubmission);
        }
    }

    @Override
    @Transactional
    public void unFinalizeQueue(final Long queueId) {
        final Session session = sessionFactory.getCurrentSession();

        final Queue queue = getQueueWithoutData(queueId);
        // this queue must be finalized!
        Validate.isTrue(queue.getFinalized());


        // ITAC-456 When unfinalizing the queue, existing emails should be deleted.
        final List<Email> emailsForQueue = emailsService.getEmailsForQueue(queueId);
        for (Email e : emailsForQueue)
            session.delete(e);

        // change finalized flag
        queue.setFinalized(Boolean.FALSE);
        session.update(queue);
    }

    /**
     * Simple object for transporting results back to the client.
     */
    public static class SkeletonResult {
        private final Integer status;
        private final Long proposalId;
        private final String error;

        public SkeletonResult(Integer status, Long proposalId, String error) {
            this.status = status;
            this.proposalId = proposalId;
            this.error = error;
        }

        public Integer getStatus() {
            return status;
        }

        public Long getProposalId() {
            return proposalId;
        }

        public String getError() {
            return error;
        }

        public boolean isSuccessful() {
            return status > 0 && status < 300;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SkeletonResult> exportSkeletons(final Long queueId, final File pdfFolder) {
        final Queue queue = getQueueWithoutData(queueId);
        final List<Proposal> proposalsForExport = getProposalsForSkeletonExport(queue);
        final List<SkeletonResult> results = new ArrayList<SkeletonResult>();

        final String serviceUrl;
        if (queue.getSite().equals(Site.NORTH)) {
            serviceUrl = skeletonServiceUrlNorth;
        } else {
            serviceUrl = skeletonServiceUrlSouth;
        }

        ProposalExporterImpl exporter = new ProposalExporterImpl();
        for (Proposal p : proposalsForExport) {
            LOGGER.log(Level.INFO, "Exporting proposal " + p.getId());
            int status = 0;
            String message = "";
            try {
                byte[] xmlBytes = exporter.getAsXml(p);
                byte[] pdfBytes = proposalService.getProposalPdf(p, pdfFolder);
                String fileName = p.getPartner().getAbbreviation() + "-" + p.getId() + ".pdf";
                Part[] parts = {
                        new StringPart("proposal", new String(xmlBytes, Charset.forName("UTF-8")), "UTF-8"),
                        new FilePart("attachment", new ByteArrayPartSource(fileName, pdfBytes))
                };

                PostMethod post = new PostMethod(serviceUrl + "?convert=true");
                post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
                HttpClient client = new HttpClient();
                client.getHttpConnectionManager().getParams().setConnectionTimeout(3000);

                status = client.executeMethod(post);
                switch (status) {
                    case 200:
                        message = "skeleton updated";
                        break;
                    case 201:
                        message = "skeleton created";
                        break;
                    case 400:
                        message = "bad request (skeleton servlet did not accept the request)";
                        break;
                    case 409:
                        message =
                                "skeleton with this id is in an advanced state (beyond Phase 2) and " +
                                        "can not be updated anymore";
                        break;
                    case 500:
                        message = "request failed (odb is down or other problem)";
                        break;
                    default:
                        message = post.getStatusText();
                }

                if (status == 0 || status >= 300) {
                    LOGGER.log(Level.WARN, String.format("Skeleton export for proposal %d failed (http status = %d): %s", p.getId(), status, message));
                } else {
                    LOGGER.log(Level.INFO, String.format("Skeleton export for proposal %d was successful (http status = %d): %s", p.getId(), status, message));
                }
                results.add(new SkeletonResult(status, p.getId(), message));

            } catch (Exception e) {
                LOGGER.log(Level.WARN, String.format("Skeleton export for proposal %d failed: %s", p.getId(), e.getMessage()));
                results.add(new SkeletonResult(status, p.getId(), e.getMessage()));
            }
        }

        return results;
    }

    private List<Proposal> getProposalsForSkeletonExport(Queue queue) {
        List<Proposal> proposals = new LinkedList<Proposal>();
        Long committeeId = queue.getCommittee().getId();

        // get all banded ones, create temporary joint proposals for partially accepted ones
        // (remember that the existing joints always reflect the state of FULLY accepted joint proposals
        // with ALL proposals, for the skeleton export we need joints that contain the accepted ones only)
        for (Banding banding : queue.getBandings()) {
            if (banding instanceof JointBanding) {
                JointBanding jointBanding = (JointBanding) banding;
                if (jointBanding.isPartiallyAccepted()) {
                    // create temporary for partially accepted joints
                    Proposal partialJoint = createPartiallyAcceptedJoint(jointBanding);
                    proposals.add(partialJoint);
                } else {
                    // use the existing joint proposal for fully accepted joints
                    Proposal bandedProposal = banding.getProposal();
                    Proposal fullyLoadedProposal = proposalService.getProposal(committeeId, bandedProposal.getId());
                    proposals.add(fullyLoadedProposal);
                }
            } else if (!banding.isJointComponent()) {
                Proposal bandedProposal = banding.getProposal();
                Proposal fullyLoadedProposal = proposalService.getProposal(committeeId, bandedProposal.getId());
                proposals.add(fullyLoadedProposal);
            }
        }

        // don't forget the classical ones, they're by definition always fully accepted
        for (Proposal classicalProposal : queue.getCopyOfClassicalProposalsSorted()) {
            if (!classicalProposal.isJointComponent()) {
                Proposal fullyLoadedProposal = proposalService.getProposal(committeeId, classicalProposal.getId());
                proposals.add(fullyLoadedProposal);
            }
        }

        // return the whole bunch for further processing
        return proposals;
    }

    @Override
    @Transactional
    public Banding getBanding(Long bandingId) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Banding where id = " + bandingId);
        return (Banding) query.uniqueResult();
    }

    @Override
    @Transactional(readOnly = true)
    public Queue getFinalizedQueue(final Long committeeId, final Site site) {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("queue.findFinalizedQueueByCommitteeId").setLong("committeeId", committeeId);
        final List<Queue> finalizedQueues = query.list();
        for (Queue queue : finalizedQueues) {
            if (queue.getSite().equals(site)) {
                return queue;
            }
        }
        return null;
    }


    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public QueueCreationParameters getQueueCreationParams(final Long committeeId, final String siteDisplayName) {
        final QueueCreationParameters params = new QueueCreationParameters();

        final Session session = sessionFactory.getCurrentSession();
        final Site site = (Site) session.getNamedQuery("site.findSiteByDisplayName").setParameter("displayName", siteDisplayName).uniqueResult();
        final List<Queue> queues = committeeService.getAllQueuesForCommittee(committeeId, site.getId());

        if (queues.size() == 0) {
            return params;
        }

        Queue latestQueue = queues.get(0);
        final Query query = session.createQuery(
                "from PartnerCharge pc where pc.queue.id = :queueId").
                setLong("queueId", latestQueue.getId());

        final List<PartnerCharge> partnerCharges = query.list();

        params.setSubaruProposalsSelected(latestQueue.getSubaruProposalsEligibleForQueue());
        params.setPartnerCharges(partnerCharges);
        params.setScheduleSubaruAsPartner(latestQueue.getSubaruScheduledByQueueEngine());
        params.setBand1Cutoff(latestQueue.getBand1Cutoff());
        params.setBand2Cutoff(latestQueue.getBand2Cutoff());
        params.setBand3ConditionsThreshold(latestQueue.getBand3ConditionsThreshold());
        params.setBand3Cutoff(latestQueue.getBand3Cutoff());
        params.setBinConfiguration(latestQueue.getBinConfiguration());
        params.setConditionSet(latestQueue.getConditionSet());
        params.setOverfillLimit(latestQueue.getOverfillLimit());
        params.setPartnerWithInitialPick(latestQueue.getPartnerWithInitialPick());
        params.setTotalTimeAvailable(latestQueue.getTotalTimeAvailable());
        params.setUseBand3AfterThresholdCrossed(latestQueue.getUseBand3AfterThresholdCrossed());
        if (latestQueue.getRolloverSet() != null) {
            params.setRolloverSetId(latestQueue.getRolloverSet().getId());
        }
        params.addRestrictionBins(latestQueue.getRestrictedBins());
        params.addDefaultRestrictionBins(restrictedBinsService.getDefaultRestrictedbins());

        return params;
    }

    @Override
    @Transactional
    public void addBandingToQueue(final Long queueId, final Banding banding) {
        Validate.notNull(banding, "Null banding passed");
        Validate.notNull(banding.getBand(), "No science band in banding");
        Validate.notNull(banding.getProposal(), "No proposal in banding");
        if (banding.getQueue() != null) {
            Validate.isTrue(banding.getQueue().getId().equals(queueId), "Banding already belongs to a different queue");
        }

        LOGGER.log(Level.INFO, "Adding to Queue[" + queueId + "]" + banding.getBand().toString() + " with Proposal[" + banding.getProposal().getEntityId() + "]");
        final Session session = sessionFactory.getCurrentSession();
        session.saveOrUpdate(banding);

        final Queue queue = getQueueWithoutData(queueId);
        for (Banding b : queue.getBandings()) {
            if (b.getProposal().getPhaseIProposal().getPrimary().getReceipt().getReceiptId().equals(banding.getProposal().getPhaseIProposal().getPrimary().getReceipt().getReceiptId())) {
                String msg = "Cannot add proposal[" + banding.getProposal().getEntityId() + "] as Proposal Key '" + banding.getProposal().getPhaseIProposal().getPrimary().getReceipt().getReceiptId() + "' is already in Queue[" + queueId + "]";
                LOGGER.log(Level.WARN, msg);
                throw new IllegalArgumentException(msg);
            }
        }

        final TimeAmount awardedTime = new TimeAmount(banding.getProposal().getTotalRecommendedTime());
        final Itac itac= banding.getProposal().getItac();
        ItacAccept accept;

        if (itac.getAccept() == null) {
            accept = new ItacAccept();
            accept.setBand((int)banding.getBand().getRank());
        } else {
            accept = banding.getProposal().getItac().getAccept();
        }
        accept.setAward(awardedTime);
        if (!queue.getBandings().contains(banding)) {
            queue.addBanding(banding);
        }

        session.update(queue);
        //Program ID
        session.update(banding);
    }


    // --- export skeletons helper -> create a joint for partially accepted joints / split joints
    // The primary proposal should always be used as the "master" for the joint since it will define the
    // targets, observations and resources for the joint as a whole. But in cases where the primary is
    // not accepted we have no choice but to make an arbitrary of the other proposals the new master.

    private Proposal createPartiallyAcceptedJoint(JointBanding jointBanding) {
        // check some silly pre-conditions (believe it or not, sometimes they're not true...)
        Validate.isTrue(jointBanding.getProposal() instanceof JointProposal);
        Validate.isTrue(jointBanding.getProposal().getProposals().size() > 1);
        Validate.isTrue(jointBanding.getAcceptedProposals().size() > 0);
        Validate.isTrue(jointBanding.getRejectedProposals().size() > 0);

        LOGGER.log(Level.INFO, String.format("Creating partially accepted joint proposal for joint banding %d for joint proposal %d.", jointBanding.getId(), jointBanding.getProposal().getId()));
        Long committeeId = jointBanding.getQueue().getCommittee().getId();

        // if the actual primary is not amongst the accepted proposals we have to use another one as primary
        JointProposal fullJoint = (JointProposal) jointBanding.getProposal();
        Proposal primary = null;
        for (Proposal p : jointBanding.getAcceptedProposals()) {
            if (p.getId() == fullJoint.getPrimaryProposal().getId()) {
                // ok, actual primary is accepted, use it as primary
                primary = fullJoint.getPrimaryProposal();
                break;
            }
        }
        if (primary == null) {
            // actual primary of joint proposal was not accepted
            // there is no rule what to do in this case, we just use the first of the accepted ones as primary
            primary = jointBanding.getAcceptedProposals().iterator().next();
        }

        // create a new joint and add primary (first one to be added to the joint will be the primary)
        Proposal fullyLoadedPrimary = proposalService.getProposal(committeeId, primary.getId());
        JointProposal partialJoint = new JointProposal();
        partialJoint.add(fullyLoadedPrimary);

        // now add all other accepted proposals (except for the already added primary one)
        for (Proposal p : jointBanding.getAcceptedProposals()) {
            if (p.getId() != primary.getId()) {
                Proposal fullyLoadedProposal = proposalService.getProposal(committeeId, p.getId());
                partialJoint.add(fullyLoadedProposal);
            }
        }

        // create new itac accept part for partially accepted joint
        ItacAccept fullAccept = fullJoint.getItac().getAccept();
        ItacAccept partialAccept = new ItacAccept();
        // get the band, program id and the awarded time from the banding -> necessary in case of split joints (ITAC-285)
        // (for split joints the itacExtension data in the proposal will be set twice, so we don't have all the information there)
        partialAccept.setProgramId(jointBanding.getProgramId());
        partialAccept.setAward(jointBanding.getAwardedTime());
        partialAccept.setBand((int) jointBanding.getBand().getRank());
        // get all other values from the full joint proposal
        partialAccept.setContact(fullAccept.getContact());
        partialAccept.setEmail(fullAccept.getEmail());
        partialAccept.setRollover(fullAccept.isRollover());
        partialJoint.getItac().setAccept(partialAccept);

        return partialJoint;
    }

    @Override
    @Transactional
    public void addClassicalProposalToQueue(final Long queueId, final Proposal classicalProposal) {
        /* The classicalProposal *PROBABLY* is classical, but in the "manually rearrange queue" use-cases, it *MAY* be
           any GeminiNormalProposal.

         Validate.isTrue(classicalProposal.isClassical());
        */

        final Queue queue = getQueueWithoutData(queueId);

        Session session = sessionFactory.getCurrentSession();
        Query q = session.createQuery("from Proposal p where p.id = " + classicalProposal.getEntityId());
        Proposal inSession = (Proposal) q.uniqueResult();

        final Set<Proposal> classicalProposals = queue.getClassicalProposals();
        Validate.isTrue(classicalProposals.contains(inSession) == false, "Queue already contains this proposal");
        classicalProposals.add(inSession);

        session.save(queue);
    }

    @Override
    @Transactional
    public void addExchangeProposalToQueue(final Long queueId, final Proposal exchangeProposal) {
        Validate.isTrue(exchangeProposal.isExchange(), "Attempted to add non-exchange Proposal to exchange list");
        final Queue queue = getQueueWithoutData(queueId);

        Session session = sessionFactory.getCurrentSession();
        Query q = session.createQuery("from Proposal p where p.id = " + exchangeProposal.getEntityId());
        Proposal inSession = (Proposal) q.uniqueResult();

        final Set<Proposal> exchangeProposals = queue.getExchangeProposals();
        Validate.isTrue(exchangeProposals.contains(inSession) == false, "Queue already contains this proposal");
        exchangeProposals.add(inSession);

        session.save(queue);
    }

    @Override
    @Transactional
    public void deleteBandingFromQueue(Long queueId, Banding banding) {
        final org.hibernate.classic.Session session = sessionFactory.getCurrentSession();
        final Banding mergedBanding = (Banding) session.merge(banding);
        final Queue queue = getQueueWithoutData(queueId);
        queue.removeBanding(mergedBanding);
        session.delete(mergedBanding);
        session.saveOrUpdate(queue);
    }

    @Override
    @Transactional
    public void convertBandingToClassical(Long queueId, Banding banding) {
        LOGGER.log(Level.INFO, "Converting Banding[" + banding.getId() + "] (Proposal [" + banding.getProposal().getId() + "]) to Classical");
        final Session session = sessionFactory.getCurrentSession();
        banding = (Banding) session.merge(banding);
        final Proposal proposal = banding.getProposal();

        // Force resolution of phaseIProposal into child.
        PhaseIProposal p1p = proposal.getPhaseIProposalForModification();
//        final QueueProposal queueProposal = (QueueProposal) session.getNamedQuery("phaseIProposal.findQueueProposalById").setParameter("id", proposal.getPhaseIProposal().getId()).uniqueResult();
//        final ClassicalProposal classicalProposal = (ClassicalProposal) session.getNamedQuery("phaseIProposal.findClassicalProposalById").setParameter("id", proposal.getPhaseIProposal().getId()).uniqueResult();
//        proposal.setPhaseIProposal((queueProposal != null) ? queueProposal : classicalProposal);
        proposal.setPhaseIProposal(p1p);

        this.deleteBandingFromQueue(queueId, banding);
        proposal.setClassical(true);
        addClassicalProposalToQueue(queueId, proposal);
    }

    @Override
    @Transactional
    public void convertBandingToClassical(Long queueId, Long bandingId) {
        convertBandingToClassical(queueId, getBanding(bandingId));
    }

    @Override
    @Transactional
    public void convertClassicalToBanding(final Long queueId, final Proposal proposal, final ScienceBand band) {
        final Session session = sessionFactory.getCurrentSession();

        final Proposal mergedProposal = (Proposal) session.merge(proposal);
        final Queue queue = getQueueWithoutData(queueId);

        queue.remove(proposal); // Should take care of removing from the classical proposals attached.
        mergedProposal.setClassical(false);

        final Banding newBand = new Banding(mergedProposal, band);
        newBand.setMergeIndex(-1);

        queue.addBanding(newBand);

        session.saveOrUpdate(mergedProposal);
        session.saveOrUpdate(queue);
        session.refresh(proposal);
    }

    @Override
    @Transactional
    public void convertBandingToExchange(final Long queueId, Banding banding) {
        LOGGER.log(Level.INFO, "Converting Banding[" + banding.getId() + "] (Proposal [" + banding.getProposal().getId() + "]) to Exchange");
        final Session session = sessionFactory.getCurrentSession();
        banding = (Banding) session.merge(banding);
        final Proposal proposal = banding.getProposal();

        // Force resolution of phaseIProposal into child.
        PhaseIProposal p1p = proposal.getPhaseIProposalForModification();
        proposal.setPhaseIProposal(p1p);

        this.deleteBandingFromQueue(queueId, banding);
        addExchangeProposalToQueue(queueId, proposal);
    }

    @Override
    @Transactional
    public void convertBandingToExchange(Long queueId, Long bandingId) {
        convertBandingToExchange(queueId, getBanding(bandingId));
    }

    @Override
    @Transactional
    public void convertExchangeProposalToBanding(final Long queueId, final Proposal proposal, final ScienceBand band) {
        final Session session = sessionFactory.getCurrentSession();

        final Proposal mergedProposal = (Proposal) session.merge(proposal);
        final Queue queue = getQueueWithoutData(queueId);

        queue.remove(proposal); // Should take care of removing from the exchange proposals attached.

        final Banding newBand = new Banding(mergedProposal, band);
        newBand.setMergeIndex(-1);
        queue.addBanding(newBand);

        session.saveOrUpdate(mergedProposal);
        session.saveOrUpdate(queue);
        session.refresh(proposal);
    }

    @Override
    @Transactional
    public void removeProposalFromQueue(Long queueId, Long proposalId) {
        final Queue queue = getQueueWithoutData(queueId);
        final Session session = sessionFactory.getCurrentSession();
        final Proposal proposal =
                (Proposal) session.createQuery("from Proposal where id = :id").setParameter("id", proposalId).uniqueResult();

        queue.remove(proposal);
        session.saveOrUpdate(queue);
    }

    @Override
    @Transactional
    public List<Banding> getOrphanedBandings() {
        final Session session = sessionFactory.getCurrentSession();
        final Query query = session.createQuery("from Banding where queue_id is null");
        List<Banding> list = (List<Banding>) query.list();
        return list;
    }
}


