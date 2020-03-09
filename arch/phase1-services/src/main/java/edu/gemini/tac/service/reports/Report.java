package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.util.*;

/**
 * Base class for reports.
 */
public abstract class Report {
    private static final Logger LOGGER = Logger.getLogger(Report.class.getName());

    // a bunch of filters that are supported by all the reports
    // if we need more / report specific filters in the future we will have to revisit this
    private String reportTarget;
    private String siteFilter;
    protected String partnerFilter;
    private String instrumentFilter;
    private Boolean laserOpsOnlyFilter;

    private String name;
    private List<ReportBean> data;
    private String url;
    private List<Error> errors;

    // context information
    private TimeAmount contextTotalObservationTime = null;
    private Queue contextQueue = null;
    private Banding contextBanding = null;

    /**
     * Constructs a new empty report.
     */
    public Report(String name) {
        this.name = name;
        this.data = new ArrayList<ReportBean>();
        this.url = "http://localhost";
        this.errors = new ArrayList<Error>();
    }

    /**
     * Collects all relevant report data for a given committee.
     *
     * @param session
     * @param committeeId
     * @return
     */

    public List<ReportBean> collectDataForCommittee(Session session, Long committeeId) {
        // find all proposals for this committee
        @SuppressWarnings("unchecked")
        List<Proposal> proposals = session.createQuery(
                "from Proposal p " +
                        "join fetch p.phaseIProposal p1p " +
                        " where p.committee = :committeeId").
                setLong("committeeId", committeeId).
                list();

        Validate.notEmpty(proposals, "Could not find any proposals in committee " + committeeId);
        Set<Long> p1pIds = new HashSet<Long>();
        for (Proposal p : proposals) {
            p1pIds.add(p.getPhaseIProposal().getId());
        }
        preLoadObservations(session, p1pIds);

        // set report target; get committee name from first proposal (if possible)
        reportTarget = "Committee " +
                (proposals.size() > 0 ?
                        proposals.get(0).getCommittee().getName() :
                        "");

        //Collections.sort(proposals, new LastNameComparator());

        // process proposals for committee
        for (Proposal proposal : proposals) {
            // don't evaluate components to avoid duplication of data
            // by accounting data from components AND joints
            if (!proposal.isJointComponent()) {
                // evaluate data from this proposal; no band information since we are not looking at a queue
                collectDataForProposal(proposal);
            } else if (proposal.isJointComponent() && partnerFilter != null) { // ITAC-623 Completion of partial NTAC implementation, "show joints for *all* partners involved"
                collectDataForProposal(proposal);
            }
        }

        boilBeans();

        return getData();
    }

    private List<Proposal> sortProposalsByPILastName(List<Proposal> proposals) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Collects all relevant report data for a given queue.
     *
     * @param session
     * @param committeeId
     * @param queueId
     * @return
     */
    public List<ReportBean> collectDataForQueue(Session session, Long committeeId, Long queueId) {
        // find all proposals for this queue
        Queue queue = (Queue) session.createQuery(
                "from Queue q where q.committee = :committeeId and q.id = :queueId").
                setLong("committeeId", committeeId).
                setLong("queueId", queueId).
                uniqueResult();
        Validate.notNull(queue);

        // get all ids of banded phase1 proposals for this queue
        Set<Long> p1pIds = new HashSet<Long>();
        for (Banding b : queue.getBandings()) {
            p1pIds.add(b.getProposal().getPhaseIProposal().getId());
        }
        preLoadObservations(session, p1pIds);

        // set report target; get committee name from first proposal (if possible)
        contextQueue = queue;
        reportTarget = "Queue " + queue.getName();

        // process all proposals that are "banded" for the queue
        SortedSet<Banding> bandings = queue.getBandings();
        for (Banding banding : bandings) {
            // calculate total observation time for this proposal and make number available as context info
            contextTotalObservationTime = calculateTotalObservationTime(banding.getProposal());
            contextBanding = banding;
            // only count data for single proposals and for actual joint proposal
            if (!banding.isJointComponent()) {
                collectDataForProposal(banding.getProposal());
            } else if (banding.isJointComponent() && partnerFilter != null) { // ITAC-623 Completion of partial NTAC implementation, "show joints for *all* partners involved"
                collectDataForProposal(banding.getProposal());
            }
            // reset context information
            contextTotalObservationTime = null;
            contextBanding = null;
        }

        // in addition to banded proposals we have to take classical proposals into account
        for (Proposal proposal : queue.getClassicalProposals()) {
            // context information
            contextTotalObservationTime = calculateTotalObservationTime(proposal);
            // only count data for single proposals and for actual joint proposal
            if (!proposal.isJointComponent()) {
                collectDataForProposal(proposal);
            } else if (proposal.isJointComponent() && partnerFilter != null) { // ITAC-623 Completion of partial NTAC implementation, "show joints for *all* partners involved"
                collectDataForProposal(proposal);
            }
            // reset context information
            contextTotalObservationTime = null;
        }

        boilBeans();

        return getData();

    }

    private TimeAmount calculateTotalObservationTime(Proposal proposal) {
        TimeAmount totalObservationTime = new TimeAmount(0.0d, TimeUnit.HR);
        for (Observation obs : proposal.getPhaseIProposal().getObservations()) {
            totalObservationTime = totalObservationTime.sum(obs.getTime());
        }
        return totalObservationTime;
    }

    protected void collectDataForProposal(final Proposal proposal) {
        Validate.notNull(proposal);
        Validate.notNull(proposal.getPhaseIProposal());
        Validate.notNull(proposal.getPhaseIProposal().getObservations());

        // pre-condition -> we only collect data for proposals not for JointProposals
        Validate.isTrue(proposal instanceof Proposal);
        // In addition, all

        try {

            // apply proposal filters on proposal (site, partner and laserOpsOnly only;
            // instrument filter has to be applied on observations)
            if (matchesProposalFilter(proposal)) {
                Set<Observation> observations = proposal.getPhaseIProposal().getObservations();
                for (Observation observation : observations) {
                    if (matchesObservationFilter(observation)) {
                        final Instrument instrument = observation.getBlueprint().getInstrument();
                        if (matchesInstrumentFilter(instrument)) {
                            collectDataForObservation(proposal, observation);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // log error and add an error message that will be displayed in the gui
            LOGGER.log(Level.ERROR, "error in proposal " + proposal.getId() + " : " + e.getMessage());
            addError(proposal, e.getMessage());
        }
    }

    protected void collectDataForObservation(Proposal proposal, Observation observation) {
        // this method needs to be overridden or (in case of reports that are not based on observations) any
        // of the methods previous to this one in the call stack has to be overridden...
        throw new NotImplementedException();
    }

    protected void boilBeans() {
        // override this if any post processing of the collected data is needed
    }


    public boolean hasContextFinalizedQueue() {
        return contextQueue != null ? contextQueue.getFinalized() : false;
    }

    public boolean hasContextQueue() {
        return contextQueue != null ? true : false;
    }

    public boolean hasContextBanding() {
        return contextBanding != null ? true : false;
    }

    public Banding getContextBanding() {
        return contextBanding;
    }

    public TimeAmount getContextTotalObservationTime() {
        return contextTotalObservationTime;
    }

    public double getContextNormalizedExposureHrs(final Proposal proposal, final TimeAmount exposureTime) {
        if (hasContextQueue()) {
            TimeAmount totalAllocatedTime;
            if (hasContextBanding()) {
                // context of a banding -> get allocated time for accepted proposals of banding
                totalAllocatedTime = getContextBanding().getAwardedTime();
            } else {
                // no banding (classical) -> get allocated time for all proposals
                totalAllocatedTime = proposal.getTotalAwardedTime();
            }
            Validate.notNull(totalAllocatedTime);
            return calculateContextNormalizedExposureHrs(exposureTime, totalAllocatedTime);
        } else {
            return exposureTime.convertTo(TimeUnit.HR).getValue().doubleValue();
        }
    }

    private double calculateContextNormalizedExposureHrs(final TimeAmount exposureTime, final TimeAmount proposalAwardedTime) {
        Validate.notNull(getContextTotalObservationTime(), "Missing observation time");
        Validate.isTrue(getContextTotalObservationTime().getValue().compareTo(BigDecimal.ZERO) > 0, "Total observation time is 0");

        // UX-1507: manually added proposals can have zero hours awarded
        if (proposalAwardedTime.getValue().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        double exposureHrs = exposureTime.convertTo(TimeUnit.HR).getValue().doubleValue();
        double allocatedHrs = proposalAwardedTime.convertTo(TimeUnit.HR).getValue().doubleValue();
        double totalObservationHrs = getContextTotalObservationTime().convertTo(TimeUnit.HR).getValue().doubleValue();

        double hours = (exposureHrs / totalObservationHrs) * allocatedHrs;

        return hours;
    }


    /**
     * Sets the filter for a site (e.g. North or South)
     *
     * @param siteFilter
     */
    public void setSiteFilter(String siteFilter) {
        this.siteFilter = siteFilter;
    }

    protected boolean matchesSiteFilter(String site) {
        if (siteFilter != null && !siteFilter.equals(site)) {
            return false;
        }
        return true;
    }

    /**
     * Sets filtering for a specific partner using its abbreviation (e.g. US, CA, CL, AU etc).
     *
     * @param partnerFilter
     */
    public void setPartnerFilter(String partnerFilter) {
        this.partnerFilter = partnerFilter;
    }

    protected boolean matchesPartnerFilter(String partner) {
        if (partnerFilter != null && !partnerFilter.equals(partner)) {
            return false;
        }
        return true;
    }

    /**
     * Sets filtering for observations that include laser operations.
     *
     * @param laserOperationsFilter
     */
    public void setLaserOpsOnlyFilter(Boolean laserOperationsFilter) {
        this.laserOpsOnlyFilter = laserOperationsFilter;
    }

    protected boolean matchesLaserOpsOnlyFilter(boolean laser) {
        if (laserOpsOnlyFilter != null && (laserOpsOnlyFilter && !laser)) {
            return false;
        }
        return true;
    }


    /**
     * Sets filtering for a specific instrument name.
     *
     * @param instrumentFilter
     */
    public void setInstrumentFilter(String instrumentFilter) {
        this.instrumentFilter = instrumentFilter;
    }


    protected boolean matchesInstrumentFilter(final Instrument instrument) {
        Validate.notNull(instrument);
        if (instrumentFilter != null && !Instrument.valueOf(instrumentFilter).equals(instrument)) {
            return false;
        }

        return true;
    }

    /**
     * Applies all proposal specific filters to the proposal.
     *
     * @param proposal
     * @return <code>true</code> if the proposal has to be evaluated for the report
     */
    protected boolean matchesProposalFilter(final Proposal proposal) {
        // specific partner only?
        if (!matchesPartnerFilter(proposal.getPartner().getAbbreviation())) {
            return false;
        }

        return true;
    }

    /**
     * Applies all observation specific filters to the observation.
     *
     * @param observation
     * @return <code>true</code> if the observation has to be evaluated for the report
     */
    protected boolean matchesObservationFilter(final Observation observation) {
        // find the site string for this observation's blueprint
        // NOTE: exchange blueprints don't have a site (null), therefore we need to handle these cases separately
        BlueprintBase b = observation.getBlueprint();
        String site;
        if (b.isAtKeck()) {
            site = "Keck";
        } else if (b.isAtSubaru()) {
            site = "Subaru";
        }  else if (b.getSite() == null) {          // NOTE: exchanges don't have a site, this is a bit quirky, but that's how it is...
            site = "Exchange";
        } else {
            site = b.getSite().getDisplayName();
        }

        // specific site only?
        if (!matchesSiteFilter(site)) {
            return false;
        }

        // laser operations only?
        if (!matchesLaserOpsOnlyFilter(observation.getBlueprint().hasLgs())) {
            return false;
        }

        return true;
    }


    public String getSubtitleString() {
        // create a string with all selected filters
        StringBuilder builder = new StringBuilder();
        boolean insertSpace = false;
        if (siteFilter != null) {
            builder.append("Site=");
            builder.append(siteFilter);
            insertSpace = true;
        }
        if (partnerFilter != null) {
            if (insertSpace) {
                builder.append(", ");
            }
            builder.append("Partner=");
            builder.append(partnerFilter);
            insertSpace = true;
        }

        if (instrumentFilter != null) {
            if (insertSpace) {
                builder.append(", ");
            }
            builder.append("Instrument=");
            builder.append(instrumentFilter);
            insertSpace = true;
        }
        if (laserOpsOnlyFilter != null && laserOpsOnlyFilter == true) {
            if (insertSpace) {
                builder.append(", ");
            }
            builder.append("Laser operations only");
            insertSpace = true;
        }

        // concatenate target information with filter information
        if (builder.length() > 0) {
            return reportTarget + " [" + builder.toString() + "]";
        } else {
            return reportTarget;
        }
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the collected report data as a list of report data beans
     * each representing a row in the report.
     *
     * @return
     */
    public List<ReportBean> getData() {
        return data;
    }

    /**
     * Adds a data bean (i.e. a row) to the report.
     *
     * @param bean
     */
    protected void addResultBean(ReportBean bean) {
        data.add(bean);
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public String getUrl() {
        return this.url;
    }

    public void addError(Proposal proposal, String message) {
        errors.add(new Error(proposal, message));
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<Error> getErrors() {
        return errors;
    }

    public class Error {
        private Proposal proposal;
        private String message;

        public Error(Proposal proposal, String message) {
            this.proposal = proposal;
            this.message = message;
        }

        public Proposal getProposal() {
            return proposal;
        }

        public String getMessage() {
            return message;
        }
    }

    protected String getBandName(Proposal proposal) {
        String bandName = "-";
        if (proposal.isClassical()) {
            bandName = "Classical";
        } else if (hasContextBanding()) {
            bandName = getContextBanding().getBand().getDescription();
        }
        return bandName;
    }

    protected ScienceBand getBand(Proposal proposal) {
        if (hasContextBanding()) {
            return getContextBanding().getBand();
        } else {
            // classical proposals don't have a banding
            return ScienceBand.CLASSICAL;
        }
    }


    private List<Observation> preLoadObservations(Session session, Set<Long> p1pIds) {
        // bring in the observations together will all targets, blueprints and conditions
        @SuppressWarnings("unchecked")
        List<Observation> observations =
                session.createQuery(
                        "from Observation o " +
                                "join fetch o.target " +
                                "join fetch o.blueprint " +
                                "join fetch o.condition " +
                                "where o.proposal.id in (:p1pIds)").setParameterList("p1pIds", p1pIds).list();
        return observations;
    }
}
