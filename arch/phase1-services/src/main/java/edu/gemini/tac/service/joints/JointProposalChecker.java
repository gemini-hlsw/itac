package edu.gemini.tac.service.joints;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssueCategory;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.service.check.ProposalCheck;
import edu.gemini.tac.service.check.ProposalChecker;
import edu.gemini.tac.service.check.ProposalIssue;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
* Proposal checker that validates a series of conditions that should hold true for joint proposals and warns
* if they're violated. This could/should be part of the ProposalChecker that is implemented in Scala but due to
* my lack of knowledge of Scala I provide here my own pathetic implementation using old-school Java.
*/
public class JointProposalChecker implements ProposalChecker {

    public Set<ProposalCheck> checks() {
        // currently we don't use this; checks are "hard-coded" and not dynamically configured
        throw new NotImplementedException();
    }

    public Set<ProposalIssue> exec(Collection<Proposal> proposals) {
        Set<ProposalIssue> issues = new HashSet<ProposalIssue>();

        for (Proposal proposal : proposals) {
            if (proposal.isJoint()) {
                checkJointProposal((JointProposal) proposal, issues);
            }
        }

        return issues;
    }

    private void checkJointProposal(JointProposal jointProposal, Set<ProposalIssue> issues) {
        checkObservingMode(jointProposal, issues);
        checkSite(jointProposal, issues);
        checkObservations(jointProposal, issues);
    }

    // ----------------
    // THE RULEZ.......
    // ----------------

    /**
     * Checks that all component proposals have the same observing mode.
     * @param jointProposal
     * @param issues
     */
    private void checkObservingMode(JointProposal jointProposal, Set<ProposalIssue> issues) {
        Proposal primaryProposal = jointProposal.getPrimaryProposal();
        final String primaryObservingMode = primaryProposal.getPhaseIProposal().getObservingMode();

        for (Proposal proposal : jointProposal.getProposals()) {
            final String observingMode = proposal.getPhaseIProposal().getObservingMode();

            if (!primaryObservingMode.equals(observingMode)) {
                ProposalIssue newIssue =
                        new JointProposalIssue(
                                ProposalIssue.Severity.error,
                                jointProposal,
                                "Proposal " + proposal.getPartnerReferenceNumber() + " has observing mode " + observingMode +
                                " while primary proposal has observing mode " + primaryObservingMode,
                                ProposalIssueCategory.Structural);
                issues.add(newIssue);
            }
        }
    }

    /**
     * Checks that all component proposals belong to the same site.
     * @param jointProposal
     * @param issues
     */
    private void checkSite(JointProposal jointProposal, Set<ProposalIssue> issues) {
        Proposal primaryProposal = jointProposal.getPrimaryProposal();
        final Site primaryProposalSite = primaryProposal.getSite();

        for (Proposal proposal : jointProposal.getProposals()) {
            final Site proposalSite = proposal.getSite();

            // Proposals for exchange instruments may have null sites.
            if (primaryProposalSite != null && proposalSite != null) {
                if (!proposalSite.equals(primaryProposalSite)) {
                    ProposalIssue newIssue =
                            new JointProposalIssue(
                                    ProposalIssue.Severity.error,
                                    jointProposal,
                                    "Proposal " + proposal.getPartnerReferenceNumber() + " belongs to site " + proposalSite.getDisplayName() +
                                    " while primary proposal belongs to " + primaryProposalSite.getDisplayName(),
                                    ProposalIssueCategory.Structural);
                    issues.add(newIssue);
                }
            }
        }
    }


    /**
     * Checks that all component proposals have exactly the same observations in their obeservation lists.
     * Same observation means: same targets (matching coordiantes and type), same instrument, same constraints
     * and same exposure times.
     * @param jointProposal
     * @param issues
     */
    private void checkObservations(JointProposal jointProposal, Set<ProposalIssue> issues) {
        Proposal primaryProposal = jointProposal.getPrimaryProposal();
        Map<ObservationKey, ObservationPlaceholder> primaryObservations = createObservationSet(jointProposal, primaryProposal);

        for (Proposal proposal : jointProposal.getProposals()) {
            if (proposal == primaryProposal) {
                // don't have to compare primary with itself..
                continue;
            }

            // create special set of observations for secondary
            Map<ObservationKey, ObservationPlaceholder> secondaryObservations = createObservationSet(jointProposal, proposal);

            // create issues for observations that are in secondary but not in primary
            final boolean hasTooMany = createIssuesForTooManyInSecondary(primaryObservations, secondaryObservations, issues);
            // if all observations in secondary are in primary, also check if there are observations missing in secondary
            // NOTE: this is only done if so far no differences between secondary and primary could be found because
            // otherwise many differences produce two issues at the same time, which can be confusing when trying to
            // track down problems (for example observations in primary and secondary where only the band is different
            // will otherwise show up once as "too many in secondary" and once again as "missing from primary")
            if (!hasTooMany) {
                createIssuesForMissingInSecondary(primaryObservations, secondaryObservations, proposal, issues);
            }

            // check for equal observations if they are identical (same instrument, constraints and exposure)
            createIssuesForEqualObservations(primaryObservations, secondaryObservations, issues);
        }
    }


    // --- helpers for implementation of checkObservations

    /**
     * Creates a set with specialised observation objects that allow to find equal observations (same type
     * and coordinates).
     * @param jointProposal
     * @param proposal
     * @return
     */
    private Map<ObservationKey, ObservationPlaceholder> createObservationSet(JointProposal jointProposal, Proposal proposal) {
        final Set<Observation> obsList = proposal.getPhaseIProposal().getObservations();
        Map<ObservationKey, ObservationPlaceholder> observations = new HashMap<ObservationKey, ObservationPlaceholder>();
        for (Observation observation : obsList) {
            ObservationKey key = new ObservationKey(observation);
            ObservationPlaceholder obs = new ObservationPlaceholder(jointProposal, proposal, observation);
            observations.put(key,  obs);
        }
        return observations;
    }

    /**
     * Creates issues for observations that are in secondary but not in primary proposal.
     * @param primaryObservations
     * @param secondaryObservations
     * @param issues
     */
    private boolean createIssuesForTooManyInSecondary(
            final Map<ObservationKey, ObservationPlaceholder> primaryObservations,
            final Map<ObservationKey, ObservationPlaceholder> secondaryObservations,
            final Set<ProposalIssue> issues) {
        final Set<ObservationKey> secondaryObservationKeys = secondaryObservations.keySet();
        final Set<ObservationKey> primaryObservationKeys = primaryObservations.keySet();
        final Set<ObservationKey> secondaryOnly = new HashSet<ObservationKey>(secondaryObservationKeys);
        secondaryOnly.removeAll(primaryObservationKeys);

        boolean hasTooMany = false;

        for (ObservationKey key : secondaryOnly) {
            ObservationPlaceholder primaryObservation = primaryObservations.get(key);
            ObservationPlaceholder secondaryObservation = secondaryObservations.get(key);
            Validate.isTrue(primaryObservation == null);
            Validate.notNull(secondaryObservation);
            ProposalIssue newIssue =
                    new JointProposalIssue(
                            ProposalIssue.Severity.error,
                            secondaryObservation.getJointProposal(),
                            "Proposal " + secondaryObservation.getProposal().getPartnerReferenceNumber() +
                                " has a " + secondaryObservation.getBand() + " observation for " +
                                secondaryObservation.getBlueprintName() + " with science target " +
                                secondaryObservation.getName() + " that is not contained in the primary proposal.",
                            ProposalIssueCategory.Structural);
            issues.add(newIssue);
            hasTooMany = true;
        }

        return hasTooMany;
    }

    /**
     * Creates issues for observations that are in primary but not in secondary proposal.
     * @param primaryObservations
     * @param secondaryObservations
     * @param issues
     */
    private void createIssuesForMissingInSecondary(Map<ObservationKey, ObservationPlaceholder> primaryObservations, Map<ObservationKey, ObservationPlaceholder> secondaryObservations, Proposal secondaryProposal, Set<ProposalIssue> issues) {
        Set<ObservationKey> primaryOnly = new HashSet<ObservationKey>(primaryObservations.keySet());
        primaryOnly.removeAll(secondaryObservations.keySet());

        for (ObservationKey key : primaryOnly) {
            ObservationPlaceholder primaryObservation = primaryObservations.get(key);
            ObservationPlaceholder secondaryObservation = secondaryObservations.get(key);
            Validate.notNull(primaryObservation);
            Validate.notNull(secondaryProposal);
            Validate.isTrue(secondaryObservation == null);
            ProposalIssue newIssue =
                    new JointProposalIssue(
                            ProposalIssue.Severity.error,
                            primaryObservation.getJointProposal(),
                            "Proposal " + secondaryProposal.getPartnerReferenceNumber() +
                                " is missing a " + primaryObservation.getBand() + " observation for " +
                                primaryObservation.getBlueprintName() + " with science target " +
                                primaryObservation.getName(),
                            ProposalIssueCategory.Structural);
            issues.add(newIssue);
        }
    }

    /**
     * Checks if equal observations are also identical.
     * Equal in the context of the checker means same targets (type and coordinates), identical means in addition
     * to being equal having the same instrument, constraints and exposure time.
     * @param primaryObservations
     * @param secondaryObservations
     * @param issues
     */
    private void createIssuesForEqualObservations(Map<ObservationKey, ObservationPlaceholder> primaryObservations, Map<ObservationKey, ObservationPlaceholder> secondaryObservations, Set<ProposalIssue> issues) {
        final Set<ObservationKey> primaryObservationKeys = primaryObservations.keySet();
        final Set<ObservationKey> secondaryObservationKeys = secondaryObservations.keySet();
        Set<ObservationKey> both = new HashSet<ObservationKey>(primaryObservationKeys);
        both.retainAll(secondaryObservationKeys);

        for (ObservationKey key : both) {
            ObservationPlaceholder primaryObservation = primaryObservations.get(key);
            ObservationPlaceholder secondaryObservation = secondaryObservations.get(key);
            Validate.notNull(primaryObservation);
            Validate.notNull(secondaryObservation);
            primaryObservation.createIssuesWithPrimary(secondaryObservation, issues);
        }
    }

    /**
     * Special class that implements equality between targets in the context of the joint proposal checker.
     * Two targets are considered to be equal when they have the same type and the same coordinates.
     */
    private class TargetKey {
        private final boolean isNonSidereal;
        private final boolean isSidereal;
        private final boolean isToo;
        private final String name;
        private final String siderealTargetCoordinates;

        public TargetKey(Target targetKey) {
            isNonSidereal = targetKey.isNonSidereal();
            isSidereal = targetKey.isSidereal();
            isToo = targetKey.isToo();
            name = targetKey.getName();

            if (isSidereal) {
                final SiderealTarget siderealTarget = (SiderealTarget) targetKey;
                siderealTargetCoordinates = siderealTarget.getCoordinates().toDisplayString();
            } else {
                siderealTargetCoordinates = null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TargetKey targetKey = (TargetKey) o;

            if (isNonSidereal != targetKey.isNonSidereal) return false;
            if (isSidereal != targetKey.isSidereal) return false;
            if (isToo != targetKey.isToo) return false;
            if (name != null ? !name.equals(targetKey.name) : targetKey.name != null) return false;
            if (siderealTargetCoordinates != null ? !siderealTargetCoordinates.equals(targetKey.siderealTargetCoordinates) : targetKey.siderealTargetCoordinates != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (isNonSidereal ? 1 : 0);
            result = 31 * result + (isSidereal ? 1 : 0);
            result = 31 * result + (isToo ? 1 : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (siderealTargetCoordinates != null ? siderealTargetCoordinates.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "TargetKey{" +
                    "isNonSidereal=" + isNonSidereal +
                    ", isSidereal=" + isSidereal +
                    ", isToo=" + isToo +
                    ", name='" + name + '\'' +
                    ", siderealTargetCoordinates=" + siderealTargetCoordinates +
                    '}';
        }
    }

    /**
     * Special class that implements equality between observations in the context of the joint proposal checker.
     * Two observations are considered to be "equal" when their targets are equal, the same instrument (blueprint)
     * is used and the band is equal. All observations that are considered to be equal should be "identical" which
     * is in the context of the checker that they have the same constraints and exposure times.
     */
    private class ObservationKey {
        private final TargetKey targetKey;
        private final String blueprintDisplay;
        private final Band band;
        private final BigDecimal exposureTime;

        public ObservationKey(final Observation observation) {
            this.targetKey = new TargetKey(observation.getTarget());
            this.blueprintDisplay = observation.getBlueprint().getDisplay();
            this.band = observation.getBand();
            this.exposureTime = observation.getTime().getValueInHours();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ObservationKey that = (ObservationKey) o;

            if (blueprintDisplay != null ? !blueprintDisplay.equals(that.blueprintDisplay) : that.blueprintDisplay != null)
                return false;
            if (targetKey != null ? !targetKey.equals(that.targetKey) : that.targetKey != null)
                return false;
            if (band != null ? !band.equals(that.band) : that.band != null)
                return false;
            if (exposureTime != null ? ! exposureTime.equals(that.exposureTime) : that.exposureTime != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = targetKey != null ? targetKey.hashCode() : 0;
            result = 31 * result + (blueprintDisplay != null ? blueprintDisplay.hashCode() : 0);
            result = 13 * result + (band != null ? band.hashCode() : 0);
            result = 31 * result + (exposureTime != null ? exposureTime.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ObservationKey{" +
                    "targetKey=" + targetKey +
                    '}';
        }
    }

    /**
     * A placeholder class for observations that carries a lot of additional information.
     */
    private class ObservationPlaceholder {
        private JointProposal jointProposal;
        private Proposal proposal;
        private final String name;
        private final String blueprintName;
        private final Condition condition;
        private TimeAmount exposureTime;
        private final Band band;

        public ObservationPlaceholder(JointProposal jointProposal, Proposal proposal, Observation observation) {
            this.blueprintName = observation.getBlueprint().getDisplay();
            this.condition = observation.getCondition();
            this.jointProposal = jointProposal;
            this.proposal = proposal;
            this.name = observation.getTarget().getName();
            this.exposureTime = observation.getTime();
            this.band = observation.getBand();
        }

        public JointProposal getJointProposal() {
            return jointProposal;
        }

        public Proposal getProposal() {
            return proposal;
        }

        public String getName() {
            return name;
        }

        public String getBand() {
            return band.value();
        }

        public String getBlueprintName() {
            return blueprintName;
        }

        public void createIssuesWithPrimary(ObservationPlaceholder secondary, Set<ProposalIssue> issues) {
            if(this.name == null){
                issues.add(createIssue(secondary, " -- observation does not have a science target"));
            }

            // NOTE: equals() on SiteQuality and Time is defined more strictly than we want it to be for the
            // checker. I don't won't to change that code in order not to break any functionality and implement
            // therefore the equality of the constraints and time as we need it for the checker.

            // for the checker constraints are equal when they define same values for IQ, CC, SB and WV
            if (!condition.getImageQuality().equals(secondary.condition.getImageQuality())) {
                issues.add(createIssue(secondary, " IQ constraint is different."));
            }
            if (!condition.getCloudCover().equals(secondary.condition.getCloudCover())) {
                issues.add(createIssue(secondary, " CC constraint is different."));
            }
            if (!condition.getSkyBackground().equals(secondary.condition.getSkyBackground())) {
                issues.add(createIssue(secondary, " SB constraint is different."));
            }
            if (!condition.getWaterVapor().equals(secondary.condition.getWaterVapor())) {
                issues.add(createIssue(secondary, " WV constraint is different."));
            }

            // for the checker times are equal if they express the same amount of time regardless of the actual units
            // -> get time as hours and compare that value (equals in Time compares units and amount)
            if (exposureTime.getDoubleValueInHours() != secondary.exposureTime.getDoubleValueInHours()) {
                issues.add(createIssue(secondary, " exposure time is different."));
            }
        }

        private ProposalIssue createIssue(ObservationPlaceholder secondary, String message) {
            ProposalIssue newIssue =
                    new JointProposalIssue(
                            ProposalIssue.Severity.error,
                            jointProposal,
                            secondary.getBand() + " observation " + secondary.getName() + " in " + secondary.getProposal().getPartnerReferenceNumber() +
                                " matches observation " + this.getName() + " in primary proposal but " + message,
                            ProposalIssueCategory.Structural);
            return newIssue;
        }
    }

    /**
     * Implementation of issues that are found during checking.
     * The interface originates from the Scala implementation and does unfortunately not stick to JavaBean conventions.
     * But re-using it here has the advantage of being able to re-use the proposal display page including it's
     * filter and sort capabilities.
     */
    public class JointProposalIssue implements ProposalIssue {
        private Severity severity;
        private JointProposal jointProposal;
        private String message;
        private ProposalIssueCategory category;

        public JointProposalIssue(Severity severity, JointProposal jointProposal, String message, ProposalIssueCategory category) {
            this.severity = severity;
            this.jointProposal = jointProposal;
            this.message = message;
            this.category = category;
        }

        public Severity severity() {
            return severity;
        }

        public ProposalCheck check() {
            // not in use
            throw new NotImplementedException();
        }

        public Proposal proposal() {
            return jointProposal;
        }

        public String message() {
            return message;
        }

        public ProposalIssueCategory category() {
            return category;
        }

        public void category(ProposalIssueCategory category) {
            this.category = category;
        }
    }
}
