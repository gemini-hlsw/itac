package edu.gemini.tac.persistence;

import edu.gemini.tac.persistence.queues.Queue;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * A single entry in a log.  Committees can have log entries for items
 * that are relevant at that level, for example, creating a new queue,
 * editing membership, et al.
 *
 * Queues also have their own logs containing relevant information, for
 * example, creation time, manual re-ranking.
 *
 * @author ddawson
 *
 */
@Entity
@Table(name = "log_entries")
@NamedQueries({
	@NamedQuery(name = "logEntry.getForCommittee", query = "from LogEntry le where le.committee = :committeeId and le.queue is null order by le.updatedAt desc"),
	@NamedQuery(name = "logEntry.getForQueue", query = "from LogEntry le join fetch le.queue where le.queue = :queueId  order by le.updatedAt desc"),
    @NamedQuery(name = "logEntry.getForProposal", query = "from LogEntry le join fetch le.proposal where le.proposal= :proposalId  order by le.updatedAt desc")
})
public class LogEntry {
	/**
	 * A gross indicator of the kinds of log entry that this represents.  Used
	 * primarily in order to filter.
	 *
	 * @author ddawson
	 *
	 */
	public enum Type {
		SKIPPED,
		PROPOSAL_BULK_IMPORT,
		PROPOSAL_SINGLE_IMPORT,
		PROPOSAL_EDITED,
        PROPOSAL_DELETED,
		PROPOSAL_QUEUE_GENERATED,
		QUEUE_ADDED,
        QUEUE_ERROR,
        QUEUE_FORCE_ADD, //Proposal manually added to a Queue post-creation
		PROPOSAL_ADDED,
		PROPOSAL_SKIPPED,
		PROPOSAL_HAS_NOTE,
        EMAIL,
        PROPOSAL_DUPLICATED,
        PROPOSAL_ROLLOVER_ELIGIBILITY_CHANGED,
        PROPOSAL_JOINT,
        QUEUE_FORCE_REMOVE,
        QUEUE_OBSERVING_MODE_CHANGE,
        QUEUE_REBAND
		;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Column(name = "created_at")
	private Date createdAt;

	@Column(name = "updated_At")
	private Date updatedAt;

	@Column(name = "message")
	private String message;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "log_entry_id")
    @Fetch(FetchMode.SUBSELECT)
	private Set<LogNote> notes;

	@OneToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "queue_id")
	private Queue queue;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "committee_id")
	private Committee committee;

	@ElementCollection(targetClass = Type.class, fetch = FetchType.EAGER)
	@JoinTable(name = "log_entry_types", joinColumns = @JoinColumn(name = "log_entry_id"))
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
    @Fetch(FetchMode.SUBSELECT)
	private Set<Type> types = new HashSet<Type>();

	/**
	 * Builds a single string containing all of the types that this
	 * entry is.
	 *
	 * @return
	 */
	public String getTypeJoin() {
		final StringBuilder builder = new StringBuilder();
		for (Type type : types) {
			builder.append(type);
			builder.append(" ");
		}
		return builder.toString();
	}

	public final Date getCreatedAt() {
		return createdAt;
	}
	public final void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}
	public final Date getUpdatedAt() {
		return updatedAt;
	}
	public final void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	public final String getMessage() {
		return message;
	}
	public final void setMessage(String message) {
		this.message = message;
	}
	public final Set<LogNote> getNotes() {
		return notes;
	}
	public final void setNotes(Set<LogNote> notes) {
		this.notes = notes;
	}
	public final Queue getQueue() {
		return queue;
	}
	public final void setQueue(Queue queue) {
		this.queue = queue;
	}
	public final Committee getCommittee() {
		return committee;
	}
	public final void setCommittee(Committee committee) {
		this.committee = committee;
	}

    public Set<Type> getTypes() {
        return types;
    }

	public void setTypes(Set<Type> types) {
		this.types = types;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }
}
