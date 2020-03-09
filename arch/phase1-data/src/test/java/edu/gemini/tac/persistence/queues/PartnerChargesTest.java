package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.queues.partnerCharges.*;
import junit.framework.Assert;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class PartnerChargesTest extends FastHibernateFixture.WithQueuesLoadOnce {

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(PartnerChargesTest.class.getName());

    @Test
    @Transactional
    @Ignore
    public void createPartnerCharges() {
        // Confirm fixture's ok
        Queue q = getQueue(0);
        List<Partner> ps = getPartners();
        Assert.assertNotNull(q);
        Assert.assertNotNull(ps.size() > 0);

        // create some partner charges
        Map<Partner, RolloverPartnerCharge> roMap = new HashMap<Partner, RolloverPartnerCharge>();
        Map<Partner, ExchangePartnerCharge> exMap = new HashMap<Partner, ExchangePartnerCharge>();
        Map<Partner, ClassicalPartnerCharge> clMap = new HashMap<Partner, ClassicalPartnerCharge>();
        Map<Partner, AvailablePartnerTime> avMap = new HashMap<Partner, AvailablePartnerTime>();

        for (Partner p : ps) {
            roMap.put(p, new RolloverPartnerCharge(q, p, new TimeAmount(BigDecimal.valueOf(10), TimeUnit.HR)));
            exMap.put(p, new ExchangePartnerCharge(q, p, new TimeAmount(BigDecimal.valueOf(10), TimeUnit.HR)));
            clMap.put(p, new ClassicalPartnerCharge(q, p, new TimeAmount(BigDecimal.valueOf(10), TimeUnit.HR)));
            avMap.put(p, new AvailablePartnerTime(q, p, new TimeAmount(BigDecimal.valueOf(10), TimeUnit.HR)));
        }
        q.setRolloverPartnerCharges(roMap);
        q.setExchangePartnerCharges(exMap);
        q.setClassicalPartnerCharges(clMap);
        q.setAvailablePartnerTimes(avMap);

        Session session = sessionFactory.openSession();
        try {
            session.getTransaction().begin();
            session.saveOrUpdate(q);
            Long id = q.getId();
            Assert.assertNotNull(id);
            session.flush();

            // evict from cache(!!!) and re-read from database to be sure partner charges have been stored properly
            session.evict(q);
            final Queue q2 = (Queue) session.createQuery("from Queue where id = " + id.longValue()).uniqueResult();

            // check partner charges
            Assert.assertEquals(partners.size(), q2.getRolloverPartnerCharges().size());
            Assert.assertEquals(partners.size(), q2.getExchangePartnerCharges().size());
            Assert.assertEquals(partners.size(), q2.getClassicalPartnerCharges().size());
            Assert.assertEquals(partners.size(), q2.getAvailablePartnerTimes().size());
            for (Partner p : partners) {
                Assert.assertEquals(roMap.get(p).getId(), q2.getRolloverPartnerCharges().get(p).getId());
                Assert.assertEquals(exMap.get(p).getId(), q2.getExchangePartnerCharges().get(p).getId());
                Assert.assertEquals(clMap.get(p).getId(), q2.getClassicalPartnerCharges().get(p).getId());
                Assert.assertEquals(avMap.get(p).getId(), q2.getAvailablePartnerTimes().get(p).getId());
            }

        } finally {
            session.close();
        }
    }
}
