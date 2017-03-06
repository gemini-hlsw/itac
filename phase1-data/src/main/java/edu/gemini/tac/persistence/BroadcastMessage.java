package edu.gemini.tac.persistence;

/**
 * Intended to be used to communicate current information with all users on the site.  For example, "ITAC will be going
 * down for maintenance in 1 hour at 2:15 HST".
 */

import org.apache.log4j.Logger;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "broadcast_messages")
@NamedQueries({
        @NamedQuery(name = "broadcastMessage.findLatest",
                query = "from BroadcastMessage bm order by bm.created desc"
        )}
)
public class BroadcastMessage {
    private static final Logger LOGGER = Logger.getLogger(BroadcastMessage.class.getName());

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "message")
    private String message;

    @Column(name = "created")
    private Date created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BroadcastMessage)) return false;

        BroadcastMessage that = (BroadcastMessage) o;

        if (created != null ? !created.equals(that.created) : that.created != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (created != null ? created.hashCode() : 0);
        return result;
    }
}
