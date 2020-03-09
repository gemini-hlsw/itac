package edu.gemini.tac.service;

import edu.gemini.shared.util.DateRange;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.daterange.DateRangePersister;
import edu.gemini.tac.persistence.daterange.Shutdown;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class ShutdownServiceTest extends FastHibernateFixture.With2012BProposalsLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(ShutdownServiceTest.class.getName());

    @Resource(name = "shutdownService")
    private IShutdownService shutdownService;

    Long committeeId = 1002333L;

    @Resource(name = "committeeService")
    private ICommitteeService committeeService;

    @Resource(name = "siteService")
    private ISiteService siteService;

    @Test
    @Transactional
    public void startsWithNone() {
        final List<Shutdown> allShutdowns = shutdownService.forCommittee(committeeId);
        assertNotNull(allShutdowns);
        assertEquals(0, allShutdowns.size());
    }

    @Test
    @Transactional
    public void canSaveOne() {
        Shutdown shutdown = saveNewShutdown();
        assertNotNull(shutdown.getId());
    }

    @Test
    @Transactional
    public void canRetrieveOne(){
        Shutdown saved = saveNewShutdown();
        Long id = saved.getId();
        Shutdown retrieved = shutdownService.forId(id);
        assertNotNull(retrieved);
    }

    @Test
    @Transactional
    public void canRetrieveForCommittee(){
       Shutdown sd = saveNewShutdown();
       saveNewShutdown();
       Long committeeId = sd.getCommittee().getId();
       assertNotNull(committeeId);
       List<Shutdown> sds = shutdownService.forCommittee(committeeId);
       assertEquals(3, sds.size());
       assertTrue(sds.remove(sd));
    }

    @Test
    @Transactional
    public void canDelete(){
        Shutdown saved = saveNewShutdown();
        Long committeeId = saved.getCommittee().getId();
        Long id = saved.getId();
        shutdownService.delete(id);
        sessionFactory.getCurrentSession().flush();
        List<Shutdown> ss = shutdownService.forCommittee(committeeId);
        boolean removedSoPresent = ss.remove(saved);
        assertFalse(removedSoPresent);
    }

    private Shutdown saveNewShutdown() {
        Committee committee = committeeService.getAllCommittees().get(0);
        assertNotNull(committee);
        assertNotNull(committee.getId());
        Site site = siteService.findByDisplayName("North");
        assertNotNull(site);
        DateTime start = new DateTime(2012, 3, 26, 12, 0, 0, 0);
        DateTime stop = start.plus(Period.days(1));
        DateRange dr = new DateRange(start.toDate(), stop.toDate());
        DateRangePersister drp = new DateRangePersister(dr);
        Shutdown shutdown = new Shutdown(drp, site, committee);
        assertNotNull(shutdown);
        shutdownService.save(shutdown);
        sessionFactory.getCurrentSession().flush();
        return shutdown;
    }
}
