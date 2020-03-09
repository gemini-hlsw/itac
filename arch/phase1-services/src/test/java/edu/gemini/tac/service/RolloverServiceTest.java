//TODO: Re-implement
//package edu.gemini.tac.service;
//
//
//import edu.gemini.tac.persistence.Site;
//import edu.gemini.tac.persistence.fixtures.DatabaseFixture;
//import edu.gemini.tac.persistence.rollover.*;
//import org.apache.log4j.Logger;
//import org.eclipse.jdt.internal.compiler.lookup.InvocationSite;
//import org.hibernate.SessionFactory;
//import org.junit.Assert;
//import org.junit.Ignore;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//
//import javax.annotation.Resource;
//import java.rmi.RemoteException;
//import java.util.HashSet;
//import java.util.Set;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"/services-applicationContext.xml"})
//public class RolloverServiceTest extends DatabaseFixture {
//    private static final Logger LOGGER = Logger.getLogger(RolloverServiceTest.class.getName());
//
//    @Resource(name = "rolloverService")
//    IRolloverService rolloverService;
//
//    @Resource(name = "siteService")
//    ISiteService siteService;
//
//    @Test
//    public void canGetRolloverReport() throws RemoteException {
//        Assert.assertNotNull(rolloverService);
//        Site targetSite = siteService.findByDisplayName("North");
//        IRolloverReport rpt = rolloverService.report(targetSite, null);
//        Assert.assertNotNull(rpt);
//
//        Assert.assertEquals(targetSite.getDisplayName(), rpt.site().getDisplayName());
//        Set<IRolloverObservation> observations = rpt.observations();
//        Assert.assertEquals(3, observations.size());
//    }
//
//
//}