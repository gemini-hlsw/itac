package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Proposal;

import javax.persistence.*;

@Entity
@Table(name = "rankings")
public class Ranking implements Comparable<Ranking> {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@OneToOne
	@JoinColumn(name = "queue_id")
	private Queue queue;

	@OneToOne
	@JoinColumn(name = "proposal_id")
	private Proposal proposal;

	@Column(name = "ranking")
	private Long ranking;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public Proposal getProposal() {
		return proposal;
	}

	public void setProposal(Proposal proposal) {
		this.proposal = proposal;
	}

	public Long getRanking() {
		return ranking;
	}

	public void setRanking(Long ranking) {
		this.ranking = ranking;
	}

	@Override
	public String toString() {
		return "Ranking [id=" + id + ", proposal=" + proposal + ", queue="
				+ queue + ", ranking=" + ranking + "]";
	}

	@Override
	public int compareTo(Ranking o) {
		if (ranking < o.getRanking())
			return -1;
		if (ranking > o.getRanking())
			return 1;
		if (id < o.getId())
			return -1;
		if (id > o.getId())
			return 1;

		return 0;
	}
}
