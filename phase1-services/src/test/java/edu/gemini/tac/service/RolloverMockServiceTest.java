package edu.gemini.tac.service;

/**
 * TODO: Why does this class exist?
 * Author: lobrien
 * Date: 3/8/11
 */

/*
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class RolloverMockServiceTest extends DatabaseFixture {
    @Test
    public void canReadJsonStuff() throws IOException{
        Site site = Mockito.mock(Site.class);
        Mockito.when(site.getDisplayName()).thenReturn("North");

        IRolloverService rolloverService = new RolloverMockService();
        IRolloverReport rpt = rolloverService.report(site, null);
        Assert.assertNotNull(rpt);
        Assert.assertEquals(site.getDisplayName(), rpt.site().getDisplayName());
        Assert.assertEquals(2, rpt.observations().size());
        RolloverObservation ro = (RolloverObservation) rpt.observations().toArray()[0];
        Assert.assertEquals("ra=8 dec=12 (J2000)", ro.target().getCoordinates().toString());
    }

    @Test
    public void canSaveRolloverSet(){
        Site site = Mockito.mock(Site.class);
        Mockito.when(site.getDisplayName()).thenReturn("North");

        IRolloverService rolloverService = new RolloverMockService();

        String[] obsIds = new String[]{"My Observation ID"};

        RolloverSet mySet = rolloverService.createRolloverSet(site, "My Rollovers", obsIds, null);
        Assert.assertEquals(1, mySet.getObservations().size());
    }

    @Test
    public void canGetDataForDisplay(){
        IRolloverService rolloverService = new RolloverMockService();

        Set<RolloverSet> rolloverSets = rolloverService.rolloverSetsFor("North");
        Assert.assertEquals(2, rolloverSets.size());
    }

    @Test
    public void canGetSingleRolloverSet(){
        IRolloverService rolloverService = new RolloverMockService();

        RolloverSet set = rolloverService.getRolloverSet(3L);
        Assert.assertNotNull(set);
    }

}
*/