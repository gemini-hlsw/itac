package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.phase1.Condition;
import edu.gemini.tac.persistence.phase1.SiderealTarget;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import org.hibernate.Session;
import org.w3c.dom.Element;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Date;
import java.util.Map;

/**
 * Implements interface
 *
 * Author: lobrien
 * Date: 3/8/11
 */

@Entity
@DiscriminatorValue("edu.gemini.tac.persistence.rollover.RolloverObservation")
public class RolloverObservation extends AbstractRolloverObservation
{
    public RolloverObservation(){
       this.setCreatedTimestamp(new Date());
    }

    /* Not a "pure" copy constructor -- clones IProposalElement links */
    public RolloverObservation(final RolloverObservation orig, final Session session){
        if(orig.getId() != null){
            session.update(orig);
        }
        this.setParent(orig.getParent());
        this.setObservationId(orig.getObservationId());
        this.setObservationTime(new TimeAmount(orig.getObservationTime()));
        this.setSiteQuality(new Condition(orig.getCondition()));
        this.setPartner(orig.getPartner());
        this.setTarget(orig.getTarget());
        //Another "impurity"
        this.setCreatedTimestamp(new Date());
    }

    public RolloverObservation(final IRolloverObservation orig) {
        this.setObservationId(orig.observationId());
        this.setObservationTime(new TimeAmount(orig.observationTime()));
        this.setTarget(orig.target().copy());
        this.setCondition(new Condition(orig.condition()));
        //Changed timestamp
        this.setCreatedTimestamp(new Date());
    }

    public RolloverObservation(Element el, Map<String, Partner> partnerMap) {
        this.setObservationId(el.getElementsByTagName("id").item(0).getTextContent());
        this.setObservationTime(new TimeAmount((Element) el.getElementsByTagName("time").item(0)));
        this.setSiteQuality(new Condition((Element) el.getElementsByTagName("conditions").item(0)));
        this.setTarget(new SiderealTarget((Element) el.getElementsByTagName("target").item(0)));
        String partnerName = el.getElementsByTagName("partner").item(0).getTextContent();
        // Sorry for this. the odb uses Republic of Korea while ITAC uses Korea
        // I'm too coward to change the name in ITAC so this is the next worse option
        if (partnerName.equalsIgnoreCase("Republic of Korea")) {
          partnerName = "Korea";
        }
        this.setPartner(partnerMap.get(partnerName));
        this.setCreatedTimestamp(new Date());
    }
}
