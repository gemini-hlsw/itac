package edu.gemini.tac.service;

import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.service.configuration.IBinConfigurationsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class BinConfigurationTest extends FastHibernateFixture.Basic {
	@Resource(name = "binConfigurationsService")
	private IBinConfigurationsService service;

	@Test
	public void getAllBinConfigurations() {
		final List<BinConfiguration> allBinConfigurations = service.getAllBinConfigurations();
		assertNotNull(allBinConfigurations);
		assertEquals(7, allBinConfigurations.size());
	}

	@Test
	public void update() {
		final List<BinConfiguration> binConfigurations = service.getAllBinConfigurations();
		BinConfiguration original = null;

		for (BinConfiguration bc : binConfigurations) {
			if(bc.getName().equals("Bin Configuration 1"))
				original = bc;
		}
		assertNotNull(original);
		assertEquals(Integer.valueOf(1), original.getSmoothingLength());
		original.setSmoothingLength(Integer.valueOf(6));

		service.updateBinConfiguration(original);

		final List<BinConfiguration> newBinConfigurations = service.getAllBinConfigurations();
		BinConfiguration newBc = null;

		for (BinConfiguration bc : newBinConfigurations) {
			if(bc.getName().equals("Bin Configuration 1"))
				newBc = bc;
		}
		assertNotNull(newBc);
		assertEquals(Integer.valueOf(6), newBc.getSmoothingLength());

		newBc.setSmoothingLength(Integer.valueOf(1));
		service.updateBinConfiguration(newBc);
	}

	@Test
	public void findByName() {
		final BinConfiguration isThere = service.getBinConfigurationByName("Bin Configuration 1");
		assertNotNull(isThere);
		final BinConfiguration isNotThere = service.getBinConfigurationByName("Bin Configuration 321312");
		assertNull(isNotThere);
	}

	@Test
	public void findById() {
        Long some = service.getAllBinConfigurations().get(0).getId();
		final BinConfiguration isThere = service.getBinConfigurationById(some);
		assertNotNull(isThere);
		final BinConfiguration isNotThere = service.getBinConfigurationById(Long.valueOf(-1));
		assertNull(isNotThere);
	}

}
