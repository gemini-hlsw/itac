package edu.gemini.tac.persistence;

import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.tac.persistence.daterange.*;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.queues.Queue;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.hibernate.LazyInitializationException;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.*;
import java.util.logging.Logger;

/**
 * The fundamental organization that groups most TAC work.  A committee
 * is convened in order to prioritize and rank a set of proposals from partner
 * organizations.  This ranking is committed to and will serve as the basis for
 * the semester's operations.
 * <p/>
 * We're punting down the road the difference between NTAC and ITAC committees. If that refinement becomes necessary,
 * not that AuthorityRole ought probably to change as well (since today we only have ROLE_COMMITTEE but presumably
 * part of the difference would relate to security)
 *
 * @author ddawson
 */
@Entity
@Table(name = "committees")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@NamedQueries({
        @NamedQuery(name = "committee.findAllCommittees",
                query = "from Committee c order by c.created, c.updated",
                hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true")}
        ),
        @NamedQuery(name = "committee.findActiveCommittees",
                query = "from Committee c join fetch c.semester s where c.active = true",
                hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true")}
        ),
        @NamedQuery(name = "committee.findCommitteeById",
                query = "from Committee c join fetch c.semester s left outer join fetch c.partnerSequence where c.id = :id",
                hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true")}
        ),
        //Must support all the data needed by Proposal.getClassString()
        @NamedQuery(name = "committee.findCommitteeForExchangeAnalysis",
                query = "from Committee c " +
                        "left join fetch c.proposals p " +
                        "left join fetch p.phaseIProposal p1p " +
                        "left join fetch p1p.observations " +       // needed for "isBand3()"
                        "left join fetch p1p.blueprints " +
                        "where c.id = :id"
        ),
        @NamedQuery(name = "committee.getCommitteeWithMembers",
                query = "from Committee c left outer join fetch c.members join fetch c.semester s where c.id = :committee_id",
                hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true")}
        ),
        @NamedQuery(name = "committee.getCommitteeWithQueues",
                query = "from Committee c left outer join fetch c.queues left outer join fetch c.partnerSequence where c.id = :committee_id",
                hints = {@QueryHint(name = "org.hibernate.cacheable", value = "true")}
        )
})
public class Committee {
    private static final Logger LOGGER = Logger.getLogger(Committee.class.getName());
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToMany(targetEntity = Person.class)
    @JoinTable(name = "memberships",
            joinColumns = @JoinColumn(name = "committee_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id"))
    private Set<Person> members = new HashSet<Person>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id")
    private Set<Queue> queues;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id")
    private Set<Proposal> proposals = new HashSet<Proposal>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "partner_sequence_id")
    private PartnerSequence partnerSequence;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id")
    private Set<LogEntry> logEntries;

    @Column(name = "active")
    private Boolean active = false;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "committee_id")
    private Set<Shutdown> shutdowns = new HashSet<Shutdown>();

    public Map<String, List<Proposal>> getExchangeTimeProposals() {
        final Map<String, List<Proposal>> exchangeTimeProposals = new TreeMap<String, List<Proposal>>();
        final List<Proposal> fromKeckProposals = new ArrayList<Proposal>();
        final List<Proposal> fromSubaruProposals = new ArrayList<Proposal>();
        final List<Proposal> forKeckProposals = new ArrayList<Proposal>();
        final List<Proposal> forSubaruProposals = new ArrayList<Proposal>();

        for (Proposal p : getProposals()) {
            if (p.isExchange()) {
                // check if this is a proposal FROM Keck or Subaru
                final Partner exchangeFrom = p.getExchangeFrom();
                if (exchangeFrom != null) {
                    if (exchangeFrom.getPartnerCountryKey().equals(ExchangePartner.KECK.name())) {
                        fromKeckProposals.add(p);
                    } else if (exchangeFrom.getPartnerCountryKey().equals(ExchangePartner.SUBARU.name())) {
                        fromSubaruProposals.add(p);
                    }
                }
                // and if not, check if it is FOR Keck or Subaru
                else {
                    final ExchangePartner exchangeFor = p.getExchangeFor();
                    if (exchangeFor != null) {
                        if (exchangeFor.equals(ExchangePartner.KECK)) {
                            forKeckProposals.add(p);
                        } else if (exchangeFor.equals(ExchangePartner.SUBARU)) {
                            forSubaruProposals.add(p);
                        }
                    }
                }
            }
            exchangeTimeProposals.put("from-" + ExchangePartner.KECK.name(), fromKeckProposals);
            exchangeTimeProposals.put("from-" + ExchangePartner.SUBARU.name(), fromSubaruProposals);
            exchangeTimeProposals.put("for-" + ExchangePartner.KECK.name(), forKeckProposals);
            exchangeTimeProposals.put("for-" + ExchangePartner.SUBARU.name(), forSubaruProposals);
        }

        return exchangeTimeProposals;
    }

    public List<Proposal> getClassicalProposals() {
        final List<Proposal> classicalProposals = new LinkedList<Proposal>();
        for (Proposal p : proposals) {
            if (p.isClassical()) {
                classicalProposals.add(p);
            }
        }
        return classicalProposals;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Set<Person> getMembers() {
        return members;
    }

    public Set<Shutdown> getShutdowns(){
        return shutdowns;
    }

    public void setShutdowns(Set<Shutdown> shutdowns){
        this.shutdowns = shutdowns;
    }

    public void addShutdown(Shutdown shutdown){
        getShutdowns().add(shutdown);
    }

    public boolean removeShutdown(Shutdown shutdown){
        boolean wasThere = shutdowns.remove(shutdown);
        shutdown.setCommittee(null);
        return wasThere;
    }

    public void setMemberships(final Set<Person> members) {
        this.members = members;
    }


    public Set<Queue> getQueues() {
        return queues;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public Semester getSemester() {
        return semester;
    }

    public void setSemester(final Semester semester) {
        this.semester = semester;
    }

    public void setLogEntries(Set<LogEntry> logEntries) {
        this.logEntries = logEntries;
    }

    public Set<LogEntry> getLogEntries() {
        return logEntries;
    }

    public void setProposals(Set<Proposal> proposals) {
        this.proposals = proposals;
    }

    public Set<Proposal> getProposals() {
        return proposals;
    }

    /**
     * If null, Committee should use ProportionalPartnerSequence
     *
     * @param partnerSequence
     */
    public void setPartnerSequence(PartnerSequence partnerSequence){
        this.partnerSequence = partnerSequence;
    }

    public PartnerSequence getPartnerSequence(){
        return this.partnerSequence;
    }

    @Column(name = "created")
    private Date created;

    @Column(name = "updated")
    private Date updated;

    public Date getCreated() {
        return created;
    }

    public Date getUpdated() {
        return updated;
    }

    public void setCreated(final Date created) {
        this.created = created;
    }

    public void setUpdated(final Date updated) {
        this.updated = updated;
    }


    // @PrePersist
    // JPA annotations do not seem to be working at the moment.  No more time for investigation, I suspect
    // that it is because state is being managed by the session rather than the entity manager.
    public void onCreate() {
        created = new Date();
    }

    // @PreUpdate
    // JPA annotations do not seem to be working at the moment.  No more time for investigation, I suspect
    // that it is because state is being managed by the session rather than the entity manager.
    public void onUpdate() {
        if (created == null) // Do a ghetto fixup since we didn't orginally have timestamps on these objects.
            created = new Date();
        updated = new Date();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Committee)) return false;

        Committee committee = (Committee) o;

        if (name != null ? !name.equals(committee.name) : committee.name != null) return false;
        if (semester != null ? !semester.equals(committee.semester) : committee.semester != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (semester != null ? semester.hashCode() : 0);
        return result;
    }

    public String toString() {
        try {
            return new ToStringBuilder(this).
                    append("id", id).
                    append("name", name).
                    //        append("memberships", memberships).
                            //        append("queues", queues).
                            append("active", active).
                    toString();
        } catch (LazyInitializationException lie) {
            return "Commitee id[" + id + "] [<--- threw LazyInitializationException --->]";
        }
    }

    public List<Proposal> getProposalsForPartner(final Partner partner) {
        final List<Proposal> partnerProposals = new ArrayList<Proposal>();
        for (Proposal p : proposals)
            if (p.getPartner().equals(partner))
                partnerProposals.add(p);

        return partnerProposals;
    }
}
