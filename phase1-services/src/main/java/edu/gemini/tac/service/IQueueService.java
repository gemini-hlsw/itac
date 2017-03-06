package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.springframework.security.access.annotation.Secured;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Facade that handles creation and retrieval of queues.
 *
 * @author ddawson
 *
 */
public interface IQueueService {

    /**
     * Creates a new queue based on raw ids.
     *
     * @param committeeId - the committee responsible and associated with forming the queue.
     * @param site - the site at which the queue would be executed
     * @param totalTimeAvailable - the total number of hours available for execution
     * @param partnerInitalPick - the partner who gets the initial pick in the quanta algorithm
     * @param conditionBinsId - the condition set associated with the creation of this queue
     * @param binConfigurationsId - the bin configuration associated with this queue
     * @param band1Threshold - the threshold at which band 1 ends
     * @param band2Threshold - the threshold at which band 2 ends
     * @param band3Threshold - the threshold at which band 3 ends
     * @param band3ConditionsThreshold - the threshold at which we begin using band 3 conditions for acceptance
     * @param band3ForAllOverThreshold - whether all proposals are accepted at band 3 conditions when we have crossed the threshold
     * @param queueName - a descriptive name for the queue
     * @param queueNotes - an initial note to be associated with the queue
     * @param bandRestrictionIds - the restrictions in effect for this queue
     * @param restrictedBinIds - the restricted bins in effect for this queue
     * @param keckExchangesByPartnerName - number of hours to charge for partners
     * @param subaruExchangesByPartnerName - number of hours to charge for partners,
     * @param subaruScheduleViaQueue - if false, Subaru proposals are "treated as Exchange"==auto-scheduled in Band1. If true, they are scheduled with the queue engine
     * @param exchangeProposalsAcccepted - ids of proposals accepted for exchange
     * @param classicalProposalsAcccepted - ids of accepted classical proposals
     * @param queueOverfillLimit - amount to allow overfilling of band 3
     * @return - the newly created queue
     */
    @Secured("ROLE_QUEUE_WRITER")
    Queue createQueue(Long committeeId, String site, int totalTimeAvailable, Long partnerInitalPick,
            Long conditionBinsId, Long binConfigurationsId,
            int band1Threshold, int band2Threshold, Integer band3Threshold,
            Integer band3ConditionsThreshold, Boolean band3ForAllOverThreshold,
            String queueName, String queueNotes, Long[] bandRestrictionIds,
            final Map<Long, Integer> restrictedBinIds,
            final Map<String, Double> keckExchangesByPartnerName,
            final Map<String, Double> subaruExchangesByPartnerName,
            final Boolean subaruScheduleViaQueue,
            final Long[] exchangeProposalsAcccepted,
            final Long[] classicalProposalsAcccepted,
            final Long rolloverId,
            final Map<String, Float> adjustments,
            final Map<String, Float> exchanges,
            final Integer queueOverfillLimit);

    /**
     * Retrieves a specific queue by id and pre-loads all proposal data.
     *
     * @param queueId - the numerical id of the queue
     * @return
     */
    Queue getQueue(final Long queueId);

    /**
     * Retrieves a specific queue by id without pre-loading proposal data that belongs to the queue.
     *
     * @param queueId - the numerical id of the queue
     * @return
     */
    Queue getQueueWithoutData(final Long queueId);

    /**
     * Retrieves the finalized queue for a given committee.
     * And remember: "There can be only one!".
     *
     * @param committeeId - the numerical id of the committee
     * @return
     */
    Queue getFinalizedQueue(final Long committeeId, final Site site);

    /**
     * Gets an object with a set of parameters based on the latest queue for a given committee and site that should
     * be used as presets when creating a new queue for this committee and site.
     *
     *
     * @param committeeId
     * @param siteDisplayName
     * @return
     */
    QueueCreationParameters getQueueCreationParams(final Long committeeId, final String siteDisplayName);

    /**
     * Persists queue.
     *
     * @param queue
     */
    @Secured("ROLE_QUEUE_WRITER")
    void saveQueue(Queue queue);

    /**
     * Modifies the band property for a banding.  Initially used for the manual
     * reassignment of proposals between bands.
     *
     * @param bandingId
     * @param band
     */
    @Secured("ROLE_QUEUE_WRITER")
    void setBandingBand(final long bandingId, final int band);

    /**
     * Finalize a queue.
     *
     * @param queueId
     */
    @Secured("ROLE_QUEUE_WRITER")
    void finalizeQueue(final Long queueId);

    /**
     * Un-finalize a queue.
     *
     * @param queueId
     */
    @Secured("ROLE_QUEUE_WRITER")
    void unFinalizeQueue(final Long queueId);

    /**
     * Export phase2 skeletons to ODB.
     *
     * @return
     */
    @Secured("ROLE_QUEUE_WRITER")
    Collection<QueueHibernateService.SkeletonResult> exportSkeletons(final Long queueId, final File pdfFolder);

    @Secured("ROLE_QUEUE_READER")
    Banding getBanding(Long bandingId);

    /**
     * Add a Banding to a Queue -- use-case is admin/secretary fix-up
     * @throws IllegalArgumentException if Proposal is already in queue (more accurately, if proposal.getDocument().getProposalKey() is duplicate)
     */
    @Secured("ROLE_QUEUE_WRITER")
    void addBandingToQueue(final Long queueId, final Banding newBanding);

    @Secured("ROLE_QUEUE_WRITER")
    void addClassicalProposalToQueue(final Long queueId, final Proposal classicalProposal);

    @Secured("ROLE_QUEUE_WRITER")
    void addExchangeProposalToQueue(final Long queueId, final Proposal exchangeProposal);

    @Secured("ROLE_QUEUE_WRITER")
    void deleteBandingFromQueue(final Long queueId, final Banding banding);

    @Secured("ROLE_QUEUE_WRITER")
    void convertBandingToClassical(final Long queueId, final Banding banding);
    @Secured("ROLE_QUEUE_WRITER")
    void convertBandingToClassical(final Long queueId, final Long bandingId);


    @Secured("ROLE_QUEUE_WRITER")
    void convertClassicalToBanding(final Long queueId, final Proposal proposal, final ScienceBand band);

    @Secured("ROLE_QUEUE_WRITER")
    void convertBandingToExchange(final Long queueId, final Banding banding);
    @Secured("ROLE_QUEUE_WRITER")
    void convertBandingToExchange(final Long queueId, final Long bandingId);

    @Secured("ROLE_QUEUE_WRITER")
    void convertExchangeProposalToBanding(final Long queueId, final Proposal proposal, final ScienceBand band);

    @Secured("ROLE_QUEUE_WRITER")
    void removeProposalFromQueue(final Long queueId, final Long proposalId);

    @Secured("ROLE_QUEUE_WRITER")
    List<Banding> getOrphanedBandings();

    //TODO: Remove once testing is complete
     Set<PartnerPercentage> calculatePartnerPercentages(boolean subaruScheduleViaQueue,
                                                               List<Partner> partners,
                                                               Queue queue,
                                                               double subaruExchangeChargeSum,
                                                               Integer rolloverHours,
                                                               Integer queueOverfillLimit);
}
