package edu.gemini.tac.persistence.fixtures;

import edu.gemini.tac.exchange.ProposalExporterImpl;
import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.proposal.ClassicalProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.proposal.QueueProposal;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class FixtureTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(FixtureTest.class.getName());

    @Resource
    SessionFactory sessionFactory;

    @Before
    public void before() {
        super.teardown();
        super.before();
        sessionFactory = getSessionFactory();
        proposals = getProposals();
        saveOrUpdateAll(authorityRoles);
        saveSemesterCommitteesProposalsPeople();
         for (Proposal p : proposals) {
            for (Target t : p.getPhaseIProposal().getTargets()) {
                if (t instanceof NonsiderealTarget) {
                    NonsiderealTarget nst = (NonsiderealTarget) t;
                    Assert.assertEquals(p.getPhaseIProposal(), nst.getPhaseIProposal());
                    final List<EphemerisElement> ephemeris = nst.getEphemeris();
                    Assert.assertNotNull(ephemeris);
                    Assert.assertTrue(ephemeris.size() > 0);
                    for(EphemerisElement ee : ephemeris){
                        Assert.assertNotNull(ee.getCoordinates());
                        Assert.assertNotNull(ee.getMagnitude());
                    }
                }
            }
        }

        partners = getPartners();
        groupProposalsByPartner();
    }
    
    @Test
    public void testDataIsValid() {
        for (Proposal p : proposals) {
            p.validate();
            // validate fixture data and see if it can be exported as xml and if that xml is actually valid
            ProposalExporterImpl exporter = new ProposalExporterImpl();
            exporter.getAsXml(p); // this will internally validate against schema
        }
    }

    @Test
    public void testDataIsQuiteComplete() {
        saveOrUpdateAll(partners);
        initJointProposals(0);
        initJointProposals(2);
        initJointProposals(4);
        initJointProposals(6);
        initJointProposals(8);

        for(Proposal p : proposals){
            if(p instanceof  JointProposal){
                JointProposal jp = (JointProposal) p;
                for(Observation o : jp.getPhaseIProposal().getObservations()){
                    Target t = o.getTarget();
                    if(t instanceof NonsiderealTarget){
                        NonsiderealTarget nst = (NonsiderealTarget) t;
                        for(EphemerisElement ee : nst.getEphemeris()){
                            ee.setTarget(t);
                            LOGGER.trace("JointProposal[" + jp.getId() + "]->Observation[" + o.getId() + "]->NonsiderealTarget[" + t.getId() + "]->EphemerisElement[" + ee.getId() + "]->NonsiderealTarget[" + ee.getTarget().getId() + "]");
                        }
                    }
                }
            }
        }

//        saveOrUpdateAll(phaseIProposals);
//        saveOrUpdateAll(guideStars);
//        saveOrUpdateAll(nonSiderealTargets);
//        saveOrUpdateAll(ephemerisElements);
//        saveOrUpdateAll(committees);
        saveOrUpdateAll(proposals);
        String mode = "";
        boolean atLeastOneQueue = false;
        boolean atLeastOneClassical = false;
        boolean atLeastOneJoint = false;
        boolean atLeastOneSidereal = false;
        int validJoints = 0;

        for (Proposal p : proposals) {
            if (p.isClassical()) {
                mode = "Classical";
                atLeastOneClassical = true;
            } else {
                if (p.getPhaseIProposal().isQueue()) {
                    mode = "Queue";
                    atLeastOneQueue = true;
                }
            }
            final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
            final Set<Observation> observations1 = phaseIProposal.getObservations();
            assertTrue("Empty observations list", observations1.size() > 0);
            if(p.isJoint()){
                atLeastOneJoint = true;
                for(Observation o : p.getPhaseIProposal().getObservations()){
                    Target t = o.getTarget();
                    Assert.assertNotNull(t);
                    if(t instanceof NonsiderealTarget){
                        NonsiderealTarget nst = (NonsiderealTarget) t;
                        Assert.assertNotNull(nst.getEphemeris());
                        for(EphemerisElement ee : nst.getEphemeris()){
                            Assert.assertEquals(t.getId(), ee.getTarget().getId());
                        }
                        validJoints++;
                    }
                }
            }
            PhaseIProposal p1p = p.getPhaseIProposal();
            for(Target t : p1p.getTargets()){
                if(t instanceof NonsiderealTarget){
                    final NonsiderealTarget nst = (NonsiderealTarget) t;
                    Assert.assertNotNull(nst.getEphemeris().get(0));
                    atLeastOneSidereal = true;
                    Assert.assertNotNull(t.getTargetId());
                    for(EphemerisElement ee : nst.getEphemeris()){
                        Assert.assertNotNull(ee.getMagnitude());
                    }
                } else if (t instanceof SiderealTarget) {
                    final SiderealTarget st = (SiderealTarget) t;
                    Assert.assertTrue(st.getMagnitudes().size() > 0);
                } else if (t instanceof TooTarget) {
                    final TooTarget tt = (TooTarget) t;
                    Assert.assertNotNull(t.getTargetId());
                } else {
                    throw new IllegalArgumentException("unknown target type");
                }
            }
        }
        Assert.assertTrue(atLeastOneQueue);
        Assert.assertTrue(atLeastOneClassical);
        Assert.assertTrue(atLeastOneJoint);
        Assert.assertEquals(4, validJoints);
        Assert.assertTrue(atLeastOneSidereal);
    }

}
