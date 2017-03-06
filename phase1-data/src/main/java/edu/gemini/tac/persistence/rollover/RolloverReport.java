package edu.gemini.tac.persistence.rollover;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.*;
import org.hibernate.Session;
import org.hibernate.annotations.Cascade;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.persistence.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Persists RolloverReports...
 * <p/>
 * Author: lobrien
 * Date: 3/8/11
 */

@Entity
@Table(name = "rollover_reports")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "report_type")
@DiscriminatorValue("class edu.gemini.tac.persistence.rollover.RolloverReport")

public class RolloverReport implements IRolloverReport {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public RolloverReport() {
    }

    public RolloverReport(IRolloverReport odbReport, Session session) {
        final Site mySite =
                (Site) session.createQuery("from Site s where s.displayName = :displayName").
                        setString("displayName", odbReport.site().getDisplayName()).uniqueResult();

        this.setSite(mySite);

        session.save(this);
        observations = new HashSet<AbstractRolloverObservation>();
        for (IRolloverObservation o : odbReport.observations()) {
            RolloverObservation oClone = new RolloverObservation(o);
            Target target = oClone.getTarget();
            if (target.getName() == null) {
                target.setName("");
            }
            //Bring in the coordinates
            if(target instanceof SiderealTarget){
                final Coordinates coordinates = ((SiderealTarget) target).getCoordinates();
            }
            session.saveOrUpdate(target);
            oClone.setParent(this);
            final String partnerCountryKey = o.partner().getPartnerCountryKey();
            Partner p = Partner.forCountryKey(partnerCountryKey, session);
            oClone.setPartner(p);
            observations.add(oClone);
            session.save(oClone);
        }
    }

    @Override
    public Site site() {
        return getSite();
    }

    @Override
    public Set<IRolloverObservation> observations() {
        Set<IRolloverObservation> upcastSet = new HashSet<IRolloverObservation>();
        upcastSet.addAll(getObservations());
        return upcastSet;
    }

    @OneToOne(fetch = FetchType.EAGER)
    Site site;

    @OneToMany(targetEntity = AbstractRolloverObservation.class)
    @JoinColumn(name = "rollover_report_id")
    @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
    Set<AbstractRolloverObservation> observations;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public Set<AbstractRolloverObservation> getObservations() {
        return observations;
    }

    public void setObservations(Set<AbstractRolloverObservation> observations) {
        this.observations = observations;
    }

    public long getTotalHours() {
        TimeAmount accumulator = new TimeAmount(BigDecimal.ZERO, TimeUnit.HR);
        for (IRolloverObservation o : getObservations()) {
            accumulator = accumulator.sum(o.observationTime());
        }
        return accumulator.getValue().longValue();
    }

    public static RolloverReport parse(String s, Map<String, Partner> partnerMap) {
        try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource src = new InputSource(new StringReader(s));
            Document document = builder.parse(src);
            return parse(document, partnerMap);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public static RolloverReport parse(Document doc, Map<String, Partner> partnerMap) {
        return new RolloverReport(doc.getDocumentElement(), partnerMap);
    }

    public RolloverReport(Element rolloverElement, Map<String, Partner> partnerMap) {
        String siteAttribute = rolloverElement.getAttribute("site");
        setSite(siteAttribute.equals("GS") ? Site.SOUTH : Site.NORTH);
        observations = new HashSet<AbstractRolloverObservation>();
        final NodeList nodeList = rolloverElement.getElementsByTagName("obs");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element el = (Element) nodeList.item(i);
            RolloverObservation o = new RolloverObservation(el, partnerMap);
            observations.add(o);
        }
    }


}
