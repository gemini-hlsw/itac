package edu.gemini.tac.itac.web.committees;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.itac.web.util.QueueEngineWrapper;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.emails.Email;
import edu.gemini.tac.persistence.phase1.proposal.GeminiNormalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import edu.gemini.tac.persistence.phase1.submission.SubmissionReceipt;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.persistence.queues.partnerCharges.*;
import edu.gemini.tac.persistence.rollover.AbstractRolloverObservation;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import edu.gemini.tac.service.*;
import edu.gemini.tac.service.configuration.IBinConfigurationsService;
import edu.gemini.tac.service.configuration.IConditionsService;
import edu.gemini.tac.service.reports.Report;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.*;

@Controller
public class QueuesController extends ReportsController {
    private static final Logger LOGGER = Logger.getLogger("edu.gemini.tac.itac.web.committees.QueuesController");

    private static final Map<String, String> confirmTitles = new HashMap();

    static {
        confirmTitles.put("createEmails", "Create emails for this queue?");
        confirmTitles.put("sendNGOEmails", "Send all unsent NGO emails of this queue?");
        confirmTitles.put("sendPIEmails", "Send all unsent PI emails of this queue?");
        confirmTitles.put("sendAllEmails", "Send ALL (NGO and PI) unsent emails of this queue?");
        confirmTitles.put("deleteEmails", "Delete all emails of this queue?");
        confirmTitles.put("exportSkeletons", "Export skeletons for this queue?");
        confirmTitles.put("forceAddToBand", "Add proposal to this queue?");
    }

    public static Link[] getCollectionLinks(final String topLevelResourceId) {
        return new Link[]{
                new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, topLevelResourceId, "create", "New Queue")
        };
    }

    public static Link[] getResourceLinks(final String committeeId, final String resourceId, final boolean isFinalizedQueue, final boolean hasEmails, final boolean existsFinalizedQueue) {
        ArrayList<Link> links = new ArrayList<Link>();
        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "", "Proposals"));
        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "constraints", "Constraints"));
        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "log", "Queue Log"));
        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "manual", "Rearrange"));
        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "bulk-edit", "Bulk Edit"));
        if (isFinalizedQueue) {
            if (hasEmails) {
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "showEmails", "Show Emails"));
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "exportEmails", "Export Emails"));
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "sendNGOEmails", "Send NGO Emails"));
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "sendPIEmails", "Send PI Emails"));
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "sendAllEmails", "Send all Emails"));
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "deleteEmails", "Delete Emails"));
            } else {
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "createEmails", "Create Emails"));
            }
            links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "rollover-eligible", "Mark Rollover Eligible"));
            links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "unfinalize", "Un-Finalize"));
            links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "exportSkeletons", "Export Skeletons"));
        } else {
            if (!existsFinalizedQueue) {
                links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "finalize", "Finalize"));
            }
        }

        links.add(new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, resourceId, committeeId, "reports", "Queue Reports"));
        Link[] objects = links.toArray(new Link[]{});
        return objects;
    }

    // pdf storage and access locations
    @Value("${itac.pdf.folder}")
    private File pdfFolder;

    @Resource(name = "conditionsService")
    private IConditionsService conditionsService;
    @Resource(name = "binConfigurationsService")
    private IBinConfigurationsService binConfigurationService;
    @Resource(name = "bandRestrictionsService")
    private IBandRestrictionService bandRestrictionsService;
    @Resource(name = "restrictedBinsService")
    private IRestrictedBinsService restrictedBinsService;
    @Resource(name = "queueService")
    private IQueueService queueService;
    @Resource(name = "emailsService")
    private IEmailsService emailsService;
    @Resource(name = "logService")
    private AbstractLogService logService;
    @Resource(name = "partnerService")
    private IPartnerService partnerService;
    @Resource(name = "proposalService")
    private IProposalService proposalService;
    @Resource(name = "rolloverService")
    private IRolloverService rolloverService;
    @Resource(name = "queueEngineWrapper")
    private QueueEngineWrapper queueEngineWrapper;

    @Resource(name = "zipArchiveView")
    private ZipArchiveView zav;

    @RequestMapping(value = "/committees/{committeeId}/queues", method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId,
                              @RequestParam(required = false) final String flash) {
        LOGGER.log(Level.DEBUG, "index");

        final ModelAndView modelAndView = new ModelAndView();
        if (StringUtils.isNotEmpty(flash))
            modelAndView.addObject("flash", flash);

        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        beforeSetup(committeeId, modelAndView);

        final List<Partner> allPartners = partnerService.findAllPartners();
        modelAndView.addObject("partners", allPartners);
        final List<Queue> orderedQueues = committeeService.getAllQueuesForCommittee(committeeId);
        modelAndView.addObject("orderedQueues", orderedQueues);

        modelAndView.setViewName("committees/queues/index");
        addQueuePartnerRolloverChargesMap(modelAndView, orderedQueues);
        addSubaruScheduling(modelAndView, orderedQueues);

        return modelAndView;
    }

    private void addSubaruScheduling(ModelAndView mav, List<Queue> queues) {
        int i = 0;
        Map<Long,Boolean> showSubaru = new HashMap<Long,Boolean>(queues.size());
        Map<Long,Double> subaruClassical = new HashMap<Long,Double>(queues.size());
        Map<Long,Double> subaruQueue = new HashMap<Long,Double>(queues.size());
        for (Queue queue : queues) {
            showSubaru.put(queue.getId(), queue.getSubaruScheduledByQueueEngine());
            if (queue.getSubaruScheduledByQueueEngine()) {
                addSubaruScheduling(queue, i, subaruClassical, subaruQueue);
            }
            i++;
        }
        mav.addObject("showSubaru", showSubaru);
        mav.addObject("subaruClassical", subaruClassical);
        mav.addObject("subaruQueue", subaruQueue);
    }

    private void addSubaruScheduling(Queue queue, int index, Map<Long,Double> subaruClassical, Map<Long,Double> subaruQueue) {
        double subaruClassicalHours = 0.0;
        for (Proposal p : queue.getClassicalProposals()) {
            Proposal initialized = proposalService.getProposal(queue.getCommittee().getId(), p.getId());
            if (initialized.getPartnerAbbreviation().equalsIgnoreCase("Subaru")) {
                subaruClassicalHours += initialized.getTotalRecommendedTime().getDoubleValueInHours();
            }
        }
        double subaruQueueHours = 0.0;
        for (Banding b : queue.getBandings()) {
            Proposal p = proposalService.getProposal(queue.getCommittee().getId(), b.getProposal().getId());
            if (p.getPartnerAbbreviation().equalsIgnoreCase("Subaru")) {
                subaruQueueHours += p.getTotalAwardedTime().getDoubleValueInHours();
            }
        }
        subaruClassical.put(queue.getId(), subaruClassicalHours);
        subaruQueue.put(queue.getId(), subaruQueueHours);
    }

    private void addQueuePartnerRolloverChargesMap(ModelAndView modelAndView, List<Queue> orderedQueues) {
        /* UX-1484 I might be going crazy, and this is clearly a terrible hack, but for this single field of queue,
          queue.rolloverPartnerCharges we are getting null returns from partners that are in the keyset, both equals and hashcode.
          The actual data structure hibernate is using is a persistent map, and it's possible that there is something different
          about the way it identifies presence, but that is equally true for all the other partner charge maps, which seem to
          work perfectly.  We're shipped and I need to get to another bug.  Forgive me.
        */
        final Map<Queue, Map<Partner, RolloverPartnerCharge>> queuePartnerRolloverChargesMap = new HashMap<Queue, Map<Partner, RolloverPartnerCharge>>();
        modelAndView.addObject("queuePartnerRolloverChargesMap", queuePartnerRolloverChargesMap);
        for (Queue q : orderedQueues) {
            final Map<Partner, RolloverPartnerCharge> rolloverPartnerChargeMap = new HashMap<Partner, RolloverPartnerCharge>();
            queuePartnerRolloverChargesMap.put(q, rolloverPartnerChargeMap);
            rolloverPartnerChargeMap.putAll(q.getRolloverPartnerCharges());
        }
    }

    private void beforeSetup(Long committeeId, ModelAndView modelAndView) {
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addUserToModel(modelAndView);
    }

    /**
     * Called when setting up the form for creation of a new queue.
     *
     * @param committeeId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/queues/create", method = RequestMethod.GET)
    public ModelAndView create(@PathVariable final Long committeeId) {
        LOGGER.log(Level.DEBUG, "create");

        final ModelAndView modelAndView = new ModelAndView();

        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        beforeSetup(committeeId, modelAndView);

        final Committee committee = committeeService.getCommitteeForExchangeAnalysis(committeeId);
        final List<Proposal> classicalProposals = committee.getClassicalProposals();
        final Map<String, List<Proposal>> exchangeTimeProposals = committee.getExchangeTimeProposals();
        final List<Proposal> fromKeckProposals = exchangeTimeProposals.get("from-" + Partner.KECK_KEY);
        final List<Proposal> fromSubaruProposals = exchangeTimeProposals.get("from-" + Partner.SUBARU_KEY);
        final List<Proposal> forKeckProposals = exchangeTimeProposals.get("for-" + Partner.KECK_KEY);
        final List<Proposal> forSubaruProposals = exchangeTimeProposals.get("for-" + Partner.SUBARU_KEY);

        final List<Partner> partners = partnerService.findAllPartners();
        final QueueCreationParameters northParams = queueService.getQueueCreationParams(committeeId, Site.NORTH.getDisplayName());
        final QueueCreationParameters southParams = queueService.getQueueCreationParams(committeeId, Site.SOUTH.getDisplayName());

        modelAndView.addObject("fromKeckProposals", fromKeckProposals);
        modelAndView.addObject("fromSubaruProposals", fromSubaruProposals);
        modelAndView.addObject("forKeckProposals", forKeckProposals);
        modelAndView.addObject("forSubaruProposals", forSubaruProposals);

        //Rollover sets
        Set<RolloverSet> northSets = rolloverService.rolloverSetsFor("North");
        modelAndView.addObject("rolloverSetsNorth", northSets);
        Set<RolloverSet> southSets = rolloverService.rolloverSetsFor("South");
        modelAndView.addObject("rolloverSetsSouth", southSets);
        validateRolloverSets(northSets);
        validateRolloverSets(southSets);

        modelAndView.addObject("classicalProposals", classicalProposals);
        modelAndView.addObject("bandRestrictions", bandRestrictionsService.getAllBandRestrictions());
        modelAndView.addObject("partners", partners);
        modelAndView.addObject("northParams", northParams);
        modelAndView.addObject("southParams", southParams);

        modelAndView.addObject("conditionBins", conditionsService.getAllConditionSets());
        modelAndView.addObject("binConfigurations", binConfigurationService.getAllBinConfigurations());
        modelAndView.addObject("committeeId", committeeId);
        modelAndView.addObject("queue", new Queue("", committee));

        modelAndView.setViewName("committees/queues/create");

        return modelAndView;
    }

    private void validateRolloverSets(Set<RolloverSet> sets) {
        for (RolloverSet set : sets) {
            validateRolloverSet(set);
        }
    }

    private void validateRolloverSet(RolloverSet set) {
        Validate.notNull(set);
        Validate.notNull(set.getName());
        Validate.notNull(set.getSite());
        Validate.notNull(set.getObservations());
        for (AbstractRolloverObservation o : set.getObservations()) {
            Validate.notNull(o);
            Validate.notNull(o.getObservationTime());
        }

    }


    @SuppressWarnings("serial")
    @RequestMapping(value = "/committees/{committeeId}/queues/create", method = RequestMethod.POST)
    public ModelAndView createPost(@PathVariable final Long committeeId,

//    		// Initial parameters
                                   @RequestParam("site") final String site,
                                   @RequestParam("total-time-available") final int totalTimeAvailable,
                                   @RequestParam("partner-initial-pick") final Long partnerInitalPick,
                                   @RequestParam("condition-bins") final Long conditionBinsId,
                                   @RequestParam("bin-configurations") final Long binConfigurationsId,
                                   // Band configuration parameters
                                   @RequestParam("band-1-threshold") final int band1Threshold,
                                   @RequestParam("band-2-threshold") final int band2Threshold,
                                   @RequestParam(value = "band-3-threshold", required = false, defaultValue = "100") Integer band3Threshold,
                                   @RequestParam(value = "band-3-conditions-threshold", required = false, defaultValue = "60") Integer band3ConditionsThreshold,
                                   @RequestParam(value = "band-3-for-all-over-threshold", required = false, defaultValue = "false") Boolean band3ForAllOverThreshold,
                                   // Notes and name
                                   @RequestParam("queue-name") final String queueName,
                                   @RequestParam(value = "queue-notes", required = false) final String queueNotes,
                                   // Restrictions
                                   @RequestParam(value = "band-restrictions", required = false) final Long[] bandRestrictionIds,
                                   @RequestParam(value = "restricted-bins", required = false) final Long[] restrictedBinIds,
                                   // Exchange
                                   @RequestParam(value = "exchange-partners-in-order", required = true) final String[] partnerNamesInExchangeOrder,
                                   @RequestParam(value = "keck-ex-participating", required = true) final Double[] keckExchangeParticipants,
                                   @RequestParam(value = "subaru-ex-participating", required = true) final Double[] subaruExchangeParticipants,
                                   @RequestParam(value = "exchange-accepted", required = false) final Long[] exchangeProposalsAccepted,
                                   // Classical
                                   @RequestParam(value = "classical-accepted", required = false) final Long[] classicalProposalsAccepted,
                                   //Rollovers
                                   @RequestParam(value = "rollovers", required = false) final Long rolloverId,
                                   @RequestParam(value = "queue-overfill-limit", required = false) final Integer queueOverfillLimit, // ITAC-441 Increase filling factor for 2012A and make it configurable
                                   @RequestParam(value = "subaruStrategyUseQueue", required = true) final Boolean useQueue,
                                   //Partner quanta
                                   ServletRequest servletRequest // Used for partner adjustments and exchange charges
    ) {
        LOGGER.log(Level.DEBUG, "createPost");

        final Map parameterMap = servletRequest.getParameterMap();
        final List<String> creationParameters = new ArrayList<String>();
        LOGGER.info("QUEUE CREATION PARAMETERS --V");

        for (Object key : parameterMap.keySet()) {
            creationParameters.add(key + " => " + (Arrays.toString((String[]) parameterMap.get(key))));
        }
        Collections.sort(creationParameters);
        for (String s : creationParameters) {
            LOGGER.info(s);
        }

        final Map<String, Float> adjustments = parametersToMap(parameterMap, "partnerAdjustment");
        final Map<String, Float> exchanges = parametersToMap(parameterMap, "partnerExchange");
        LOGGER.log(Level.DEBUG, "adjustments:" + adjustments);
        LOGGER.log(Level.DEBUG, "exchanges:" + exchanges);

        //Put together names and exchange values
        Validate.isTrue(partnerNamesInExchangeOrder.length == keckExchangeParticipants.length, "Partner names length not == Keck exchange partner names");
        Validate.isTrue(partnerNamesInExchangeOrder.length == subaruExchangeParticipants.length, "Partner names length not == Subaru` exchange partner names");
        Map<String, Double> keckExchanges = new HashMap<String, Double>();
        Map<String, Double> subaruExchanges = new HashMap<String, Double>();
        for (int i = 0; i < partnerNamesInExchangeOrder.length; i++) {
            String partnerName = partnerNamesInExchangeOrder[i];
            keckExchanges.put(partnerName, keckExchangeParticipants[i]);
            subaruExchanges.put(partnerName, subaruExchangeParticipants[i]);
        }

        final ModelAndView modelAndView = new ModelAndView();
        long begin = System.currentTimeMillis();
        final Committee committee = committeeService.getCommittee(committeeId);
        long end = System.currentTimeMillis();
        LOGGER.info("committeeService.getCommittee:" + (end - begin) + " ms");

        // Read the restriction bin values from the request
        Map <Long, Integer> restrictedBins = Maps.newHashMap();
        if (restrictedBinIds != null) {
            for (Long r:restrictedBinIds) {
                restrictedBins.put(r, Integer.parseInt(Optional.fromNullable(servletRequest.getParameter("restricted-bins-" + r + "-value")).or("0").toString()));
            }
        }

        try {
            begin = System.currentTimeMillis();
            final Queue queue = queueService.createQueue(committeeId,
                    site,
                    totalTimeAvailable,
                    partnerInitalPick,
                    conditionBinsId,
                    binConfigurationsId,
                    band1Threshold,
                    band2Threshold,
                    band3Threshold,
                    band3ConditionsThreshold,
                    band3ForAllOverThreshold,
                    queueName,
                    queueNotes,
                    bandRestrictionIds,
                    restrictedBins,
                    keckExchanges,
                    subaruExchanges,
                    useQueue,
                    exchangeProposalsAccepted,
                    classicalProposalsAccepted,
                    rolloverId,
                    adjustments,
                    exchanges,
                    queueOverfillLimit);
            end = System.currentTimeMillis();

            LOGGER.info("queueService.createQueue:" + (end - begin) + " ms");
            LOGGER.log(Level.DEBUG, queue.getBinConfiguration().toString());
            LOGGER.log(Level.DEBUG, queue.getConditionSet().toString());

            begin = System.currentTimeMillis();
            queueEngineWrapper.fill(queue, committeeService, logService);
            end = System.currentTimeMillis();
            LOGGER.info("edu.gemini.tac.qservice.impl.QueueServiceImpl.fill:" + (end - begin) + " ms");

            begin = System.currentTimeMillis();
            queueService.saveQueue(queue);
            end = System.currentTimeMillis();
            LOGGER.info("queueService.saveQueue:" + (end - begin) + " ms");

            final LogEntry queueLog = logService.createNewForQueue("Created", new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.PROPOSAL_QUEUE_GENERATED);
            }}, queue);
            final LogEntry committeeLog = logService.createNewForCommittee(queueName + " created", new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.PROPOSAL_QUEUE_GENERATED);
            }}, committee);
            final Queue refreshedQueue = queueService.getQueue(queue.getId()); // reload to get ids and everything
            logService.logBandedProposals(refreshedQueue);

            logService.addLogEntry(queueLog);
            if (queueNotes != null) {
                logService.addNoteToEntry(queueLog, new LogNote(queueNotes));
            }
            logService.addLogEntry(committeeLog);
        } catch (ConstraintViolationException cve) {
            LOGGER.log(Level.WARN, "User tried to name queue with already existing name: " + cve.toString());
            modelAndView.addObject("flash", queueName + " is already used in this committee.  Please select another name.");
            logService.createNewForCommittee("Unable to create an additional queue with nanme " + queueName, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_ERROR);
            }}, committee);
        }

        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/queues");
        return modelAndView;
    }

    private Map<String, Float> parametersToMap(final Map<String, String[]> parameterMap, final String prefix) {
        final Set<String> allKeys = parameterMap.keySet();
        final Set<String> interestingKeys = new HashSet<String>();
        final Map<String, Float> interestingMap = new HashMap<String, Float>();
        for (String key : allKeys) {
            if (key.contains(prefix + "-"))
                interestingKeys.add(key);
        }

        for (String key : interestingKeys) {
            final String[] s = (String[]) parameterMap.get(key);
            final Float value = Float.valueOf(s[0]);
            interestingMap.put(key.substring(key.indexOf('-') + 1), value);
        }

        return Collections.unmodifiableMap(interestingMap);
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}", method = RequestMethod.GET)
    public ModelAndView show(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "show");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);
        modelAndView.addObject("partners", partnerService.findAllPartners());

        modelAndView.setViewName("committees/queues/show");

        prepareModelAndViewForSorting(modelAndView, queue);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/bulk-edit", method = RequestMethod.GET)
    public ModelAndView bulkEdit(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "show");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);
        // prepare a collection with all proposals of interest for this queue
        final List<Proposal> proposals = new ArrayList<Proposal>();
        for (Banding b : queue.getBandings()) {
            if (!b.isJointComponent()) {
                proposals.add(b.getProposal());
            }
        }
        proposals.addAll(queue.getCopyOfClassicalProposalsSorted());
        proposals.addAll(queue.getExchangeProposals());

        // add data to model-view
        modelAndView.addObject("partners", partnerService.findAllPartners());
        modelAndView.addObject("proposals", proposals);
        modelAndView.setViewName("committees/queues/bulk-edit");

        return modelAndView;
    }

    /**
     * Handles edited data related to the proposal
     * information.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/{proposalId}", method = RequestMethod.POST)
    public void edit(
            @PathVariable final Long committeeId,
            @PathVariable final Long proposalId,
            @RequestParam(value = "class", required = true) final String targetType,
            @RequestParam(value = "field", required = false) final String field,
            @RequestParam(value = "value", required = false) final String value,
            @RequestParam(value = "naturalId", required = false) final String naturalId,
            HttpServletResponse response
    ) {
        doProposalEdit(committeeId, proposalId, targetType, field, value, naturalId, response);
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/reports", method = RequestMethod.GET)
    public ModelAndView reports(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "reports");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.setViewName("committees/queues/reports");

        return modelAndView;
    }

    /**
     * Creates report data and returns a model and view object for queue specific reports.
     *
     * @param queueId            the queueId for which the report has to be created
     * @param reportName         the name of the report (must match the name of the jrxml template)
     * @param renderType         output format of report (csv, html, xls or pdf)
     * @param httpServletRequest the servlet request
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/reports/{reportName}.{renderType}", method = RequestMethod.GET)
    public ModelAndView createQueueReport(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId,
            @PathVariable final String reportName,
            @PathVariable final String renderType,
            @RequestParam(required = false) final String site,
            @RequestParam(required = false) final String partner,
            @RequestParam(required = false) final String instrument,
            @RequestParam(required = false) final boolean laserOpsOnly,
            @RequestParam(required = false) final boolean ignoreErrors,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) {

        // get report by name, collect report data for queue and render it
        final Queue queue = queueService.getQueue(queueId);
        Report report = getReport(reportName, site, partner, instrument, laserOpsOnly, httpServletRequest);
        reportService.collectReportDataForQueue(report, committeeId, queueId);

        if (report.isEmpty()) {
            ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
            modelAndView.setViewName("committees/queues/reports");
            modelAndView.addObject("reportIsEmpty", true);
            return modelAndView;
        } else if (report.hasErrors() && !ignoreErrors) {
            ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
            modelAndView.setViewName("committees/queues/reports");
            modelAndView.addObject("reportUrl", getRetryReportUrl(httpServletRequest));
            modelAndView.addObject("errors", report.getErrors());
            return modelAndView;
        } else {
            return renderReport(report, renderType, httpServletRequest, httpServletResponse);
        }
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/constraints", method = RequestMethod.GET)
    public ModelAndView constraints(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "constraints");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.addObject("partners", partnerService.findAllPartners());
        List<Queue> queues = new ArrayList<Queue>();
        queues.add(queue);
        addQueuePartnerRolloverChargesMap(modelAndView, queues);

        modelAndView.setViewName("committees/queues/constraints");

        return modelAndView;
    }

    private PartnerPercentage getPartnerPercentageOrNull(Partner partner, Queue queue) {
        for (PartnerPercentage pp : queue.getPartnerPercentages()) {
            if (pp.getPartner().equals(partner)) {
                return pp;
            }
        }
        return null;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/manual", method = RequestMethod.GET)
    public ModelAndView manual(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "manual");

        final ModelAndView modelAndView = queueModelAndView(committeeId, queueId);

        // for forced-adds we need to know which proposals are *not* part of the queue
        final Queue queue = queueService.getQueue(queueId);
        Set<Long> queuedIds = new HashSet<Long>(200);
        for (Banding b : queue.getBandings()) {
            queuedIds.add(b.getProposal().getId());
        }
        for (Proposal p : queue.getClassicalProposals()) {
            queuedIds.add(p.getId());
        }
        List<Proposal> allProposals = committeeService.getAllProposalsForCommittee(committeeId);
        List<Proposal> notInQueueProposals = new ArrayList<Proposal>(200);
        for (Proposal p : allProposals) {
            if (p.belongsToSite(queue.getSite()) && !queuedIds.contains(p.getId())) {
                notInQueueProposals.add(p);
            }
        }
        modelAndView.addObject("notInQueueProposals", notInQueueProposals);
        modelAndView.addObject("targetTab", 0);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/manual/banding/{bandingId}/band/{band}", method = RequestMethod.GET)
    public ModelAndView manual(@PathVariable final Long committeeId,
                               @PathVariable final Long queueId,
                               @PathVariable final Long bandingId,
                               @PathVariable final Integer band) {
        LOGGER.log(Level.DEBUG, "manual");
        final Queue queue = queueService.getQueueWithoutData(queueId);
        final Banding banding = queueService.getBanding(bandingId);
        final Proposal proposal = proposalService.getProposal(committeeId, banding.getProposal().getId());

        final ScienceBand originalBand = banding.getBand();
        final String proposalLink = UrlFor.getProposalLink(proposal);
        final String queueLink = UrlFor.getQueueLink(committeeId, queueId, queue.getName());

        String message = null;

        if (band.longValue() == ScienceBand.CLASSICAL.getRank()) {
            queueService.convertBandingToClassical(queueId, bandingId);

            message = String.format("Manually adjusted queue proposal %s to classical from %s in %s.", proposalLink, originalBand.getDescription(), queueLink);
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_REBAND);
                add(LogEntry.Type.QUEUE_OBSERVING_MODE_CHANGE);
            }}, queue);
            logService.addLogEntry(queueLog);
        } else if (band.longValue() == ScienceBand.EXCHANGE.getRank()) {
            queueService.convertBandingToExchange(queueId, bandingId);

            message = String.format("Manually adjusted queue proposal %s to exchange from %s in %s.", proposalLink, originalBand.getDescription(), queueLink);
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_REBAND);
                add(LogEntry.Type.QUEUE_OBSERVING_MODE_CHANGE);
            }}, queue);
            logService.addLogEntry(queueLog);
        } else {
            queueService.setBandingBand(bandingId, band);

            message = String.format("Adjusted band for proposal %s to %s from %s in %s.", proposalLink, ScienceBand.lookupFromRank(band).getDescription(), originalBand.getDescription(), queueLink);
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_REBAND);
            }}, queue);
            logService.addLogEntry(queueLog);
        }

        final ModelAndView modelAndView = manual(committeeId, queueId);
        modelAndView.addObject("targetTab", band - 1);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/manual/banding/{bandingId}/remove", method = RequestMethod.GET)
    public ModelAndView removeBanding(@PathVariable final Long committeeId,
                                      @PathVariable final Long queueId,
                                      @PathVariable final Long bandingId) {
        LOGGER.log(Level.DEBUG, "removeBanding");
        final Banding banding = queueService.getBanding(bandingId);
        final Proposal proposal = proposalService.getProposal(committeeId, banding.getProposal().getId());
        final Queue queueWithoutData = queueService.getQueueWithoutData(queueId);
        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        final Submission primary = phaseIProposal.getPrimary();
        final SubmissionReceipt receipt = primary.getReceipt();
        final ScienceBand originalBand = banding.getBand();

        final String proposalLink = UrlFor.getProposalLink(proposal);
        final String queueLink = UrlFor.getQueueLink(committeeId, queueId, queueWithoutData.getName());

        queueService.deleteBandingFromQueue(queueId, banding);

        final String message = String.format("Manually removed queue proposal %s from %s was in %s.", proposalLink, queueLink, originalBand.getDescription());
        final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.QUEUE_FORCE_REMOVE);
        }}, queueWithoutData);
        logService.addLogEntry(queueLog);

        final ModelAndView modelAndView = manual(committeeId, queueId);
        modelAndView.addObject("targetTab", 6);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/manual/proposal/{proposalId}/remove", method = RequestMethod.GET)
    public ModelAndView removeProposal(@PathVariable final Long committeeId,
                                       @PathVariable final Long queueId,
                                       @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, "removeProposal");
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final Queue queueWithoutData = queueService.getQueueWithoutData(queueId);
        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        final Submission primary = phaseIProposal.getPrimary();
        final SubmissionReceipt receipt = primary.getReceipt();

        final String proposalLink = UrlFor.getProposalLink(proposal);
        final String queueLink = UrlFor.getQueueLink(committeeId, queueId, queueWithoutData.getName());

        queueService.removeProposalFromQueue(queueId, proposalId);

        final String message = String.format("Manually removed proposal %s from %s.", proposalLink, queueLink);
        final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.QUEUE_FORCE_REMOVE);
        }}, queueWithoutData);
        logService.addLogEntry(queueLog);

        final ModelAndView modelAndView = manual(committeeId, queueId);
        modelAndView.addObject("targetTab", 6);

        return modelAndView;

    }

    private ModelAndView queueModelAndView(Long committeeId, Long queueId) {
        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.setViewName("committees/queues/manual");
        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/manual/band/{targetBandRank}/proposals/{proposalId}", method = RequestMethod.GET)
    public ModelAndView addProposalToBand(@PathVariable final Long committeeId,
                                          @PathVariable final Long queueId,
                                          @PathVariable final Integer targetBandRank,
                                          @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, String.format("addProposalToBand(%d, %d, %d, %d)", committeeId, queueId, targetBandRank, proposalId));

        final Queue queue = queueService.getQueueWithoutData(queueId);
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final PhaseIProposal phaseIProposal = proposal.getPhaseIProposal();
        final Submission primary = phaseIProposal.getPrimary();
        final SubmissionReceipt receipt = primary.getReceipt();

        final ScienceBand newBand = ScienceBand.lookupFromRank(targetBandRank);
        final String proposalLink = UrlFor.getProposalLink(proposal);
        final String queueLink = UrlFor.getQueueLink(committeeId, queueId, queue.getName());

        String flash = null;

        if (proposal.isClassical()) {
            queueService.convertClassicalToBanding(queueId, proposal, ScienceBand.lookupFromRank(targetBandRank.intValue()));

            final String message = String.format("Manually converted formerly classical proposal %s in queue %s to %s.", proposalLink, queueLink, newBand.getDescription());
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_REBAND);
            }}, queue);
            logService.addLogEntry(queueLog);
        } else if (proposal.getExchangeFrom() != null) {
            queueService.convertExchangeProposalToBanding(queueId, proposal, ScienceBand.lookupFromRank(targetBandRank.intValue()));

            final String message = String.format("Manually converted former exchange proposal %s in queue %s to %s.", proposalLink, queueLink, newBand.getDescription());
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_REBAND);
            }}, queue);
            logService.addLogEntry(queueLog);
        } else if (newBand.getRank() == ScienceBand.CLASSICAL.getRank()) {
            //Only GeminiNormalProposals can be converted to classical
            if (proposal.getPhaseIProposal() instanceof GeminiNormalProposal) {
                proposal.setClassical(true);
                queueService.addClassicalProposalToQueue(queueId, proposal);

                final String message = String.format("Manually converted unqueued proposal %s to classical.", proposalLink);
                final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                    add(LogEntry.Type.QUEUE_REBAND);
                }}, queue);
                logService.addLogEntry(queueLog);
            } else {
                //Not a GeminiNormalProposal
                flash = "Could not convert that proposal into a Classical proposal. (Unsupported type: " + proposal.getPhaseIProposal().getClass().toString() + ")";
            }
        } else {
            final Banding banding = new Banding(proposal, ScienceBand.lookupFromRank(targetBandRank));
            queueService.addBandingToQueue(queueId, banding);

            final String message = String.format("Manually added proposal %s in queue %s to %s.", proposalLink, queueLink, newBand.getDescription());
            final LogEntry queueLog = logService.createNewForQueue(message, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.QUEUE_FORCE_ADD);
            }}, queue);
            logService.addLogEntry(queueLog);
        }

        final ModelAndView modelAndView = manual(committeeId, queueId);
        modelAndView.addObject("targetTab", targetBandRank - 1);
        if (flash != null) {
            modelAndView.addObject("flash", flash);
        }

        return modelAndView;

    }


    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/finalize", method = RequestMethod.GET)
    public ModelAndView finalizeQueue(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "finalizeQueue");

        queueService.finalizeQueue(queueId);
        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);
        modelAndView.setViewName("committees/queues/show");

        prepareModelAndViewForSorting(modelAndView, queue);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/unfinalize", method = RequestMethod.GET)
    public ModelAndView unfinalizeQueue(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "unFinalizeQueue");

        queueService.unFinalizeQueue(queueId);
        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);
        modelAndView.setViewName("committees/queues/show");

        prepareModelAndViewForSorting(modelAndView, queue);

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/exportSkeletons", method = RequestMethod.POST)
    public ModelAndView exportSkeletons(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "exportSkeletons");

        final Collection<QueueHibernateService.SkeletonResult> results = queueService.exportSkeletons(queueId, pdfFolder);
        int failedCount = 0;
        int successfulCount = 0;
        for (QueueHibernateService.SkeletonResult r : results) {
            if (r.isSuccessful()) successfulCount++;
            else failedCount++;
        }

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.addObject("succesfulResults", successfulCount);
        modelAndView.addObject("failedResults", failedCount);
        modelAndView.addObject("results", results);
        modelAndView.setViewName("committees/queues/exportSkeletons");

        return modelAndView;
    }

    // -------- EMAIL RELATED STUFF

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/showEmails", method = RequestMethod.GET)
    public ModelAndView listEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "index");

        List<Email> emails = emailsService.getEmailsForQueue(queueId);

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.addObject("emails", emails);

        modelAndView.setViewName("committees/emails/list");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/email", method = RequestMethod.POST)
    public ModelAndView editEmail(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId,
            @RequestParam final String submit,
            @RequestParam final Long emailId,
            @RequestParam final String address,
            @RequestParam final String content
    ) {
        LOGGER.log(Level.DEBUG, "email");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);

        if (submit.equals("save")) {
            emailsService.updateEmail(emailId, address, content);
        } else if (submit.equals("send")) {
            emailsService.sendEmail(emailId, false);
        } else if (submit.equals("resend")) {
            emailsService.sendEmail(emailId, true);
        } else if (submit.equals("cancel")) {
            // don't do anything, just reset the whole page
        } else {
            throw new IllegalArgumentException("unknown submit value " + submit);
        }

        modelAndView.addObject("emails", emailsService.getEmailsForQueue(queueId));

        modelAndView.setViewName("committees/emails/list");

        final String message = getUser().getName() + " modified email functionality with action " + submit;
        logService.addLogEntry(logService.createNewForCommittee(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.EMAIL);
        }}, committeeService.getCommittee(committeeId)));

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/createEmails", method = RequestMethod.POST)
    public ModelAndView createEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "createEmails");

        emailsService.createEmailsForQueue(queueId);

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.addObject("emails", emailsService.getEmailsForQueue(queueId));
        modelAndView.setViewName("committees/emails/list");

        final String message = getUser().getName() + " creating emails.";
        logService.addLogEntry(logService.createNewForCommittee(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.EMAIL);
        }}, committeeService.getCommittee(committeeId)));


        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/{action}", method = RequestMethod.GET)
    public ModelAndView actionConfirm(@PathVariable final Long committeeId, @PathVariable final Long queueId, @PathVariable final String action) {
        LOGGER.log(Level.DEBUG, action + "Confirm");
        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);
        modelAndView.addObject("action", action);
        modelAndView.addObject("confirmTitle", confirmTitles.get(action));
        modelAndView.addObject("confirmYes", "Yes");
        modelAndView.addObject("confirmNo", "No");
        modelAndView.setViewName("committees/queues/actionConfirm");
        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/sendAllEmails", method = RequestMethod.POST)
    public ModelAndView sendAllEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        return sendEmails(committeeId, queueId, null);
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/sendPIEmails", method = RequestMethod.POST)
    public ModelAndView sendPIEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        return sendEmails(committeeId, queueId, "PI");
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/sendNGOEmails", method = RequestMethod.POST)
    public ModelAndView sendNGOEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        return sendEmails(committeeId, queueId, "NGO");
    }

    private ModelAndView sendEmails(final Long committeeId, final Long queueId, final String subjectFilter) {
        LOGGER.log(Level.DEBUG, "sendEmails");

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, false);

        // send every email in its own transaction to make sure that if server fails / dies during sending an email
        // no more than one email is possibly resent (if transaction rolls back after email has been sent we won't
        // know that and assume the emails has not been sent).
        List<Email> emails = emailsService.getEmailsForQueue(queueId);
        for (Email email : emails) {
            if (subjectFilter == null || email.getSubject().contains(subjectFilter)) {
                emailsService.sendEmail(email.getId(), false);
            }
        }

        // reload all emails with the new information (timestamps, error messages...)
        modelAndView.addObject("emails", emailsService.getEmailsForQueue(queueId));

        modelAndView.setViewName("committees/emails/list");

        final String message = getUser().getName() + " sending " + subjectFilter + " emails.";
        logService.addLogEntry(logService.createNewForCommittee(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.EMAIL);
        }}, committeeService.getCommittee(committeeId)));


        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/deleteEmails", method = RequestMethod.POST)
    public ModelAndView deleteEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "deleteEmails");

        emailsService.deleteEmails(queueId);

        final Queue queue = queueService.getQueue(queueId);
        final ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);

        modelAndView.setViewName("committees/queues/show");

        final String message = getUser().getName() + " deleting emails.";
        logService.addLogEntry(logService.createNewForCommittee(message, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.EMAIL);
        }}, committeeService.getCommittee(committeeId)));


        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/exportEmails", method = RequestMethod.GET)
    public ModelAndView exportEmails(@PathVariable final Long committeeId, @PathVariable final Long queueId, HttpServletResponse response) throws Exception {
        LOGGER.log(Level.DEBUG, "exportEmails");

        final byte[] bytes = emailsService.exportEmails(queueId);
        final StringBuilder fileName = new StringBuilder();
        fileName.append("ITAC-Emails-Queue-");
        fileName.append(queueId);
        fileName.append(".txt");

        response.setContentType("application/xml");
        response.setContentLength(bytes.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(bytes);

        return null;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/rollover-eligible", method = RequestMethod.GET)
    public ModelAndView rolloverEligibleGet(@PathVariable final Long committeeId, @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "rolloverEligibleGet(" + committeeId + ")");
        final Queue queue = queueService.getQueue(queueId);
        ModelAndView modelAndView = prepareModelAndView(queue, committeeId, true);

        List<Proposal> proposals = new ArrayList<Proposal>();
        for (Banding b : queue.getBandings()) {
            //N.B. b.getBand().equals(ScienceBand.RANK_ONE) DOES NOT work below
            if (b.getBand().getRank() == 1) {
                final Proposal proposal = b.getProposal();
                proposals.add(proposal);
            }
        }
        modelAndView.addObject("proposals", proposals);

        modelAndView.setViewName("committees/proposals/rollovers/eligible");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/rollover-eligible", method = RequestMethod.PUT)
    public ModelAndView rolloverEligibleSet
            (
                    @PathVariable final Long committeeId,
                    @PathVariable final Long queueId,
                    @RequestParam final Long proposalId,
                    @RequestParam final boolean isRolloverEligible) {
        LOGGER.log(Level.DEBUG, "rolloverEligibleSet(" + proposalId + ", " + isRolloverEligible + ")");

        proposalService.setRolloverEligibility(proposalId, isRolloverEligible);

        final String logMessage = getUser().getName() + " set proposal " + proposalId + " to " + (isRolloverEligible ? " rollover eligible" : "rollover ineligible");
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_ROLLOVER_ELIGIBILITY_CHANGED);
        }}, committeeService.getCommittee(committeeId)));

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("proposalId", proposalId);
        modelAndView.addObject("isRolloverEligible", isRolloverEligible);
        modelAndView.setViewName("committees/proposals/rollovers/rolloverEligibleJson");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/force-add", method = RequestMethod.GET)
    public ModelAndView forceAddGet(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "forceAddGet(" + committeeId + "," + queueId);
        ModelAndView mav = prepareModelAndView(queueService.getQueue(queueId), committeeId, false);
        mav.setViewName("committees/queues/forceAddList");
        return mav;
    }

    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/proposals.xml", method = RequestMethod.GET)
    public ModelAndView exportXml(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId) {
        LOGGER.log(Level.DEBUG, "exportXML(" + committeeId + "," + queueId);
        final Queue queue = queueService.getQueue(queueId);
        final List<Long> proposalIds = new ArrayList<Long>();
        for (Banding b : queue.getBandings()) {
            if (!b.isJointComponent())
                proposalIds.add(b.getProposal().getId());
        }
        for (Proposal p : queue.getClassicalProposals()) {
            proposalIds.add(p.getId());
        }
        for (Proposal p : queue.getExchangeProposals()) {
            proposalIds.add(p.getId());
        }

        ModelAndView mav = prepareModelAndView(queueService.getQueue(queueId), committeeId, false);

        Map<String, String> pages = exportToXml(committeeId, proposalIds);
        mav.setViewName("committees/proposals/export/export-html-do");

        ModelAndView wrapper = new ModelAndView(zav);
        wrapper.addObject("pages", pages);
        return wrapper;
    }

    private Map<String, String> exportToXml(final Long committeeId, final List<Long> proposalIds) {
        Map<String, String> pages = new HashMap<String, String>();
        final List<String> errors = new ArrayList<String>();
        for (Long proposalId : proposalIds) {
            byte[] proposalAsXml = null;
            try {
                final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
                proposalAsXml = proposalService.getProposalAsXml(proposal);
                final String filename = proposal.getPartner().getAbbreviation() + "-" + proposal.getPartnerReferenceNumber().replace(".", "_").replace("/", "_").replace("\\", "_").replace("\"", "_") + "-" + proposal.getId() + ".xml";
                zav.addToArchive(filename, proposalAsXml);
            } catch (Exception e) {
                final String exceptionMessage = String.format("Unable to export proposal " + proposalId + " as XML: %s", e.getMessage());
                LOGGER.log(Level.ERROR, exceptionMessage);
                errors.add(exceptionMessage);
            }
        }
        zav.addToArchive("errors.txt", StringUtils.join(errors, "\n"));

        return pages;
    }

    //I think this is a POST because you're putting this proposal into the band for the first time -- it's a NEW banding
    @RequestMapping(value = "/committees/{committeeId}/queues/{queueId}/bandings/{bandRank}/{proposalId}", method = RequestMethod.POST)
    @PreAuthorize("isInRole('ROLE_QUEUE_WRITER')")
    public String forceAddToBand(
            @PathVariable final Long committeeId,
            @PathVariable final Long queueId,
            @PathVariable final int bandRank,
            @PathVariable final Long proposalId
    ) {
        String logMsg = String.format("forceAddToBand(%d, %d, %d, %d)", committeeId, queueId, bandRank, proposalId);
        LOGGER.log(Level.INFO, logMsg);
        Queue queue = queueService.getQueue(queueId);
        logService.addLogEntry(logService.createNewForQueue(logMsg, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.QUEUE_FORCE_ADD);
        }}, queue));


        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        if (proposal.isClassical()) {
            proposalService.setClassical(proposal, false);

            Validate.isTrue(!proposalService.getProposal(committeeId, proposalId).isClassical());
        }


        queue.remove(proposal);
        final ScienceBand band = ScienceBand.lookupFromRank(bandRank);
        final Banding newBanding = new Banding(proposal, band);
        //Set merge index
        newBanding.setMergeIndex(Integer.MAX_VALUE);
        queueService.addBandingToQueue(queueId, newBanding);

        return "redirect:/tac/committees/" + committeeId + "/queues/" + queueId;
    }

    private ModelAndView prepareModelAndView(Queue queue, Long committeeId, boolean loadProposals) {
        final ModelAndView modelAndView = new ModelAndView();
        final Queue finalizedQueue = queueService.getFinalizedQueue(committeeId, queue.getSite());
        final boolean existsFinalizedQueue = finalizedQueue != null ? true : false;
        final boolean isFinalizedQueue = finalizedQueue != null ? finalizedQueue.getId().equals(queue.getId()) : false;
        final boolean hasEmails = emailsService.queueHasEmails(queue.getId());

        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getResourceLinks(String.valueOf(committeeId), queue.getId().toString(), isFinalizedQueue, hasEmails, existsFinalizedQueue));
        addUserToModel(modelAndView);

        if (loadProposals) {
            List<Proposal> proposals = new ArrayList<Proposal>();
            for (Banding banding : queue.getBandings()) {
                proposals.add(banding.getProposal());
            }
            proposals.addAll(queue.getCopyOfClassicalProposalsSorted());
            ProposalAccordionHelper.populateModelForProposalAccordion(proposalService, modelAndView, proposals); // TODO: Consider switching this over to the non-service, non LIE valididating verison.
        }

        modelAndView.addObject("queue", queue);
        modelAndView.addObject("user", getUser());

        return modelAndView;
    }

    private void prepareModelAndViewForSorting(ModelAndView modelAndView, Queue queue) {
        //For sorting
        Set<Proposal> proposals = new HashSet<Proposal>();
        Map<Long, Set<String>> ibp = new HashMap<Long, Set<String>>();
        for (Banding b : queue.getBandings()) {
            Proposal p = b.getProposal();
            proposals.add(p);
            Set<String> instruments = proposalService.getInstrumentNamesForProposal(p);
            Set<String> instruments_by_band = new HashSet<String>();
            for (String instrument : instruments) {
                String instrument_by_band = Long.toString(b.getBand().getRank()) + instrument;
                instruments_by_band.add(instrument_by_band);
            }
            ibp.put(p.getId(), instruments_by_band);
        }
        modelAndView.addObject("proposals", proposals);
        modelAndView.addObject("instruments_by_band", ibp);
    }

    protected void addQueueToModel(final Long queueId, final ModelAndView modelAndView) {
        final Queue queue = queueService.getQueue(queueId);
        modelAndView.addObject("queue", queue);
    }

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.QUEUES;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }
}
