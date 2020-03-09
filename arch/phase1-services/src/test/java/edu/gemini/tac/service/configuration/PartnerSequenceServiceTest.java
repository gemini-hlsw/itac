package edu.gemini.tac.service.configuration;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.service.ICommitteeService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class PartnerSequenceServiceTest extends FastHibernateFixture.WithProposals {
    @Resource(name = "partnerSequenceService")
    IPartnerSequenceService service;

    @Resource(name = "committeeService")
    ICommitteeService committeeService;

    private Long getCommitteeId(){
        // NOTE: committee at position 0 has a partner sequence as part of the database fixture
        // therefore we are using the second committee for our tests which has NO partner sequences
        // (reflect changes to fixture accordingly)
        return committeeService.getAllActiveCommittees().get(1).getId();
    }

    @Test
    public void canCreate() {
        Long id = service.create(getCommitteeId(), "foo", "bar", true);
        Assert.assertNotNull(id);
        Assert.assertTrue(id > 0);
    }

    @Test
    public void canRetrieveById() {
        Long id = service.create(getCommitteeId(), "foo2", "bar", true);
        PartnerSequence ps = service.getForId(id);
        Assert.assertNotNull(ps);
        Assert.assertEquals("foo2", ps.getName());
        Assert.assertEquals("bar", ps.getCsv());
        Assert.assertEquals(true, ps.getRepeat());
    }

    @Test
    public void canRetrieveByName() {
        Long committeeId = getCommitteeId();
        String name = "foo3";
        Long id = service.create(committeeId, name, "bar", true);
        PartnerSequence ps = service.getForName(committeeId, name);
        Assert.assertNotNull(ps);
        Assert.assertEquals(id.longValue(), ps.id());
    }

    @Test
    public void canRetrieveAlLForCommittee() {
        Long id = service.create(getCommitteeId(), "foo", "bar", true);
        Long id1 = service.create(getCommitteeId(), "foo1", "bar", true);
        List<PartnerSequence> pss = service.getAll(getCommitteeId());
        Assert.assertEquals(2, pss.size());
    }

    @Test
    public void canDelete() {
        Long id = service.create(getCommitteeId(), "foo", "bar", true);
        Long id1 = service.create(getCommitteeId(), "foo1", "bar", true);
        List<PartnerSequence> pss = service.getAll(getCommitteeId());
        Assert.assertEquals(2, pss.size());
        service.delete(id);
        Assert.assertEquals(1, service.getAll(getCommitteeId()).size());
        service.delete(getCommitteeId(), "foo1");
        Assert.assertEquals(0, service.getAll(getCommitteeId()).size());

    }
}

