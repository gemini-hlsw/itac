package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.phase1.submission.Submission;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Bandings tie proposals cleanly to bands for a queue.  The concept is a little overloaded beyond the well-known
 * band one, two and three into the full menagerie described in ScienceBand.
 */
@NamedQueries({
        @NamedQuery(name = "banding.findBandingsByQueue",
                query = "from Banding b " +
                        "join fetch b.queue q " +
                        "where q.id = :queueId and (b.class = JointBanding or b.joint = null)"
        ),
        @NamedQuery(name = "banding.findComponentBandingsByQueue",
                query = "from Banding b " +
                        "join fetch b.queue q " +
                        "where q.id = :queueId and b.class = Banding"
        ),
        @NamedQuery(name = "banding.findAllBandingsByQueue",
                query = "from Banding b " +
                        "join fetch b.queue q " +
                        "where q.id = :queueId"
        )
})

@Entity
@Table(name = "bandings")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "banding_type")
@DiscriminatorValue("class edu.gemini.tac.persistence.queues.Banding")
public class Banding implements Comparable<Banding> {
    private static final Logger LOGGER = Logger.getLogger(Banding.class);
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "queue_id")
    private Queue queue;

    @OneToOne(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "proposal_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Proposal proposal;

    @Embedded
    private ScienceBand band;

    @OneToOne
    @JoinColumn(name = "joint_banding_id")
    protected JointBanding joint;

    @Column(name = "merge_index")
    private Integer mergeIndex;

    @Column(name = "program_id")
    private String programId;

    @SuppressWarnings("unused")
    protected Banding() {
    }

    public Banding(final Queue queue, final Proposal proposal, final ScienceBand band) {
        this.queue = queue;
        this.proposal = proposal;
        this.band = band;
    }

    public Banding(Proposal proposal, ScienceBand band) {
        this.proposal = proposal;
        this.band = band;
    }


    public void setProgramId(final String programId) {
        final Object[] graphPreconditions = {proposal, proposal.getPhaseIProposal(), proposal.getPhaseIProposal().getSubmissions(), proposal.getItac()};
        Validate.noNullElements(graphPreconditions);

        this.programId = programId;
    }

    public String getProgramId() {
        if (programId == null) {
            LOGGER.log(Level.WARN, "Program ID is null -- call Queue.programIds() prior to this call");
        }
        return programId;
    }

    @Override
    public int compareTo(Banding o) {

        if (getBand().getRank() < o.getBand().getRank())
            return -1;
        if (getBand().getRank() > o.getBand().getRank())
            return 1;
        if (getMergeIndex() != null && o.getMergeIndex() != null) {
            if (getMergeIndex() < o.getMergeIndex())
                return -1;
            if (getMergeIndex() > o.getMergeIndex())
                return 1;
        }
        //What if there are nulls?
        boolean thisIdIsNull = proposal.getId() == null;
        boolean thatIdIsNull = o.getProposal().getId() == null;
        if (thisIdIsNull && thatIdIsNull) {
            return 0;
        }
        if (thisIdIsNull && !thatIdIsNull) {
            return -1;
        }
        if (!thisIdIsNull && thatIdIsNull) {
            return 1;
        }

        if (proposal.getId() < o.getProposal().getId())
            return -1;
        if (proposal.getId() > o.getProposal().getId())
            return 1;

        return 0;
    }

    public ScienceBand getBand() {
        return band;
    }

    public Long getId() {
        return id;
    }

    public Queue getQueue() {
        return queue;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setBand(final ScienceBand band) {
        this.band = band;
    }

    public TimeAmount getAwardedTime() {
        TimeAmount allSubEntries = new TimeAmount(0, TimeUnit.HR);
        for (Submission s : proposal.getSubmissionsPartnerEntries().values()) {
            if (s.getAccept() != null) {
                allSubEntries = allSubEntries.sum(s.getAccept().getRecommend());
            }
        }
        return allSubEntries;
    }

    public Integer getMergeIndex() {
        return mergeIndex;
    }

    public void setMergeIndex(Integer mergeIndex) {
        this.mergeIndex = mergeIndex;
    }

    public boolean isJoint() {
        return false;
    }

    public boolean isJointComponent() {
        return joint != null;
    }

    public Set<Banding> getBandings() {
        Set<Banding> smallSet = new HashSet<Banding>();
        smallSet.add(this);
        return smallSet;
    }

    /**
     * Gets the partner abbrevation for the proposal(s) in this banding.
     * This value has to be calculated dynamically for partially accepted proposals in a queue.
     *
     * @return the partner abbreviation (eg. "UK" or "J:UK/US" for joints)
     */
    public String getPartnerAbbreviation() {
        return proposal.getPartnerAbbreviation();
    }

    /**
     * Gets the partner abbreviation for all rejected proposals.
     *
     * @return
     */
    public String getRejectedPartnerAbbreviation() {
        return "";
    }

    /**
     * Gets the partner reference number for the proposal(s) in this banding.
     * This value has to be calculated dynamically for partially accepted proposals in a queue.
     *
     * @return the partner reference number
     */
    public String getPartnerReferenceNumber() {
        return proposal.getPartnerReferenceNumber();
    }

    /**
     * Gets the partner ranking
     * This value has to be calculated dynamically for partially accepted proposals in a queue.
     *
     * @return
     */
    public String getPartnerRanking() {
        return proposal.getPartnerRanking();
    }

    public String getPartnerRecommendedTime() {
        return getProposal().getPartnerRecommendedTime();
    }

    public TimeAmount getTotalAwardedTime() {
        return getProposal().getTotalAwardedTime();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(748369, 544580849).
                appendSuper(super.hashCode()).
                append(queue).
                append(proposal).
                append(band).
                append(mergeIndex).
                toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Banding))
            return false;

        final Banding rhs = (Banding) o;

        return new EqualsBuilder().
                appendSuper(super.equals(o))
                .append(queue, rhs.getQueue())
                .append(proposal, rhs.getProposal())
                .append(band, rhs.getBand())
                .append(mergeIndex, rhs.getMergeIndex())
                .isEquals();
    }

    @Override
    public String toString() {
        return "Banding{" +
                "id=" + id +
                ", queue=" + queue +
                ", proposal=" + proposal +
                ", band=" + band +
                ", mergeIndex=" + mergeIndex +
                ", awardedTime=" + getAwardedTime() +
                ", programId='" + programId + '\'' +
                '}';
    }
}
