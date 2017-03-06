package edu.gemini.tac.service;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.restrictedbin.RestrictedBin;
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
public class RestrictedBinTest extends FastHibernateFixture.WithQueues {
	@Resource(name = "restrictedBinsService")
	private IRestrictedBinsService service;

	@Test
	public void find() {
		final List<RestrictedBin> list = service.getAllRestrictedbins();
		assertNotNull(list);
		assertEquals(2, list.size());
	}
}
