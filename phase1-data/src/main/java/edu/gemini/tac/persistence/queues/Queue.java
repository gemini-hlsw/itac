package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule;
import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.persistence.phase1.queues.PartnerPercentage;
import edu.gemini.tac.persistence.queues.comparators.ClassicalProposalComparator;
import edu.gemini.tac.persistence.queues.partnerCharges.*;
import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.annotations.*;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.*;

/**
 * Monster class containing references to everything involved with a queue.
 * The monster is growing.  One of these days it's going to be time to refactor
 * this into smaller pieces and still play nice with hibernate.
 *
 * @author ddawson
 */
@Entity
@Table(name = "queues")
@NamedQueries({
        @NamedQuery(name = "queue.findQueueById",
                query = "from Queue q " +
//                        // load main information for banded proposals
//                        // IF YOU CHANGE THIS CHANGE LINES BELOW ACCORDINGLY
                        "left join fetch q.bandings b " +
                        "left join fetch b.proposal p " +
                        "left join fetch p.partner " +
                        "left join fetch p.phaseIProposal d " +
                        "left join fetch p.issues " +
                        "left join fetch q.classicalProposals cps " +
                        "left join fetch cps.partner " +
                        "left join fetch cps.phaseIProposal " +
                        "left join fetch cps.issues " +
                        "where q.id = :queueId"
        ),
        @NamedQuery(name = "queue.findOnlyQueueWithBandingsById",
                query = "from Queue q " +
                        "left join fetch q.bandings b " +
                        "left join fetch b.proposal p " +
                        "left join fetch p.partner " +
                        "left join fetch p.phaseIProposal d " +
                        "where q.id = :queueId"
        ),
        @NamedQuery(name = "queue.findOnlyQueueWithClassicalProposalsByQueue",
                query = "from Queue q " +
                        "left join fetch q.classicalProposals cps " +
                        "left join fetch cps.partner " +
                        "left join fetch cps.phaseIProposal " +
                        "where q = :queue"
        ),
        @NamedQuery(name = "queue.findFinalizedQueueByCommitteeId",
                query = "from Queue q " +
                        "left join fetch q.committee c " +
                         "left join fetch q.bandings b " +
                        "left join fetch b.proposal p " +
                        "left join fetch p.partner " +
                        "left join fetch p.phaseIProposal d " +
                        "left join fetch d.conditions cs " +
                        "left join fetch p.issues " +
                        "left join fetch q.classicalProposals cps " +
                        "left join fetch cps.issues " +
                        "where q.finalized = true and c.id = :committeeId"
        ),
        @NamedQuery(name = "queue.findQueueOnlyById",
                query = "from Queue q " +
                        "where q.id = :queueId"
        )
})
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Queue {
    private static final Logger LOGGER = Logger.getLogger(Queue.class);

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY) //, cascade = CascadeType.ALL)
    @JoinColumn(name = "committee_id")
    private Committee committee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    @Fetch(FetchMode.JOIN)
    private Site site;

    // Miscellaneous initial conditions
    @Column(name = "total_time_available")
    private Integer totalTimeAvailable;

    @OneToOne(fetch = FetchType.LAZY) //, cascade = CascadeType.ALL)
    @JoinColumn(name = "partner_with_initial_pick_id")
    @Fetch(FetchMode.JOIN)
    private Partner partnerWithInitialPick;

    @OneToOne(fetch = FetchType.LAZY) //, cascade = CascadeType.ALL)
    @JoinColumn(name = "condition_set_id")
    @Fetch(FetchMode.JOIN)
    private ConditionSet conditionSet;

    @OneToOne(fetch = FetchType.LAZY) //, cascade = CascadeType.ALL)
    @JoinColumn(name = "bin_configuration_id")
    @Fetch(FetchMode.JOIN)
    private BinConfiguration binConfiguration;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='Exchange'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, ExchangePartnerCharge> exchangePartnerCharges = new HashMap<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='Rollover'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, RolloverPartnerCharge> rolloverPartnerCharges = new HashMap<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='Classical'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, ClassicalPartnerCharge> classicalPartnerCharges = new HashMap<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='PartnerExchange'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, PartnerExchangePartnerCharge> partnerExchangePartnerCharges = new HashMap<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='Adjustment'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, AdjustmentPartnerCharge> adjustmentPartnerCharges = new HashMap<>();


    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @MapKeyJoinColumn(name = "partner_id")
    @Where(clause = "charge_type='Available'")
    @Fetch(FetchMode.SUBSELECT)
    private Map<Partner, AvailablePartnerTime> availablePartnerTimes = new HashMap<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "queue_proposals_exchange",
            joinColumns = @JoinColumn(name = "queue_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "proposal_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Proposal> exchangeProposals = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "queue_proposals_classical",
            joinColumns = @JoinColumn(name = "queue_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "proposal_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Proposal> classicalProposals = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @Sort(type = SortType.NATURAL)
    @Fetch(FetchMode.SUBSELECT)
    private SortedSet<Banding> bandings = new TreeSet<>();

    @OneToMany(fetch = FetchType.LAZY,cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "queue_id")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    private Set<LogEntry> logEntries = new HashSet<>();

    // Band specific conditions
    @Column(name = "band_1_cutoff")
    private Integer band1Cutoff = 30;
    @Column(name = "band_2_cutoff")
    private Integer band2Cutoff = 60;
    @Column(name = "band_3_cutoff")
    private Integer band3Cutoff = 80;
    @Column(name = "band_3_conditions_threshold")
    private Integer band3ConditionsThreshold;
    @Column(name = "use_after_band_3")
    private Boolean useBand3AfterThresholdCrossed;

    //Queue strategy for Subaru
    @Column(name = "subaru_scheduled_by_queue_engine")
    private Boolean subaruScheduledByQueueEngine;

    //Where to hold the Subaru proposals *eligible* for being scheduled in the Queue ("scheduled as Partner")
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "queue_subaru_exchange_proposals_eligible_for_queue",
            joinColumns = @JoinColumn(name = "queue_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "proposal_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<Proposal> subaruProposalsEligibleForQueue = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="queue_id")
    @Fetch(FetchMode.SUBSELECT)
    private Set<PartnerPercentage> partnerPercentages = new HashSet<>();

    // Restriction conditions
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "queue_band_restriction_rules",
            joinColumns = @JoinColumn(name = "queue_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "band_restriction_rule_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<BandRestrictionRule> bandRestrictionRules;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "queue_restricted_bins",
            joinColumns = @JoinColumn(name = "queue_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "restricted_bin_id", referencedColumnName = "id")
    )
    @Fetch(FetchMode.SUBSELECT)
    private Set<RestrictedBin> restrictedBins;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name = "queue_id")
    @Fetch(FetchMode.SUBSELECT)
    private Set<QueueNote> notes;

    @Column(name = "name")
    private String name;

    @Column(name = "created_timestamp")
    private Date createdTimestamp;

    @Column(name = "finalized")
    private Boolean finalized = Boolean.FALSE;

    @Column(name = "dirty")
    private Boolean dirty = Boolean.FALSE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "rolloverset_id")
    private RolloverSet rolloverset;

    @Column(name = "overfill_limit")
    private Integer overfillLimit = 5;

    @SuppressWarnings("unused")
    public Queue() {
    }

    public Queue(final String name, final Committee committee) {
        this.name = name;
        this.committee = committee;
        this.createdTimestamp = new Date();
    }

    public Committee getCommittee() {
        return committee;
    }

    public void setRestrictedBins(Set<RestrictedBin> restrictedBins) {
        this.restrictedBins = restrictedBins;
    }

    public Set<RestrictedBin> getRestrictedBins() {
        return restrictedBins;
    }

    public Long getId() {
        return id;
    }

    public Date getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getName() {
        return name;
    }

    public Boolean getFinalized() {
        return finalized;
    }

    public void setFinalized(Boolean finalized) {
        this.finalized = finalized;
    }

    public Boolean getDirty() {
        return dirty;
    }

    public void setDirty(Boolean dirty) {
        this.dirty = dirty;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Integer getTotalTimeAvailable() {
        return totalTimeAvailable;
    }

    public void setTotalTimeAvailable(Integer totalTimeAvailable) {
        this.totalTimeAvailable = totalTimeAvailable;
    }

    public Partner getPartnerWithInitialPick() {
        return partnerWithInitialPick;
    }

    public void setPartnerWithInitialPick(Partner partnerWithInitialPick) {
        this.partnerWithInitialPick = partnerWithInitialPick;
    }

    public ConditionSet getConditionSet() {
        return conditionSet;
    }

    public void setConditionSet(ConditionSet conditionSet) {
        this.conditionSet = conditionSet;
    }

    public BinConfiguration getBinConfiguration() {
        return binConfiguration;
    }

    public void setBinConfiguration(BinConfiguration binConfiguration) {
        this.binConfiguration = binConfiguration;
    }

    public Integer getBand1Cutoff() {
        return band1Cutoff;
    }

    public void setBand1Cutoff(Integer band1Cutoff) {
        this.band1Cutoff = band1Cutoff;
    }

    public Integer getBand2Cutoff() {
        return band2Cutoff;
    }

    public void setBand2Cutoff(Integer band2Cutoff) {
        this.band2Cutoff = band2Cutoff;
    }

    public Integer getBand3Cutoff() {
        return band3Cutoff;
    }

    public void setBand3Cutoff(Integer band3Cutoff) {
        this.band3Cutoff = band3Cutoff;
    }

    /**
     * Returns how much we are allowed to go past the band 3 cutoff as a
     * percentage of a partner's available queue time.  (ITAC-441)  The queue
     * service will rejected this queue config if negative, but it can be
     * unspecified (null) or zero.
     */
    public Integer getOverfillLimit() {
        // Hardcoded to 5, which should be the default.  UI can be updated to
        // set this value.
        return overfillLimit;
    }

    public Integer getBand3ConditionsThreshold() {
        return band3ConditionsThreshold;
    }

    public void setBand3ConditionsThreshold(Integer band3ConditionsThreshold) {
        this.band3ConditionsThreshold = band3ConditionsThreshold;
    }

    public Boolean getUseBand3AfterThresholdCrossed() {
        return useBand3AfterThresholdCrossed;
    }

    public void setUseBand3AfterThresholdCrossed(
            Boolean useBand3AfterThresholdCrossed) {
        this.useBand3AfterThresholdCrossed = useBand3AfterThresholdCrossed;
    }

    public Set<BandRestrictionRule> getBandRestrictionRules() {
        return bandRestrictionRules;
    }

    public void setBandRestrictionRules(
            Set<BandRestrictionRule> bandRestrictionRules) {
        this.bandRestrictionRules = bandRestrictionRules;
    }

    public Set<QueueNote> getNotes() {
        return notes;
    }

    public void setNotes(Set<QueueNote> notes) {
        this.notes = notes;
    }

    public void setBandings(SortedSet<Banding> bandings) {
        this.bandings = bandings;
    }

    public void setSubaruScheduledByQueueEngine(boolean useQueueEngine){
        this.subaruScheduledByQueueEngine = useQueueEngine;
    }

    public Boolean getSubaruScheduledByQueueEngine(){
        return this.subaruScheduledByQueueEngine;
    }

    /**
     * Adds all bandings in the collection to this queue. Also adds component bandings of joint bandings
     * in the collection, these bandings must not be added again!
     *
     * @param bandings
     */
    public void addAllBandings(Collection<Banding> bandings) {
        SortedSet newSet = new TreeSet(this.bandings);
        for (Banding banding : bandings) {
            if (banding instanceof JointBanding) {
                for (Banding componentBanding : ((JointBanding) banding).getBandings()) {
                    newSet.add(componentBanding);
                }
            } else {
                newSet.add(banding);
            }
        }
        this.bandings = newSet;
    }

    /**
     * Adds a joint banding and all its component bandings to the queue.
     *
     * @param jointBanding
     */
    public void addBanding(JointBanding jointBanding) {
        SortedSet newSet = new TreeSet(this.bandings);
        newSet.add(jointBanding);
        for (Banding banding : jointBanding.getBandings()) {
            newSet.add(banding);
        }
        this.bandings = newSet;
    }

    /**
     * Adds a banding to the queue.
     *
     * @param banding
     */
    public void addBanding(Banding banding) {
        SortedSet<Banding> newSet = new TreeSet<>(this.bandings);
        newSet.add(banding);
        this.bandings = newSet;
    }

    public void removeBanding(Banding banding) {
        final SortedSet<Banding> queueBandings = getBandings();
        final HashSet<Banding> hackBandings = new HashSet<>(queueBandings);
        hackBandings.addAll(queueBandings);
        hackBandings.remove(banding);
        bandings = new TreeSet<>(hackBandings);
        // We need to remove the banding from the queue it's associated with as well as delete it from the database.
        // There is an interaction going on between SortedSet remove and the Banding compareTo that is preventing
        // me from just calling queueBandings.remove(banding).  Usually remove would be depend on equal, but
        // SortedSet appears to be using compareTo instead (new behavior to me) which I can see will be quicker
        // for finding the element, but we never seem to run across it.  Something is wrong with our compareTo in
        // Banding or it's component objects equals.  I have not tracked this down and instead am just doing
        // very old-school (and inefficient) iterator style removal.
//        final Iterator<Banding> iterator = queueBandings.iterator();
//        while (iterator.hasNext()) {
//            Banding iteratorBanding = iterator.next();
//            if (iteratorBanding.equals(banding)) {
//                LOGGER.debug("Found banding to remove.");
//                iterator.remove();
//                break;
//            }
//        }

        if (banding.isJoint()) {
            final Set<Banding> bandingBandings = banding.getBandings();
            for (Banding b : bandingBandings) {
                Queue queue = b.getQueue();
                if (queue != null)
                    queue.removeBanding(b);
            }
        }
    }

    public SortedSet<Banding> getBandings() {
        return bandings;
    }

    public void setExchangePartnerCharges(Map<Partner, ExchangePartnerCharge> exchangePartnerCharges) {
        this.exchangePartnerCharges = exchangePartnerCharges;
    }

    public Map<Partner, ExchangePartnerCharge> getExchangePartnerCharges() {
        return exchangePartnerCharges;
    }

    public void setRolloverPartnerCharges(Map<Partner, RolloverPartnerCharge> rolloverPartnerCharges) {
        this.rolloverPartnerCharges = rolloverPartnerCharges;
    }

    public Map<Partner, RolloverPartnerCharge> getRolloverPartnerCharges() {
        return rolloverPartnerCharges;
    }

    public void setClassicalPartnerCharges(Map<Partner, ClassicalPartnerCharge> classicalPartnerCharges) {
        this.classicalPartnerCharges = classicalPartnerCharges;
    }

    public Map<Partner, ClassicalPartnerCharge> getClassicalPartnerCharges() {
        return classicalPartnerCharges;
    }

    public void setAvailablePartnerTimes(Map<Partner, AvailablePartnerTime> availablePartnerTimes) {
        this.availablePartnerTimes = availablePartnerTimes;
    }

    public Map<Partner, AvailablePartnerTime> getAvailablePartnerTimes() {
        return availablePartnerTimes;
    }

    public void setExchangeProposals(Set<Proposal> exchangeProposals) {
        this.exchangeProposals = exchangeProposals;
    }

    public Set<Proposal> getExchangeProposals() {
        return exchangeProposals;
    }

    public void setSubaruProposalsEligibleForQueue(Set<Proposal> subaruExchangeProposals){
        this.subaruProposalsEligibleForQueue = subaruExchangeProposals;
    }

    public Set<Proposal> getSubaruProposalsEligibleForQueue(){
        return subaruProposalsEligibleForQueue;
    }

    public void setClassicalProposals(Set<Proposal> classicalProposals) {
        this.classicalProposals = classicalProposals;
    }

    public Set<Proposal> getClassicalProposals() {

        return classicalProposals;
    }

    public List<Proposal> getCopyOfClassicalProposalsSorted() {
        final List<Proposal> sortedClassicalProposals = new ArrayList<Proposal>(classicalProposals.size());
        sortedClassicalProposals.addAll(classicalProposals);
        Collections.sort(sortedClassicalProposals, new ClassicalProposalComparator());

        return sortedClassicalProposals;
    }

    public void setRolloverset(RolloverSet rolloverSet) {
        this.rolloverset = rolloverSet;
    }

    public RolloverSet getRolloverSet() {
        return this.rolloverset;
    }

    public Map<Partner, PartnerExchangePartnerCharge> getPartnerExchangePartnerCharges() {
        return partnerExchangePartnerCharges;
    }

    public Map<Partner, AdjustmentPartnerCharge> getAdjustmentPartnerCharges() {
        return adjustmentPartnerCharges;
    }

    public String getDetailsString() {
        StringBuffer sb = new StringBuffer();
        sb.append(site.getDisplayName());
        if (finalized != null && finalized) {
            sb.append(" - final");
        }
        if (dirty != null && dirty) {
            sb.append(" - !INCONSISTENT!");
        }
        return sb.toString();
    }

    /**
     * Supports testing.
     *
     * @param id
     */
    protected void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(117236111, 26276197).
                appendSuper(super.hashCode()).
                append(committee).
                append(name).
                toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Queue))
            return false;

        final Queue rhs = (Queue) o;

        return new EqualsBuilder().
                appendSuper(super.equals(o))
                .append(committee, rhs.getCommittee())
                .append(name, rhs.getName())
                .isEquals();
    }

    public String toString() {
        return new ToStringBuilder(this).
//			append("committee", committee).
//			append("site", site).
//			append("totalTimeAvailable", totalTimeAvailable).
//			append("partner", partnerWithInitialPick).
//			append("partnerQuanta", partnerQuanta).
//			append("conditionSet", conditionSet).
//			append("binConfiguration", binConfiguration).
//			append("lptacTime", lptacParticipants).
//			append("band1Cutoff", band1Cutoff).
//			append("band2Cutoff", band2Cutoff).
//			append("band3Cutoff", band3Cutoff).
//			append("band3ConditionsThreshold", band3ConditionsThreshold).
//			append("useBand3AfterThresholdCrossed", useBand3AfterThresholdCrossed).
//			append("bandRestrictionRules", bandRestrictionRules).
//			append("restrictedBin", getRestrictedBins()).
//			append("rolloverAllocation", rolloverAllocation).
//			append("notes", notes).
//			append("name", getName()).
        toString();
    }

    List<Banding> bandingsFor(ScienceBand band) {
        List<Banding> bandings = new ArrayList<Banding>();
        for (Banding banding : getBandings()) {
            if (banding.getBand().getRank() == band.getRank()) {
                bandings.add(banding);
            }
        }
        return bandings;
    }

    public Map<ScienceBand, Map<String, Banding>> programIds(boolean pINamesSortedAscending) {
        Comparator piNameComparator = new PINameComparator(pINamesSortedAscending);
        Map<ScienceBand, Map<String, Banding>> pids = new HashMap<ScienceBand, Map<String, Banding>>();

        int lpCounter = 0;
        int band1Counter = 100;
        int band2Counter = 200;
        int band3Counter = 300;
        int band4Counter = 400;
        for (ScienceBand band : ScienceBand.byProgramIdOrder()) {
            Map<String, Banding> byProgramId = new HashMap<String, Banding>();
            pids.put(band, byProgramId);
            List<Banding> bandings = this.bandingsFor(band);
            Collections.sort(bandings, piNameComparator);

            for (Banding banding : bandings) {
                if (!banding.isJointComponent()) {
                    String programId;
                    if (banding.getProposal().isLargeProgram()) {
                        programId = banding.getProposal().createProgramId(++lpCounter);
                    } else if (banding.getBand().getRank() == 1) {
                        if (band1Counter == 199) {
                            throw new java.lang.Error("Band 1 program count reached its maximum");
                        }
                        programId = banding.getProposal().createProgramId(++band1Counter);
                    } else if (banding.getBand().getRank() == 2) {
                        if (band2Counter == 299) {
                            throw new java.lang.Error("Band 2 program count reached its maximum");
                        }
                        programId = banding.getProposal().createProgramId(++band2Counter);
                    } else if (banding.getBand().getRank() == 3) {
                        if (band3Counter == 399) {
                            throw new java.lang.Error("Band 3 program count reached its maximum");
                        }
                        programId = banding.getProposal().createProgramId(++band3Counter);
                    } else {
                        if (band4Counter == 499) {
                            throw new java.lang.Error("Band 4 program count reached its maximum");
                        }
                        programId = banding.getProposal().createProgramId(++band4Counter);
                    }
                    banding.setProgramId(programId);
                    byProgramId.put(programId, banding);
                }
            }
        }

        return pids;
    }

    public Map<ScienceBand, Map<String, Banding>> programIds(boolean pINamesSortedAscending, Session session) {
        Map<ScienceBand, Map<String, Banding>> pids = programIds(pINamesSortedAscending);
        for (Map<String, Banding> bandings : pids.values()) {
            for (Banding banding : bandings.values()) {
                session.save(banding);
            }
        }
        return pids;
    }

    public boolean remove(Proposal proposal) {
        Banding oneToRemove = null;
        for (Banding b : bandings) {
            final Proposal p = b.getProposal();
            if (p.getEntityId().longValue() == proposal.getEntityId().longValue()) {
                oneToRemove = b;
                break;
            }
        }
        if (oneToRemove != null) {
            bandings.remove(oneToRemove);
            return true;
        }
        Proposal proposalToRemove = null;
        for (Proposal p : classicalProposals) {
            if (p.getEntityId().longValue() == proposal.getEntityId().longValue()) {
                proposalToRemove = p;
                break;
            }
        }
        if (proposalToRemove != null) {
            classicalProposals.remove(proposalToRemove);
            return true;
        }
        for (Proposal p : exchangeProposals) {
            if (p.getEntityId().longValue() == proposal.getEntityId().longValue()) {
                proposalToRemove = p;
                break;
            }
        }
        if (proposalToRemove != null) {
            exchangeProposals.remove(proposalToRemove);
            return true;
        }
        return false;
    }

    public Banding getBandingFor(Proposal proposal) {
        for (Banding b : bandings) {
            if (b.getProposal().getEntityId().longValue() == proposal.getEntityId().longValue()) {
                return b;
            }
        }
        return null;
    }

    public SortedSet<Banding> getBandingsFor(ScienceBand band) {
        SortedSet<Banding> bs = new TreeSet<Banding>();
        for (Banding b : getBandings()) {
            if (b.getBand().getRank() == band.getRank()) {
                bs.add(b);
            }
        }
        return bs;
    }


    class PINameComparator implements Comparator<Banding> {
        private boolean ascendingSort;

        PINameComparator(boolean ascendingSort) {
            this.ascendingSort = ascendingSort;
        }

        public int compare(Banding one, Banding two) {
            final String nameOne = one.getProposal().getPhaseIProposal().getInvestigators().getPi().getLastName().toLowerCase().trim();
            final String nameTwo = two.getProposal().getPhaseIProposal().getInvestigators().getPi().getLastName().toLowerCase().trim();
            if (ascendingSort) {
                return nameOne.compareTo(nameTwo);
            } else {
                return nameTwo.compareTo(nameOne);
            }
        }
    }

    public void setOverfillLimit(final Integer overfillLimit) {
        this.overfillLimit = overfillLimit;
    }

    public Set<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void setLogEntries(Set<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    public Set<PartnerPercentage> getPartnerPercentages(){
        /*
        Would this be a wortwhile runtime check?
        double total = 0.0;
        for(PartnerPercentage pp : partnerPercentages){
            total += pp.getPercentage()
        }
        Validate.isEqual(1.0, total, Double.MIN_VALUE);
        */
        return partnerPercentages;
    }

    public void setPartnerPercentages(Set<PartnerPercentage> partnerPercentages){
        this.partnerPercentages = partnerPercentages;
    }
}


