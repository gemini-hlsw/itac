package edu.gemini.tac.persistence;

import edu.gemini.model.p1.mutable.NgoPartner;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.EphemerisElement;
import edu.gemini.tac.persistence.phase1.Itac;
import edu.gemini.tac.persistence.phase1.NonsiderealTarget;
import edu.gemini.tac.persistence.phase1.Target;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static junit.framework.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class Migrate2013ATests extends FastHibernateFixture.WithProposalsLoadOnce {
    @Resource
    SessionFactory sessionFactory;

    @Before
    public void before() {
//        super.teardown();
//        super.before();
        sessionFactory = getSessionFactory();
//        initAll();
//        partners = getPartners();
//        proposals = getProposals();
    }


    @Test
    public void UKisNotAValidPartner() {
        boolean ukExists = false;
        for (Partner p : getPartners()) {
            assertNotSame("UK", p.getAbbreviation());
        }
    }

    @Test
    public void schedulingIsPersistent() {
        final Proposal proposal = getProposal(0);
        final Session session = sessionFactory.openSession();
        final Proposal mergedProposal = (Proposal) session.merge(proposal);
        assertTrue(StringUtils.isEmpty(mergedProposal.getPhaseIProposal().getScheduling()));
        mergedProposal.getPhaseIProposal().setScheduling("Test scheduling string.");
        try {
            session.saveOrUpdate(mergedProposal);
            final Query query = session.createQuery("from Proposal p left join fetch p.phaseIProposal p1p where p.id = :id").setParameter("id", proposal.getId());
            final Proposal queriedProposal = (Proposal) query.uniqueResult();
            assertSame("Test scheduling string.", queriedProposal.getPhaseIProposal().getScheduling());
        } finally {
            session.close();
        }
    }

    @Test
    public void ngoAuthorityIsPersistent() {
        final Proposal proposal = getProposal(0);
        final Session session = sessionFactory.openSession();
        final Proposal mergedProposal = (Proposal) session.merge(proposal);

        Itac itac = mergedProposal.getPhaseIProposal().getItac();
        assertTrue(itac == null || itac.getNgoAuthority() == null);
        if (itac == null) {
            itac = new Itac();
        }
        itac.setNgoAuthority(NgoPartner.AR);
        try {
            session.saveOrUpdate(itac);
            final Long id = itac.getId();
            final Query query = session.createQuery("from Itac i where id = :id").setParameter("id", id);
            final Itac queriedItac = (Itac) query.uniqueResult();
            assertSame(NgoPartner.AR, queriedItac.getNgoAuthority());
        } finally {
            session.close();
        }
    }
}
