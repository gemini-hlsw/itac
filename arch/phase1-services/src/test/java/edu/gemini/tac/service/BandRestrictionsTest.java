package edu.gemini.tac.service;

import edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class BandRestrictionsTest extends FastHibernateFixture.Basic {
    @Resource(name = "bandRestrictionsService")
    private IBandRestrictionService service;

    @Test
    public void getAll() {
        final List<BandRestrictionRule> allBandRestrictions = service.getAllBandRestrictions();
        assertNotNull(allBandRestrictions);
        assertEquals(4, allBandRestrictions.size());
    }

}
