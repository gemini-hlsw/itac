package edu.gemini.tac.persistence;

import javax.persistence.*;

/**
 * Arbitrary free form notes that attach to log entries so that
 * tac participants can record and share reasoning behind actions
 * and events.
 * 
 * @author ddawson
 *
 */
@Entity
@Table(name = "log_notes")
public class LogNote {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@ManyToOne
	@JoinColumn(name = "log_entry_id")
	LogEntry logEntry;
	
	@Column(name = "note_text")
	private String text;

	public LogNote() {};
	public LogNote(final String text) { this.text = text; };

    public LogEntry getLogEntry() {
        return logEntry;
    }

    public void setText(String text) {
        this.text = text;
    }

	public String getText() {
		return text;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
}
