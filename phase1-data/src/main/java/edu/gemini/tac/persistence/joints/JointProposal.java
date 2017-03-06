package edu.gemini.tac.persistence.joints;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.model.p1.mutable.InvestigatorStatus;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.ProposalIssue;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

/**
 * A Joint Proposal is a proposal that replaces (?) two or more Proposals, by merging their data according to
 * element-specific logic. The reason for a class is primarily to facilitate the "Undo" process -- rather
 * than trying to undo the merge, just return pointers to the original Proposals.  Joint proposals originally
 * had a design where the merge happened in live instance's in the database.  With the great schema replacement of 2011, most
 * of the changes are pass-throughs to the primary proposal with several notable differences.  The JointProposal
 * has it's own ITAC element as well as differing aggregations of investigators and NGO submissions.
 * <p/>
 *
 * @author lobrien
 * @author ddawson
 * @since Dec 7, 2010
 */

@NamedQueries({
        @NamedQuery( name = "jointproposal.findForEditingById",
                query = "from Proposal jp " +
                        "join fetch jp.itac " +
                        "join fetch jp.primaryProposal p " +
                        "join fetch jp.committee comm " +
                        "left outer join fetch jp.partner part " +
                        "join fetch p.phaseIProposal p1p " +
                        "join fetch p1p.itac " +
                        "join fetch p1p.investigators i " +
                        "left outer join fetch p.issues " +
                        "where jp.committee = :committeeId and jp.id = :proposalId"),
        @NamedQuery( name = "jointproposal.findByComponentAbbreviation",
                query = "from JointProposal jp " +
                        "join fetch jp.proposals p " +
                        "left join fetch jp.proposals p2 " +
                        "join fetch p2.partner part " +
                        "where p.committee = :committeeId and " +
                        "part.abbreviation = :partnerAbbreviation")
})
@Entity
@DiscriminatorValue("class edu.gemini.tac.persistence.joints.JointProposal")
public class JointProposal extends Proposal {
    private static final Logger LOGGER = Logger.getLogger(JointProposal.class.getName());

    @OneToMany(targetEntity = Proposal.class, fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "joint_proposal_id")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    private Set<Proposal> proposals = new HashSet<Proposal>(2);

    @OneToOne(targetEntity = Proposal.class, fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_proposal_id")
    private Proposal primaryProposal;

    @Transient
    private PhaseIProposal derivedPhaseIProposal = null;

    /**
     * ITACs are the one permanent (and independent) property of a joint proposal.  Investigators are
     * not identical to the primary proposal, but they are re-derived each time.
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "joint_proposal_itac_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Itac itac = new Itac();

    public JointProposal(JointProposal that){
        memberClone(that);
    }

    /**
     * Performs a deep clone of _this_, cloning member proposals.
     * ITAC will be different, as always.
     * derivedPhaseIProposal, as always, is lazy and null at this point
     */
    protected void memberClone(JointProposal that){
        // do not call: super.memberClone(that) here, this will make yet another copy of the primary proposal!
        this.setCommittee(that.getCommittee());
        this.setPartner(that.getPartner());

        //Do a deep clone /this includes the primary proposal, see comment above)
        Proposal thatPrimary = that.getPrimaryProposal();
        for(Proposal thatP : that.getProposals()){
            final Proposal cloneP = new Proposal(thatP);
            this.proposals.add(cloneP);
            //One of them is the primary. When found, set this primary to the clone
            if(thatP.equals(thatPrimary)){
                this.setPrimaryProposal(cloneP);
            }
        }
    }

     public Proposal duplicate() {
        LOGGER.log(Level.DEBUG, "JointProposal.duplicate()");
        final Proposal dup = new JointProposal(this);
        return dup;
    }


    public void setPhaseIProposal(final PhaseIProposal phaseIProposal) {
        derivedPhaseIProposal = null;
        if (primaryProposal != null)
            phaseIProposal.setParent(getPrimaryProposal());
        else
            phaseIProposal.setParent(this);
    }


    /**
     * Let's try not to walk the path of merging madness again.  The instance here is not completely backed by
     * the database.  Most of it directly passes through to the primary proposal, with some exceptions that aggregate
     * information from the component proposals
     *
     * @return
     */
    @Override
    public PhaseIProposal getPhaseIProposal() { return getPhaseIProposal(false); }
    public PhaseIProposal getPhaseIProposal(final boolean forceRegeneration) {
        if (derivedPhaseIProposal != null && !forceRegeneration)
            return derivedPhaseIProposal;

        final PhaseIProposal primaryPIProposal = getPrimaryProposal().getPhaseIProposal();

        derivedPhaseIProposal = primaryPIProposal.memberCloneLoaded();

        // Now for the elements that don't have such simple logic.
        // First, investigators.
        final Investigators mergedInvestigators = new Investigators();
        derivedPhaseIProposal.setInvestigators(mergedInvestigators);
        final HashSet<PrincipalInvestigator> pis = new HashSet<PrincipalInvestigator>();
        final Set<CoInvestigator> cois = new HashSet<CoInvestigator>();
        for (Proposal p : proposals) {
            final Investigators pInvestigators = p.getPhaseIProposal().getInvestigators();
            pis.add(pInvestigators.getPi());
            cois.addAll(pInvestigators.getCoi());
        }
        final List<String> piFirstNames = new ArrayList<String>();
        final List<String> piLastNames = new ArrayList<String>();
        final List<String> piEmails = new ArrayList<String>();
        final List<String> piStatuses = new ArrayList<String>();
        final List<String> piInstitutions = new ArrayList<String>();
        final List<String> piAddresses = new ArrayList<String>();
        final List<String> piCountries = new ArrayList<String>();
        for (PrincipalInvestigator pi : pis) {
            piFirstNames.add(pi.getFirstName());
            piLastNames.add(pi.getLastName());
            piEmails.add(pi.getEmail());
            piStatuses.add(pi.getStatus().value());
            piInstitutions.add(pi.getInstitutionAddress().getInstitution());
            piAddresses.add(pi.getInstitutionAddress().getAddress());
            piCountries.add(pi.getInstitutionAddress().getCountry());
        }

        mergedInvestigators.getPi().setFirstName(StringUtils.join(piFirstNames, "/"));
        mergedInvestigators.getPi().setLastName(StringUtils.join(piLastNames, "/"));
        mergedInvestigators.getPi().setEmail(StringUtils.join(piEmails, ";"));
        if (piStatuses.size() > 1)
            mergedInvestigators.getPi().setStatus(InvestigatorStatus.OTHER);
        else
            mergedInvestigators.getPi().setStatus(getPrimaryProposal().getPhaseIProposal().getInvestigators().getPi().getStatus());
        mergedInvestigators.getPi().setStatusDisplayString(StringUtils.join(piStatuses, ";"));
        mergedInvestigators.getPi().setInstitutionAddress(new InstitutionAddress());
        mergedInvestigators.getPi().getInstitutionAddress().setInstitution(StringUtils.join(piInstitutions, "/"));
        mergedInvestigators.getPi().getInstitutionAddress().setAddress(StringUtils.join(piAddresses, "/"));
        mergedInvestigators.getPi().getInstitutionAddress().setCountry(StringUtils.join(piCountries, "/"));
        mergedInvestigators.getCoi().addAll(cois);

        // The joint proposal has it's very own ITAC.
        derivedPhaseIProposal.setItac(itac);

        // Submissions consists of the primary submission for each component proposal.
        derivedPhaseIProposal.clearSubmissions();
        derivedPhaseIProposal.addSubmission(getPrimaryProposal().getPhaseIProposal().getPrimary());
        for (Proposal p : getSecondaryProposals()) {
            final Submission primarySubmission = p.getPhaseIProposal().getPrimary();
            derivedPhaseIProposal.addSubmission(primarySubmission);
        }

        derivedPhaseIProposal.getAllObservations().clear();
        derivedPhaseIProposal.getAllObservations().addAll(getPrimaryProposal().getPhaseIProposal().getAllObservations());
        derivedPhaseIProposal.getBlueprints().clear();
        derivedPhaseIProposal.getBlueprints().addAll(getPrimaryProposal().getPhaseIProposal().getBlueprints());
        derivedPhaseIProposal.getConditions().clear();
        derivedPhaseIProposal.getConditions().addAll(getPrimaryProposal().getPhaseIProposal().getConditions());
        derivedPhaseIProposal.getTargets().clear();
        derivedPhaseIProposal.getTargets().addAll(getPrimaryProposal().getPhaseIProposal().getTargets());

        derivedPhaseIProposal.setPrimary(getPrimaryProposal().getPhaseIProposal().getPrimary());
        derivedPhaseIProposal.setParent(this);

        return derivedPhaseIProposal;
    }

    public boolean hasPrimaryProposal() {
        return !(primaryProposal == null);
    }

    public Proposal getPrimaryProposal() {
        if (primaryProposal == null) {
            if (proposals == null || proposals.size() == 0) {
                throw new IllegalStateException("No proposals in joint proposal");
            }
            throw new IllegalStateException("Primary proposal not chosen");
        }
        return primaryProposal;
    }

    /**
     * Set one of the member proposals as the primary proposal
     *
     * @param primary Must be member of proposals or will throw IllegalArgumentException
     * @return
     */
    public void setPrimaryProposal(final Proposal primary) {
        /*
        TODO: Reinstate this block *after* the issue with Hibernate PersistentMaps and Proposal equals()/hashCode()
        is resolved. Today, this function improperly returns *false* on proposals.contains(primary) due to this issue.
        We can't replace it with a check on ID because we call this function prior to saving entities. So we *could*
        write different codepaths depending on whether its been saved or not. But since this is essentially just
        a validation block (there is no use-case that is expected to try to promote a non-member proposal) and risk
        for new codepaths is high, just let it ride...
         */
//        if (!proposals.contains(primary)) {
//            throw new IllegalArgumentException("Joint proposal does not contain proposal being marked as primary");
//        }
        primaryProposal = primary;
        //Copy ITAC data (but not Phase I Proposal data
        this.setCommittee(primaryProposal.getCommittee());
        this.setPartner(primaryProposal.getPartner());
        if (primary.getIssues() != null) {
            // copy the elements into a new collection, don't reuse the collection; hibernate does not like that
            this.setIssues(new HashSet<ProposalIssue>(primaryProposal.getIssues()));
        } else {
            this.setIssues(null);
        }
    }

    @Override
    public Set<Proposal> getProposals() {
        return proposals;
    }

    public void setProposals(Set<Proposal> proposals) {
        invalidateDerivedProposal();
        this.proposals = proposals;
    }

    //Override property getters
    @Override
    public boolean isJoint() {
        return true;
    }

    public JointProposal() {
        super();
    }

    public JointProposal(Proposal p, Session s) {
        super(p, s);
        primaryProposal = p;
    }

    /**
     * Adds the proposal to this JointProposal, makes it the primary proposal if no primary exists yet.
     * Also takes care of saving or updating this joint proposal.
     */
    public void add(Proposal p, Session session) {
        validateNoDuplicatePartners(p);
        invalidateDerivedProposal();

        proposals.add(p);
        if (primaryProposal == null) {        // if this is the first proposal added to this joint make it the primary
            setPrimaryProposal(p);
        }

        session.saveOrUpdate(this);
    }

    /**
     * Adds the proposal to this JointProposal, makes it the primary proposal if no primary exists yet.
     */
    public void add(Proposal p) {
        validateNoDuplicatePartners(p);
        invalidateDerivedProposal();
        // if this is the first proposal added to this joint make it the primary
        //Actually do the add
        proposals.add(p);
        if (primaryProposal == null) {
            setPrimaryProposal(p);
        }
    }

    public void addAll(Collection<Proposal> proposals, Session s) {
        invalidateDerivedProposal();
        for (Proposal p : proposals) {
            this.add(p, s);
        }
    }

    @Override
    public String toDot() {
        String dot = "digraph Proposal {\n";
        String myDot = "JointProposal_" + (this.getId() == null ? "NULL" : getId().toString());
        dot += myDot + ";\n";
        dot += myDot + "->" + this.getPhaseIProposal().toDot() + ";\n";
        //Show links to original
        dot += "Primary;\n";
        dot += myDot + "->Primary;Primary->" + getPrimaryProposal().toDot() + "\n";
        for (Proposal p : proposals) {
            //Just show their top-level existence, not their whole graphs (toString() rather than toDot() )
            dot += this.toString() + "->" + p.toDot() + ";\n";
        }
        dot += "}\n";
        return dot;
    }

    public boolean hasComponent(final Proposal proposal) {
        for (Proposal p : proposals) {
            if (p.getId().longValue() == proposal.getId().longValue()) {
                return true;
            }
        }
        return false;
    }

    public Set<Proposal> getSecondaryProposals() {
        final Set<Proposal> componentsDuplicate = new HashSet<Proposal>(getProposals());
        componentsDuplicate.remove(getPrimaryProposal());
        return componentsDuplicate;
    }

    @Override
    public String getPartnerAbbreviation() {
        return getPartnerAbbreviation(proposals, true);
    }

    public String getPartnerAbbreviation(final Set<Proposal> proposals) {
        return getPartnerAbbreviation(proposals, true);
    }

    public String getRejectedPartnerAbbreviation(final Set<Proposal> proposals) {
        return getPartnerAbbreviation(proposals, false);
    }

    private String getPartnerAbbreviation(final Set<Proposal> proposals, final boolean accepted) {
        StringBuffer sb = new StringBuffer();
        if (accepted) {
            sb.append("J:");
        } else if (proposals.size() > 0) {
            sb.append("Rejected: ");
        }
        int i = 0;
        for (Proposal proposal : proposals) {
            // check that this proposal is really a part of this joint proposal
            Validate.isTrue(proposals.contains(proposal));

            if (i > 0) {
                sb.append("/");
            }
            sb.append(proposal.getPartner().getAbbreviation());
            i++;
        }
        return sb.toString();
    }

    @Override
    public String getPartnerReferenceNumber() {
        return getPartnerReferenceNumber(proposals);
    }

    public String getPartnerReferenceNumber(final Set<Proposal> proposals) {
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Proposal proposal : proposals) {

            // check that this proposal is really a part of this joint proposal
// UX-1502: The validation below does not work, it seems there is a problem with either hashCode() or equals();
// at this late stage and a few hours before using the system in production I dont't dare to touch any
// of these methods.
//            Validate.isTrue(this.proposals.contains(proposal));
// UX-1502: Instead we use the check below (using the ids), this serves the same purpose and works as expected.
            boolean contained = false;
            for (Proposal jointMember : this.proposals) {
                if (jointMember.getId().equals(proposal.getId())) {
                    contained = true;
                }
            }
            Validate.isTrue(contained);  // check if the proposal is indeed part of the joint
// END UX-1502.

            if (i > 0) {
                sb.append(" ");
            }
            sb.append(proposal.getPartnerAbbreviation());
            sb.append("[");
            sb.append(proposal.getPartnerReferenceNumber());
            sb.append("]");
            i++;
        }
        return sb.toString();
    }

    @Override
    public String getPartnerRanking() {
        return getPartnerRanking(proposals);
    }

    public String getPartnerRanking(Set<Proposal> proposals) {
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Proposal proposal : proposals) {
            // check that this proposal is really a part of this joint proposal
            Validate.isTrue(proposals.contains(proposal));

            if (i > 0) {
                sb.append(" ");
            }
            sb.append(proposal.getPartnerAbbreviation());
            sb.append("[");
            sb.append(proposal.getPartnerRanking());
            sb.append("]");
            i++;
        }
        return sb.toString();
    }

    @Override
    public String getPartnerAwardedTime() {
        return getPartnerAwardedTime(proposals);
    }

    public String getPartnerAwardedTime(Set<Proposal> proposals) {
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Proposal proposal : proposals) {
            // check that this proposal is really a part of this joint proposal
            Validate.isTrue(proposals.contains(proposal));

            if (i > 0) {
                sb.append(" ");
            }
            sb.append(proposal.getPartnerAbbreviation());
            sb.append("[");
            sb.append(proposal.getPartnerAwardedTime());
            sb.append("]");
            i++;
        }
        return sb.toString();
    }

    @Override
    public String getPartnerRecommendedTime() {
        return getPartnerRecommendedTime(proposals);
    }

    public String getPartnerRecommendedTime(Set<Proposal> proposals) {
        final StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Proposal proposal : proposals) {
            // check that this proposal is really a part of this joint proposal
            Validate.isTrue(proposals.contains(proposal));

            if (i > 0) {
                sb.append(" ");
            }
            sb.append(proposal.getPartnerAbbreviation());
            sb.append("[");
            sb.append(proposal.getPartnerRecommendedTime());
            sb.append("]");
            i++;
        }
        return sb.toString();
    }

    @Override
    public TimeAmount getTotalAwardedTime() {
        return this.getPhaseIProposal().getTotalAwardedTime();
    }

    @Override
    public TimeAmount getTotalRequestedTime() {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);

        for (Submission s : getPhaseIProposal().getSubmissions()) {
            totalTime = totalTime.sum(s.getRequest().getTime());
        }

        return totalTime;
    }

    public TimeAmount getTotalRequestedTime(Set<Proposal> proposals) {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);
        for (Proposal p : proposals) {
            totalTime = totalTime.sum(p.getTotalRequestedTime());
        }
        return totalTime;
    }

    @Override
    public TimeAmount getTotalMinRequestedTime() {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);

        for (Submission s : getPhaseIProposal().getSubmissions()) {
            totalTime = totalTime.sum(s.getRequest().getMinTime());
        }

        return totalTime;
    }

    public TimeAmount getTotalMinRequestedTime(Set<Proposal> proposals) {
        TimeAmount totalTime = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);
        for (Proposal p : proposals) {
            totalTime = totalTime.sum(p.getTotalMinRequestedTime());
        }
        return totalTime;
    }

    @Override
    public TimeAmount getTotalRecommendedTime() {
        return getTotalRecommendedTime(proposals);
    }

    public TimeAmount getTotalRecommendedTime(Set<Proposal> proposals) {
        TimeAmount totalTime = new TimeAmount(new BigDecimal(0), TimeUnit.HR);
        for (Proposal p : proposals) {
            totalTime = totalTime.sum(p.getTotalRecommendedTime());
        }
        return totalTime;
    }

    @Override
    public TimeAmount getTotalMinRecommendTime() {
        return getTotalMinRecommendTime(proposals);
    }

    public TimeAmount getTotalMinRecommendTime(Set<Proposal> proposals) {
        TimeAmount totalTime = new TimeAmount(new BigDecimal(0), TimeUnit.HR);
        for (Proposal p : proposals) {
            totalTime = totalTime.sum(p.getTotalMinRecommendTime());
        }
        return totalTime;
    }

    public Set<Observation> getObservations() {
        return getPrimaryProposal().getObservations();
    }
    public Set<Observation> getAllObservations() {
        return getPrimaryProposal().getAllObservations();
    }
    public List<Target> getTargets() {
        return getPrimaryProposal().getTargets();
    }
    public List<Condition> getConditions() {
        return getPrimaryProposal().getConditions();
    }

    public Set<Observation> getObservationsForBand(Band band) {
        Set<Observation> obs = new HashSet<Observation>();
        for (Observation o : getPrimaryProposal().getObservations()) {
            if (o.getBand() == band) {
                obs.add(o);
            }
        }
        return obs;
    }

    @Override
    public File getPdfLocation(File parentPath) {
        return primaryProposal.getPdfLocation(parentPath);
    }

    public PhaseIProposal getPhaseIProposalForModification() {
        return getPrimaryProposal().getPhaseIProposal();
    }
    private void invalidateDerivedProposal() {
        derivedPhaseIProposal = null;
    }

    /**
     * TODO: It's our belief after some code investigation that this is an artificial restriction that we're imposing.  It
     * is not a business rule, as occasionally it would be useful to force a joint between two proposals of the same partner.
     * It's not a queue engine restriction as that uses a composite key of {partner, partnerReferenceNumber}.  In the absence,
     * of a dedicated amount of time, validating that our understanding is true is going to take a back seat to competing
     * features at this moment, however, we may revisit this.
     *
     * @param newProposal
     */
    
    private void validateNoDuplicatePartners(Proposal newProposal) {
        for (Proposal p : proposals) {
            Validate.isTrue(
                !p.getPartner().equals(newProposal.getPartner()),
                String.format(
                    "Partners must be unique in joint proposals, there is already a component for %s with key %s.",
                    newProposal.getPartner().getName(), newProposal.getPhaseIProposal().getSubmissionsKey())
                );
        }
    }

    @Override
    public void delete(Session session){
        //Need to copy into temp so that component.delete() is valid (otherwise, Hibernate "...re-saved by cascade" problem)
        List<Proposal> components = new ArrayList<Proposal>();
        components.addAll(this.getProposals());
        proposals.clear();
        for(Proposal component : components){
            component.delete(session);
        }
        //Now take care of this
        super.delete(session);
    }
    
    @Override
    public void validate() {
        // joint propsal specific checks
        Validate.notNull(primaryProposal, "Joint proposal must have a primary proposal.");
        Validate.isTrue(proposals.size() >= 2, "Joint proposal must contain at least two proposals.");
    }
}
