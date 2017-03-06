package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.Date;

/**
 * Helper class to implement the i'face
 *
 * Author: lobrien
 * Date: 3/24/11
 */

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "rollover_observations")
@DiscriminatorColumn(name = "type")
public abstract class AbstractRolloverObservation implements IRolloverObservation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    Partner partner;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    Target target;

    @ManyToOne
    @JoinColumn(name = "rollover_report_id")
    RolloverReport parent; //Note that this could be a RolloverSet (subtype)

    /**
     * New data schema is removing the intermediate site quality, and instead naming that container as a condition.
     */
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_id")
    @org.hibernate.annotations.Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    protected Condition condition;


    @Embedded
    TimeAmount observationTime;

    @Column(name="created_timestamp")
    private Date createdTimestamp;

    @Column(name = "observation_id_reference")
    String observationId;

    public Partner getPartner() {
        return partner;
    }

    public void setPartner(Partner partner) {
        this.partner = partner;
    }

    public String getObservationId() {
        return observationId;
    }

    public void setObservationId(String observationId) {
        this.observationId = observationId;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public Condition getSiteQuality() {
        return getCondition();
    }

    public Condition getCondition() {
        return condition;
    }

    public void setSiteQuality(final Condition condition) {
        setCondition(condition);
    }

    public void setCondition(final Condition condition) {
        this.condition = condition;
    }

    public TimeAmount getObservationTime() {
        return observationTime;
    }

    public void setObservationTime(TimeAmount time) {
        this.observationTime = time;
    }

    public RolloverReport getParent() {
        return this.parent;
    }

    public void setParent(RolloverReport parent) {
        this.parent = parent;
    }

    public Long getId() {
        return this.id;
    }

    @Override
    public Partner partner() {
        return getPartner();
    }

    @Override
    public String observationId() {
        return getObservationId();
    }

    @Override
    public Target target() {
        return getTarget();
    }

    @Override
    public Condition siteQuality() {
        return condition;
    }

    @Override
    public Condition condition() {
        return condition;
    }

    @Override
    public TimeAmount observationTime() {
        return getObservationTime();
    }

    public Date getCreatedTimestamp(){
        return createdTimestamp;
    }

    public void setCreatedTimestamp(Date timestamp){
        this.createdTimestamp = timestamp;
    }
}
