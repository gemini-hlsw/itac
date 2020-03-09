package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.Target;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import org.hibernate.Session;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Date;

/**
 * In contrast to RolloverObservation objects, which are unique and immutable, these are derivatives that are non-unique
 * and at least potentially mutable.
 * Author: lobrien
 * Date: 3/23/11
 */

@Entity
@DiscriminatorValue("edu.gemini.tac.persistence.rollover.DerivedRolloverObservation")
public class DerivedRolloverObservation extends AbstractRolloverObservation {
    public DerivedRolloverObservation() {

    }

    /* Not a "pure" copy constructor -- clones IProposalElement links */
    public DerivedRolloverObservation(RolloverObservation orig, Session session) {
        if (orig.getId() != null) {
            session.update(orig);
        }
        this.setParent(orig.getParent());
        this.setObservationId(orig.getObservationId());
        this.setObservationTime(new TimeAmount(orig.getObservationTime()));
        this.setCondition(new Condition(orig.getCondition()));
        this.setPartner(orig.getPartner());
        final Target cloneTarget = orig.getTarget().copy(); // Beware, potential semantic change from v1.1 era ITAC which was using a clone registry.
        this.setTarget(cloneTarget);
        //Another "impurity"
        this.setCreatedTimestamp(new Date());
    }
}
