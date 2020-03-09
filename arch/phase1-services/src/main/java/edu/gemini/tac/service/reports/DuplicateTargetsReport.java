package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.util.*;

/**
*/
public class DuplicateTargetsReport extends Report {

    public static final String NAME = "duplicateTargets";
    public static final double DUPLICATE_MAX_DISTANCE_ARCSECS = 30;

    private static final Logger LOGGER = Logger.getLogger(DuplicateTargetsReport.class.getName());

    /**
     * A set of sets of matching targets.
     * Targets that do not match any other targets will be stored in the set as a set with only one target.
     * In order to keep some additional data not only the target / observation is stored but a
     * highly sophisticated container object.
     */
    private Set<Set<TargetContainer>> matchList;

    /**
     * Constructs a new duplicate target reports object.
     */
    public DuplicateTargetsReport() {
        super(NAME);
        matchList = new HashSet<Set<TargetContainer>>();
    }

    @Override
    protected void collectDataForObservation(Proposal proposal, Observation observation) {
        Set<Observation> obsList = proposal.getPhaseIProposal().getObservations();
        Instrument instrument = observation.getBlueprint().getInstrument();

        TargetContainer newContainer = new TargetContainer();
        newContainer.proposal = proposal;
        newContainer.instrument = instrument;
        newContainer.observation = observation;

        // a set of all sets of targets the new target matches with (i.e. "is a duplicate of")
        Set<Set<TargetContainer>> matchingMatchSets = new HashSet<Set<TargetContainer>>();

        // check against all already existing match sets
        for (Set<TargetContainer> matchSet : matchList) {
            // check against all targets in this set
            for (TargetContainer targetContainer : matchSet) {
                // don't compare targets that belong to same proposal
                String key1 = targetContainer.proposal.getPhaseIProposal().getSubmissionsKey();
                String key2 = proposal.getPhaseIProposal().getSubmissionsKey();
                if (!key1.equals(key2)) {
                    // do comparison only if observations use the same instrument
                    if (targetContainer.instrument.equals(newContainer.instrument)) {
                        // finally we can compare the coordinates / names
                        if (compareTargets(targetContainer.observation.getTarget(), newContainer.observation.getTarget())) {
                            LOGGER.log(Level.DEBUG, "Found a MatchSet");
                            matchingMatchSets.add(matchSet);
                            break;
                        }else{
                            LOGGER.log(Level.DEBUG, "Not a Match");
                        }
                    }
                }
            }
        }

        // no matching match set, add a new one that contains the new target only
        if (matchingMatchSets.size() == 0) {
            Set<TargetContainer> newSet = new HashSet<TargetContainer>();
            newSet.add(newContainer);
            matchList.add(newSet);
        }
        // one matching match set found -> add new target to this match set
        else if (matchingMatchSets.size() == 1) {
            matchingMatchSets.iterator().next().add(newContainer);
        }
        // more than one matching match set found -> this basically means that this target "connects" existing
        // match sets (think {a,b} and {c,d} and e matches a and c i.e. there is a new match set {a,b,c,d,e})
        // -> concatenate all match sets and add the new target
        else {
            Set<TargetContainer> concat = new HashSet<TargetContainer>();
            for (Set<TargetContainer> m : matchingMatchSets) {
                matchList.remove(m);
                concat.addAll(m);
            }
            concat.add(newContainer);
            matchList.add(concat);
        }

    }

    private boolean compareTargets(Target a, Target b) {
        // ===
        // this is a hack to avoid running into problems when hibernate proxies are passed into this method
        // this should be handled somewhere else, but this will fix the immediate problem (casting does not
        // work with proxies and since the three different target types don't have a common way to access their
        // coordinates we unfortunately need to cast
        if (a instanceof HibernateProxy) {
            Hibernate.initialize(a);
            a = (Target) ((HibernateProxy) a).getHibernateLazyInitializer().getImplementation();
        }
        if (b instanceof HibernateProxy) {
            Hibernate.initialize(b);
            b = (Target) ((HibernateProxy) b).getHibernateLazyInitializer().getImplementation();
        }
        // ===

        LOGGER.log(Level.DEBUG, "comparing " + (a.getName()==null?"null":a.getName()) + " - " + (b.getName()==null?"null":b.getName()));
        
        // Ignore ToOs
        if (a.isToo() || b.isToo()) {
            return false;
        } 

        // One of the targets is expressed in non-sidereal coordinates
        // --> best thing we can do is compare the names (see notes below)
        if (a.isNonSidereal() || b.isNonSidereal()) {
            // NOTE: comments about duplicate checking for non-sidereal objects from Sandy Leggett / Chad Trujillo:
            //1) checking based on coordinates will never work, they just move too fast"
            //2) checking based on name is the only option, however for the small bodies each can have up to four different names
            //   you can find these by a search here http://ssd.jpl.nasa.gov/sbdb.cgi
            // so if one of the targets is described with non-sidereal coordinates we just compare the names
            // (currently without trying to resolve ambiguities using the database described above)

            LOGGER.log(Level.DEBUG, "comparing two non-sidereal coordinates by name");
            return haveSimilarNames(a, b);
        }

        // Both targets must be sidereal coordinates
        // --> they are considered potential duplicates if the differences between them are less than 15 arc-seconds
        Validate.isTrue(a.isSidereal());
        Validate.isTrue(b.isSidereal());

        // check epoch, must be equal to be comparable
        SiderealTarget nsA = (SiderealTarget) a;
        SiderealTarget nsB = (SiderealTarget) b;
        if (!nsA.getEpoch().equals(nsB.getEpoch())) {
            LOGGER.log(Level.WARN, "can not compare coordinates of different epochs");
            return false;
        }

        // check difference between the target coordinates
        LOGGER.log(Level.DEBUG, "comparing two sidereal coordinates with same epoch");
        double nsAra  = nsA.getCoordinates().getRa().toArcsecs().getMagnitude();
        double nsAdec = nsA.getCoordinates().getDec().toArcsecs().getMagnitude();

        double nsBra  = nsB.getCoordinates().getRa().toArcsecs().getMagnitude();
        double nsBdec = nsB.getCoordinates().getDec().toArcsecs().getMagnitude();

        // check for distances smaller than 30 arc-seconds or ~0.00834 degrees
        if (Math.abs(nsAra - nsBra) < DUPLICATE_MAX_DISTANCE_ARCSECS &&
            Math.abs(nsAdec - nsBdec) < DUPLICATE_MAX_DISTANCE_ARCSECS ) {
            return true;
        }

        return false;
    }

    private DuplicateTargetsReportBean createBean(Proposal p, Target a, Instrument instrument) {
        PrincipalInvestigator pi = p.getPhaseIProposal().getInvestigators().getPi();
        Submission submission = p.getSubmissionsPartnerEntry();

        DuplicateTargetsReportBean bean = new DuplicateTargetsReportBean();
        bean.setTargetInstrument(instrument.getDisplayName());
        bean.setTargetName(a.getName());
        bean.setPartner(p.getPartner().getAbbreviation());
        bean.setPartnerReference(submission.getReceipt().getReceiptId());
        bean.setPiName(pi.getFirstName() + " " + pi.getLastName());
        bean.setProposalTitle(p.getPhaseIProposal().getTitle());
        bean.setTargetCoordinates(a.getDisplayCoordinates());
        bean.setProposalUrl(getUrl() + "/tac/committees/" + p.getCommittee().getId() + "/proposals/"+ p.getId());

        return bean;
    }

    @Override
    protected void boilBeans() {
        int duplicateId = 0;
        for (Set<TargetContainer> m : matchList) {
            if (m.size() > 1) {
                for (TargetContainer t : m) {
                    DuplicateTargetsReportBean bean = createBean(t.proposal, t.observation.getTarget(), t.instrument);
                    bean.setDuplicateId(duplicateId);
                    addResultBean(bean);
                }
                duplicateId++;
            }
        }
    }

    private boolean haveSimilarNames(Target a, Target b) {
        if (a.getName() == null || b.getName() == null) {
            return false;
        }

        String nameA = a.getName().trim().replace(" ", "");
        String nameB = b.getName().trim().replace(" ", "");
        if (nameA.equalsIgnoreCase(nameB)) {
            return true;
        }
        LOGGER.log(Level.DEBUG, "'" + nameA + "' != '" + nameB + "'");
        return false;
    }

    private class TargetContainer {
        public Proposal proposal;
        public Instrument instrument;
        public Observation observation;

        @Override
        public int hashCode() {
            return new HashCodeBuilder(117236111, 2141713121).
                    append(proposal).
                    append(instrument).
                    toHashCode();
        }

        /**
         * Two target containers can be considered to be equal for this report if they refer to the same science target.
         * This is due to the fact that a science target can be referred to by more than one observation in a proposal,
         * but in the match set we want this target to be listed once only. Iterating over the targets instead of the
         * observations does not solve the problem because we need the information provided by the observation (instrument)
         * in addition to the information in the target (coordinates...). So using sets and defining hashCode and equals
         * as we need it will eliminate these duplicates while collecting the data. We do NOT compare the observation
         * for equality but the actual target that is used by the observation!
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TargetContainer))
                return false;

            final TargetContainer that = (TargetContainer) o;

            return new EqualsBuilder().
                    append(this.proposal, that.proposal).
                    append(this.instrument, that.instrument).
                    append(this.observation.getTarget(), that.observation.getTarget()).
                    isEquals();
        }

    }

    public class DuplicateTargetsReportBean extends ReportBean {
        private int duplicateId;
        private String proposalTitle;
        private String piName;
        private String partner;
        private String partnerReference;
        private String targetName;
        private String targetCoordinates;
        private String targetInstrument;
        private String proposalUrl;

        public int getDuplicateId() {
            return this.duplicateId;
        }

        public void setDuplicateId(int duplicateId) {
            this.duplicateId = duplicateId;
        }

        public String getTargetName() {
            return targetName;
        }

        public void setTargetName(String targetName) {
            this.targetName = targetName;
        }

        public String getTargetCoordinates() {
            return targetCoordinates;
        }

        public void setTargetCoordinates(String targetCoordinates) {
            this.targetCoordinates = targetCoordinates;
        }

        public String getTargetInstrument() {
            return targetInstrument;
        }

        public void setTargetInstrument(String targetInstrument) {
            this.targetInstrument = targetInstrument;
        }

        public void setProposalTitle(String proposalTitle) {
            this.proposalTitle = proposalTitle;
        }

        public void setPiName(String piName) {
            this.piName = piName;
        }

        public void setPartner(String partner) {
            this.partner = partner;
        }

        public void setPartnerReference(String partnerReference) {
            this.partnerReference = partnerReference;
        }

        public String getProposalTitle() {
             return proposalTitle;
        }

        public String getPiName() {
            return piName;
        }

        public String getPartner() {
            return partner;
        }

        public String getPartnerReference() {
            return partnerReference;
        }

        public String getProposalUrl() {
            return proposalUrl;
        }

        public void setProposalUrl(String proposalUrl) {
            this.proposalUrl = proposalUrl;
        }

    }

}
