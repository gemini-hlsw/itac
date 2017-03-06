package edu.gemini.tac.service;

import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.rollover.AbstractRolloverObservation;
import edu.gemini.tac.persistence.rollover.IRolloverReport;
import edu.gemini.tac.persistence.rollover.RolloverObservation;
import edu.gemini.tac.persistence.rollover.RolloverSet;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Basic test of RolloverHibernateService
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
//TODO: Extend FastHibernateFixture to incorporate rollover reports and then change this to inherit from FastHibernateFixture
public class RolloverHibernateServiceTest extends HibernateFixture {
    @Resource(name = "rolloverService")
    private IRolloverService rolloverService;

    @Resource(name = "siteService")
    private ISiteService siteService;

    @Before
    public void before() {
        super.teardown();
        super.before();
        List<RolloverObservation> orphans = new ArrayList<RolloverObservation>();
        for(RolloverObservation ro : rolloverObservations){
            if(ro.getParent() == null){
                orphans.add(ro);
            }
        }
        for(RolloverObservation ro: orphans){
            rolloverObservations.remove(ro);
        }
        sessionFactory = getSessionFactory();
        saveSemesterCommitteesProposalsPeople();
        for(int i = 0; i < rolloverObservations.size(); i++){
            Partner p = partners.get(i % partners.size());
            rolloverObservations.get(i).setPartner(p);
        }
        saveOrUpdateAll(sites);
        saveOrUpdateAll(conditions);
        saveOrUpdateAll(rolloverSets);
        saveOrUpdateAll(rolloverReports);
    }

    @Test
    public void canGetDataForDisplay() {
        Set<RolloverSet> rolloverSets = rolloverService.rolloverSetsFor("South");
        Assert.assertEquals(3, rolloverSets.size());
    }

    private Long getRolloverSetId(){
        for(RolloverSet r : rolloverSets){
            if(r.getId() != null){
                return r.getId();
            }
        }
        Assert.fail("Could not find any rollover sets with ids");
        throw new RuntimeException();
    }
    @Test
    public void canGetSingleRolloverSet() {
        Long rolloverId = getRolloverSetId();
        RolloverSet set = rolloverService.getRolloverSet(rolloverId);
        Assert.assertNotNull(set);
    }

    @Test
    public void canGetRolloverReport() throws RemoteException {
        IRolloverReport rpt = rolloverService.report(siteService.findByDisplayName("North"), null);
        Assert.assertNotNull(rpt);
        Assert.assertEquals(10, rpt.observations().size());
    }

    @Test
    public void canCreateRolloverSet() {
        Site site = siteService.findByDisplayName("North");
        String setName = "My RolloverSet";
        String[] observationIds = new String[]{"Ref-0", "Ref-1", "Ref-2"};
        String[] times = new String[]{"8", "8", "8"};

        RolloverSet set = rolloverService.createRolloverSet(site, setName, observationIds, times);
        Assert.assertNotNull(set);
        Assert.assertEquals(3, set.getObservations().size());
        Assert.assertEquals("My RolloverSet", set.getName());
    }

    @Test
    public void canGetMultipleRolloverSets() {
        Set<RolloverSet> sets = rolloverService.rolloverSetsFor("North");
        Assert.assertEquals(3, sets.size());
    }

    @Test
    public void ordersByCreationTimeStampDesc(){
        Site site = siteService.findByDisplayName("North");
        String setName = "Set1";
        String[] observationIds = new String[]{"Ref-0", "Ref-1", "Ref-2"};
        String[] times = new String[]{"8", "8", "8"};

        RolloverSet set = rolloverService.createRolloverSet(site, setName, observationIds, times);

        String setName2 = "Set2";
        String[] times2 = new String[]{"60", "60", "60"};
        RolloverSet set2 = rolloverService.createRolloverSet(site, setName2, observationIds, times2);

        for(AbstractRolloverObservation o : set2.getObservations()){
           Assert.assertEquals(1.0, o.getObservationTime().getValueInHours().doubleValue());
        }
    }

}
