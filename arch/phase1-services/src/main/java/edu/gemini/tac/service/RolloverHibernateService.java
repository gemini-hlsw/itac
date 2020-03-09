package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.rollover.*;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* Implements the IRolloverService via Hibernate persistence
* <p/>
* Note that we also have the RolloverMockService for quick and dirty JSON-based mocking
*/

@Service("rolloverService")
@Transactional
public class RolloverHibernateService implements IRolloverService {
    Logger LOGGER = Logger.getLogger(RolloverHibernateService.class);

    @Resource(name = "sessionFactory")
    private SessionFactory sessionFactory;

    @Override
    public IRolloverReport report(Site site, IPartnerService partnerService) throws RemoteException {
        Session session = sessionFactory.getCurrentSession();
        session.update(site);

        Query query = session.createQuery("from RolloverReport r where r.class=edu.gemini.tac.persistence.rollover.RolloverReport and r.site.displayName = '" + site.getDisplayName() + "'");
        IRolloverReport rpt = (IRolloverReport) query.uniqueResult();
        //Fetch observations
        if (rpt != null && rpt.observations() != null) {
            int i = rpt.observations().size();
            LOGGER.log(Level.DEBUG, "Report has " + i + " observations");
        }
        return rpt;
    }

    @Override
    public RolloverSet createRolloverSet(Site site, String setName, String[] observationIds, String[] timesAllocated) {
        Validate.isTrue(observationIds.length == timesAllocated.length, "Passed in mismatched observations (size=" + observationIds.length + ") and times (size=" + timesAllocated.length + ")");
        Session session = sessionFactory.getCurrentSession();
        session.update(site);

        RolloverSet rolloverSet = new RolloverSet();
        rolloverSet.setSite(site);
        rolloverSet.setName(setName);
        session.save(rolloverSet);

        Set<AbstractRolloverObservation> observations = new HashSet<AbstractRolloverObservation>();
        for (int i = 0; i < observationIds.length; i++) {
            String obId = observationIds[i];
            String timeAllocated = timesAllocated[i];

            /* Order by id desc so that list(0) will be most-recently-created (latest from ODB via Rollover Observation pages) */
            Query query = session.createQuery("from RolloverObservation where observationId = '" + obId + "' order by createdTimestamp desc");
            /*
            There's no way to tell a RO that's come down from
            the ODB other than its (non-unique in our persistence layer) ID, but if they modify it in the DB, so the
            query above has the "order by" and we take the first (i.e., most recent) below.
            */
            RolloverObservation observation = (RolloverObservation) query.list().get(0);
            session.merge(observation);
            DerivedRolloverObservation clone = new DerivedRolloverObservation(observation, session);
            TimeAmount allottedTime = new TimeAmount(Double.parseDouble(timeAllocated), TimeUnit.MIN);
            clone.setObservationTime(allottedTime);
            Target target = clone.getTarget();
            session.save(target);
            session.save(clone);
            observations.add(clone);
        }
        rolloverSet.setObservations(observations);
        session.save(rolloverSet);

        return rolloverSet;
    }

    @Override
    public Set<RolloverSet> rolloverSetsFor(String siteName) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.getNamedQuery("RolloverSet.findBySite").setParameter("siteName", siteName);
        List<RolloverSet> list = (List<RolloverSet>) query.list();
        Set<RolloverSet> set = new HashSet<RolloverSet>(list);
        return set;
    }

    @Override
    public RolloverSet getRolloverSet(Long rolloverSetId) {
        Session session = sessionFactory.getCurrentSession();
        Query query = session.getNamedQuery("RolloverSet.getById").setParameter("id", rolloverSetId);
        RolloverSet set = (RolloverSet) query.uniqueResult();
        //Bring in the coordinates & conditions
        if (set != null && set.getObservations() != null) {
            LOGGER.log(Level.DEBUG, "RolloverSet has " + set.getObservations().size() + " observations");
            Hibernate.initialize(set.getObservations());
            for(AbstractRolloverObservation aro : set.getObservations()){

                Hibernate.initialize(aro.getTarget());
                Target t = aro.getTarget();
                if (t.isSidereal()) {
                    final SiderealTarget siderealTarget = (SiderealTarget) t;
                    Coordinates coordinates = siderealTarget.getCoordinates();

                    // UX-1472: The pattern matching code in the queue engine (as well as any use of "instanceof")
                    // relies on having fully materialized (non-proxied) objects only. Pattern matching will not
                    // work on hibernate proxies. These lines fix an issue where the queue creation broke with
                    // rollovers because the coordinate object was a proxy and not an actual instance of Coordinates
                    // and its subtypes.
                    Hibernate.initialize(coordinates);
                    if (coordinates instanceof HibernateProxy) {
                        coordinates = (Coordinates) ((HibernateProxy) coordinates).getHibernateLazyInitializer()
                                .getImplementation();
                    }
                    siderealTarget.setCoordinates(coordinates);
                }
                Hibernate.initialize(aro.getCondition());
            }
        }
        return set;
    }

    @Override
    public RolloverReport convert(final IRolloverReport odbReport) {
        final Session session = sessionFactory.getCurrentSession();
        final RolloverReport phase1Report = new RolloverReport(odbReport, session);

        session.save(phase1Report);

        return phase1Report;
    }
}
