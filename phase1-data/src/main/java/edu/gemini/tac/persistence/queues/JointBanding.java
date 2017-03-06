package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.submission.SubmissionAccept;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * JointBandings represents a joint proposal that is part of a queue.
 */

@Entity
@DiscriminatorValue("class edu.gemini.tac.persistence.queues.JointBanding")
public class JointBanding extends Banding {
    private static final Logger LOGGER = Logger.getLogger(JointBanding.class);

    @OneToMany(targetEntity = Banding.class, fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "joint_banding_id")
    private Set<Banding> bandings = new HashSet<Banding>(2);

    /**
     * Creates a JointBanding for a given queue and science band and refers to the given JointProposal.
     *
     * @param queue
     * @param jointProposal
     * @param band
     */
    public JointBanding(final Queue queue, final JointProposal jointProposal, final ScienceBand band) {
        super(queue, jointProposal, band);
    }

    /**
     * Creates a JointBanding for a given queue and science band, a given joint proposal and creates
     * additional bandings for all proposals in the collection with the same band. This is a shortcut
     * to creating the joint banding first and then add the individual proposals by calling the add() method.
     *
     * @param queue
     * @param jointProposal
     * @param proposals
     * @param band
     */
    public JointBanding(final Queue queue, final JointProposal jointProposal, final Collection<Proposal> proposals, final ScienceBand band) {
        this(queue, jointProposal, band);
        for (Proposal proposal : proposals) {
            add(proposal);
        }
    }

    /**
     * Creates a banding for the given proposal for the queue and the band the joint banding belongs to and adds
     * the banding to this joint banding.
     *
     * @param proposal
     */
    public void add(Proposal proposal) {
        add(proposal, this.getBand());
    }

    /**
     * Adds a set of proposals.
     */
    public void add(Collection<Proposal> proposals) {
        for (Proposal p : proposals) {
            add(p);
        }
    }

    /**
     * Creates a banding for the given proposal for the queue the joint banding belongs to with a different
     * band than the joint banding has and adds the new banding to the joint banding.
     *
     * @param proposal
     * @param band
     */
    public void add(Proposal proposal, ScienceBand band) {
        Validate.isTrue(proposal instanceof Proposal); // don't accept joint proposals!
        Banding banding = new Banding(this.getQueue(), proposal, band);
        bandings.add(banding);
    }

    /**
     * Checks if the banded joint proposal has only been partially accepted.
     *
     * @return true if only parts of the joint proposal have been accepted
     */
    public boolean isPartiallyAccepted() {
        JointProposal jp = (JointProposal) getProposal();
        return jp.getProposals().size() != bandings.size();
    }

    /**
     * Gets all proposals of the joint proposal that have been accepted for this queue.
     *
     * @return
     */
    public Set<Proposal> getAcceptedProposals() {
        Set<Proposal> accepted = new HashSet<Proposal>();
        for (Banding banding : bandings) {
            accepted.add(banding.getProposal());
        }
        return accepted;
    }

    /**
     * Gets all proposals of the joint proposal that have been rejected.
     * The sum of the rejected proposals and the banded proposals will always contain all component
     * proposals of the joint proposal.
     *
     * @return set of all component proposals of the banded joint proposal that were rejected
     */
    public Set<Proposal> getRejectedProposals() {
        Set<Proposal> rejected = new HashSet<Proposal>();
        JointProposal jp = (JointProposal) getProposal();
        rejected.addAll(jp.getProposals());
        for (Banding banding : bandings) {
            rejected.remove(banding.getProposal());
        }
        return rejected;
    }

    @Override
    public boolean isJoint() {
        return true;
    }

    @Override
    public ScienceBand getBand() {
        if (bandings != null && bandings.size() > 0) {
            ScienceBand first = (ScienceBand) ((Banding) bandings.toArray()[0]).getBand();
            for (Banding banding : bandings) {
                if (banding.getBand().getRank() != first.getRank()) {
                    return ScienceBand.MULTIBAND;
                }
            }
        }
        return super.getBand();
    }

    /**
     * Gets all bandings of this joint banding.
     *
     * @return set with all bandings for the proposals that were accepted for this joint banding / proposal
     */
    public Set<Banding> getBandings() {
        return bandings;
    }

    public void setBandings(Set<Banding> bandings) {
        this.bandings = bandings;
    }

    public void setProgramId(final String programId) {
        super.setProgramId(programId);

        for (Banding b : getBandings()) {
            b.setProgramId(programId);
        }
    }

    // helper to dynamically create value depending on accepted  proposals
    @Override
    public String getPartnerAbbreviation() {
        return ((JointProposal) getProposal()).getPartnerAbbreviation(getAcceptedProposals());
    }

    // helper to dynamically create value depending on accepted  proposals
    @Override
    public String getRejectedPartnerAbbreviation() {
        return ((JointProposal) getProposal()).getRejectedPartnerAbbreviation(getRejectedProposals());
    }

    // helper to dynamically create value depending on accepted  proposals
    @Override
    public String getPartnerReferenceNumber() {
        return ((JointProposal) getProposal()).getPartnerReferenceNumber(getAcceptedProposals());
    }

    // helper to dynamically create value depending on accepted  proposals
    @Override
    public String getPartnerRanking() {
        return ((JointProposal) getProposal()).getPartnerRanking(getAcceptedProposals());
    }

    @Override
    public String getPartnerRecommendedTime() {
        return ((JointProposal) getProposal()).getPartnerRecommendedTime(getAcceptedProposals());
    }

    @Override
    public TimeAmount getAwardedTime(){
       TimeAmount awardedTime = new TimeAmount(0, TimeUnit.HR);
        for (Proposal accepted : getAcceptedProposals()) {
            SubmissionAccept sa = accepted.getSubmissionsPartnerEntry().getAccept();
            if (sa != null) {
                awardedTime = awardedTime.sum(sa.getRecommend());
            }
        }
        return awardedTime;
    }

    // empty constructor for hibernate
    protected JointBanding() {
    }

}
