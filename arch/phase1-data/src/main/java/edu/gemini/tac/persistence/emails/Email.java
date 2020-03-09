package edu.gemini.tac.persistence.emails;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.queues.Banding;
import edu.gemini.tac.persistence.queues.Queue;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Date;

/**
 * Representation of emails in the context of a committee and the finalized queue for this committee.
 */

@Entity
@Table(name = "emails")
@NamedQueries({
	@NamedQuery(name = "email.findEmailById",
		query = "from Email e " +
				"where e.id = :emailId"
	),
    @NamedQuery(name = "email.getEmailsForQueue",
        query = "from Email e " +
                "where e.queue.id = :queueId "
    )
})

public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "queue_id")
    private Queue queue;

    @ManyToOne
    @JoinColumn(name = "banding_id")
    private Banding banding;

    @ManyToOne
    @JoinColumn(name = "proposal_id")
    private Proposal proposal;

    @Column(name="address")
    private String address;

    @Column(name="subject")
    private String subject;

    @Column(name="content")
    private String content;

    @Column(name="comment")
    private String comment;

    @Column(name="sent_timestamp")
    private Date sentTimestamp;

    @Column(name="failed_timestamp")
    private Date failedTimestamp;

    @Column(name="error")
    private String error;

    @Column(name="cc")
    private String cc;

    public Email() { }

    public Long getId() {
        return id;
    }

    public Queue getQueue() {
        return queue;
    }

    public void setQueue(Queue queue) {
        this.queue = queue;
    }

    public Banding getBanding() {
        return banding;
    }

    public void setBanding(Banding banding) {
        this.banding = banding;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(Date sent) {
        this.sentTimestamp = sent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getErrorTimestamp() {
        return failedTimestamp;
    }

    public void setErrorTimestamp(Date resent) {
        this.failedTimestamp = resent;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    @Transient
    String description;
    public void setDescription(String description){
        this.description = description;
    }

    public String getDescription(){
        return  description;
    }

    public String toString() {
        return new ToStringBuilder(this).
            append("id", id).
            toString();
    }

}
