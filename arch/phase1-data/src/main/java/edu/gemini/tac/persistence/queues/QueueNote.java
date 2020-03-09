package edu.gemini.tac.persistence.queues;

import javax.persistence.*;

@Entity
@Table(name = "queue_notes")
public class QueueNote {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "queue_id")
	private Queue queue;
	
	@Column(name = "note")
	private String note;

	public final Queue getQueue() {
		return queue;
	}

	public final void setQueue(Queue queue) {
		this.queue = queue;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getNote() {
		return note;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}
