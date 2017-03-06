package edu.gemini.tac.service;

import edu.gemini.tac.persistence.condition.ConditionBucket;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import edu.gemini.tac.service.configuration.IConditionsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class ConditionsServiceTest extends FastHibernateFixture.Basic {
	@Resource(name = "conditionsService")
	IConditionsService service;

	@Test
	public void find() {
		final List<ConditionSet> list = service.getAllConditionSets();
		assertNotNull(list);
		assertEquals(2, list.size());
	}

	@Test
	public void testDetails() {
		final List<ConditionSet> list = service.getAllConditionSets();
		final ConditionSet semesterA = list.get(0);
		final ConditionSet semesterB = list.get(1);

		assertEquals("Semester A", semesterA.getName());
		assertEquals("Semester B", semesterB.getName());

		final Set<ConditionBucket> conditions = semesterA.getConditions();
		assertEquals(9, conditions.size());

		ConditionBucket poorSeeingCloudy = null;
		for (ConditionBucket c : conditions) {
			if (c.getName().equals("poor-seeing cloudy"))
				poorSeeingCloudy = c;
		}
		assertNotNull(poorSeeingCloudy);

		assertEquals(Integer.valueOf(20),poorSeeingCloudy.getAvailablePercentage());
		assertEquals(CloudCover.CC_80,poorSeeingCloudy.getCloudCover());
		assertEquals(ImageQuality.IQ_85,poorSeeingCloudy.getImageQuality());
		assertEquals(SkyBackground.SB_100,poorSeeingCloudy.getSkyBackground());
		assertEquals(WaterVapor.WV_100, poorSeeingCloudy.getWaterVapor());
	}

	@Test
	public void updateConditionSet() {
		final List<ConditionSet> allConditionSets = service.getAllConditionSets();
		ConditionSet originalConditionSet = allConditionSets.get(0);
		if(originalConditionSet.getName().equals("Semester B"))
			originalConditionSet = allConditionSets.get(1);

		final Set<ConditionBucket> conditions = originalConditionSet.getConditions();
		ConditionBucket poorSeeingCloudy = null;
		for (ConditionBucket c : conditions) {
			if (c.getName().equals("poor-seeing cloudy")) {
				poorSeeingCloudy = c;
			}
		}
		assertNotNull(poorSeeingCloudy);
		assertEquals(Integer.valueOf(20),poorSeeingCloudy.getAvailablePercentage());

		poorSeeingCloudy.setAvailablePercentage(Integer.valueOf(30));

		service.updateConditionSet(originalConditionSet);

		final List<ConditionSet> nextConditionSets  = service.getAllConditionSets();
		ConditionSet newConditionSet = nextConditionSets.get(0);
		if(newConditionSet.getName().equals("Semester B"))
			newConditionSet = nextConditionSets.get(1);

		ConditionBucket newPoorSeeingCloudy = null;
		for (ConditionBucket c : newConditionSet.getConditions()) {
			if (c.getName().equals("poor-seeing cloudy")) {
				newPoorSeeingCloudy = c;
			}
		}
		assertEquals(Integer.valueOf(30),newPoorSeeingCloudy.getAvailablePercentage());
		newPoorSeeingCloudy.setAvailablePercentage(Integer.valueOf(20));
		service.updateConditionSet(newConditionSet);
	}

	@Test
	public void findByName() {
		final ConditionSet conditionSetByName = service.getConditionSetByName("Semester B");
		assertNotNull(conditionSetByName);
		final ConditionSet noConditionSetByName = service.getConditionSetByName("Semester BC");
		assertNull(noConditionSetByName);
	}

	@Test
	public void findById() {
		final ConditionSet conditionSetById = service.getConditionSetById(Long.valueOf(260));
		assertNotNull(conditionSetById);
		final ConditionSet noConditionSetById = service.getConditionSetById(Long.valueOf(1));
		assertNull(noConditionSetById);
	}

}
