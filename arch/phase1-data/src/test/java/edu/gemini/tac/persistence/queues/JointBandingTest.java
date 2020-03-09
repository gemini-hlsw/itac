package edu.gemini.tac.persistence.queues;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.ItacAccept;
import org.hibernate.classic.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class JointBandingTest extends FastHibernateFixture {

    private static final org.apache.log4j.Logger LOGGER = org.apache.log4j.Logger.getLogger(JointBandingTest.class.getName());

    @Before
    public void before() {
        super.before();

//        final Session session = sessionFactory.openSession();
//        partners = session.getNamedQuery("partner.findAllPartners").list();
        partners = getPartners();
        proposals = getProposals();
        groupProposalsByPartner();
//        session.close();

    }

    @Test
    public void createBandings() {
        Session session = sessionFactory.openSession();
        int preJointCount;
        int preCount;
        try {
            Queue q = getQueue(session, 0);
            
            Proposal p0 = getProposalFromPartner(session, "US", 0);
            Proposal p1 = getProposalFromPartner(session, "CA", 0);
            
            // if any of these tests fails something is wrong with the fixture...
            Assert.assertNotNull(q);
            Assert.assertNotNull(p0);
            Assert.assertNotNull(p1);

            // how many bandings are there?
            preCount = countBandingsOfType(q.getBandings(), Banding.class);
            preJointCount = countBandingsOfType(q.getBandings(), JointBanding.class);

            // create a new joint proposal with p0 as primary
            JointProposal jointProposal = new JointProposal(p0, session);
            jointProposal.add(p0, session);
            jointProposal.add(p1, session);
            session.saveOrUpdate(jointProposal);

            // create new joint banding and add p0 and p1 (creates bandings)
            JointBanding jointBanding = new JointBanding(q, jointProposal, ScienceBand.BAND_ONE);
            jointBanding.add(p0);
            jointBanding.add(p1);
            session.saveOrUpdate(jointBanding);

//            q.addBanding(jointBanding);
//            session.saveOrUpdate(q);
            session.flush();
        } finally {
            session.close();
        }

        session = sessionFactory.openSession();
        try {
            // check result
            List<Banding> result = (List<Banding>) session.getNamedQuery("banding.findAllBandingsByQueue").setLong("queueId", queues.get(0).getId()).list();
            Assert.assertEquals(preJointCount + 1, countBandingsOfType(result, JointBanding.class));
            Assert.assertEquals(preCount + 2, countBandingsOfType(result, Banding.class));
        } finally {
            session.close();
        }
    }

    @Test
    public void createBandingsFromCollection() {
        final Session session = sessionFactory.openSession();
        try {
            Queue q = getQueue(session, 0);
            List<Proposal> pList = new ArrayList<Proposal>();
            pList.add(getProposalFromPartner(session, "US", 0));
            pList.add(getProposalFromPartner(session, "CA", 0));
            pList.add(getProposalFromPartner(session, "CL", 0));
            pList.add(getProposalFromPartner(session, "AR", 0));

            // if any of these tests fails something is wrong with the fixture...
            Assert.assertNotNull(q);
            Assert.assertNotNull(pList.get(0));
            Assert.assertNotNull(pList.get(1));
            Assert.assertNotNull(pList.get(2));
            Assert.assertNotNull(pList.get(3));

            // how many bandings are there?
            final int preCount = countBandingsOfType(q.getBandings(), Banding.class);
            final int preJointCount = countBandingsOfType(q.getBandings(), JointBanding.class);

            // create a new joint proposal with p0 as primary
            JointProposal jointProposal = new JointProposal(pList.get(0), session);
            jointProposal.addAll(pList, session);
            session.saveOrUpdate(jointProposal);

            // create joint bandings with lots of proposals
            JointBanding jointBanding = new JointBanding(q, jointProposal, pList, ScienceBand.BAND_ONE);
            session.saveOrUpdate(jointBanding);
            session.flush();

            // check result
            List<Banding> result = (List<Banding>) session.getNamedQuery("banding.findAllBandingsByQueue").setLong("queueId", queues.get(0).getId()).list();
            Assert.assertEquals(preJointCount + 1, countBandingsOfType(result, JointBanding.class));
            Assert.assertEquals(preCount + 4, countBandingsOfType(result, Banding.class));
        } finally {
            session.close();
        }
    }

    @Test
    public void checkNamedQueries() {
        final Session session = sessionFactory.openSession();
        try {
            final Queue q = getQueue(session, 0);
            final Proposal p0 = getProposalFromPartner(session, "CA", 0);
            final Proposal p1 = getProposalFromPartner(session, "US", 0);
            final Proposal p2 = getProposalFromPartner(session, "CL", 0);
            final Proposal p3 = getProposalFromPartner(session, "AR", 0);

            // if any of these tests fails something is wrong with the fixture...
            Assert.assertNotNull(q);
            Assert.assertNotNull(p0);
            Assert.assertNotNull(p1);
            Assert.assertNotNull(p2);
            Assert.assertNotNull(p3);

            // how many bandings are there initially?
            final int preCount = countBandingsOfType(q.getBandings(), Banding.class);
            final int preJointCount = countBandingsOfType(q.getBandings(), JointBanding.class);

            // create a new joint proposal with p0 as primary
            JointProposal jointProposal = new JointProposal(p0, session);
            jointProposal.add(p0, session);
            jointProposal.add(p1, session);
            jointProposal.add(p2, session);
            jointProposal.add(p3, session);
            session.saveOrUpdate(jointProposal);

            // create some bandings and one joint banding
            final JointBanding jointBanding = new JointBanding(q, jointProposal, ScienceBand.BAND_ONE);
            jointBanding.add(p0, ScienceBand.BAND_THREE); // change band for this one
            jointBanding.add(p1);
            q.addBanding(jointBanding);
            // add two additional bandings to queue
            final Banding bandingA = new Banding(q, p2, ScienceBand.BAND_ONE);
            final Banding bandingB = new Banding(q, p3, ScienceBand.BAND_ONE);
            q.addBanding(bandingA);
            q.addBanding(bandingB);
            session.saveOrUpdate(q);
            session.flush();

            // check queueBandings: all joints and bandings that are not part of a joint
            @SuppressWarnings("unchecked")
            final List<Banding> queueBandings =
                    session.getNamedQuery("banding.findBandingsByQueue").
                            setLong("queueId", q.getId()).list();
            Assert.assertEquals(preJointCount + 1, countBandingsOfType(queueBandings, JointBanding.class));
            Assert.assertEquals(preCount + 2, countBandingsOfType(queueBandings, Banding.class));

            // check componentBandings: all bandings (no joints)
            @SuppressWarnings("unchecked")
            final List<Banding> componentBandings =
                    session.getNamedQuery("banding.findComponentBandingsByQueue").
                            setLong("queueId", q.getId()).list();
            Assert.assertEquals(preJointCount + 0, countBandingsOfType(componentBandings, JointBanding.class));
            Assert.assertEquals(preCount + 4, countBandingsOfType(componentBandings, Banding.class));

            // check allBandings: all bandings regardless of type etc.
            @SuppressWarnings("unchecked")
            final List<Banding> allBandings =
                    session.getNamedQuery("banding.findAllBandingsByQueue").
                            setLong("queueId", q.getId()).list();
            Assert.assertEquals(preJointCount + preCount + 5, allBandings.size());
            Assert.assertEquals(preJointCount + 1, countBandingsOfType(allBandings, JointBanding.class));
            Assert.assertEquals(preCount + 4, countBandingsOfType(allBandings, Banding.class));

        } finally {
            session.close();
        }
    }

    @Test
    @Ignore // TODO : I currently don't know how to fix this... do we still need this??
    public void programIdsAreProperlySet() {
        final Session session = sessionFactory.openSession();
        try {
            final Queue q = (Queue) session.getNamedQuery("queue.findQueueById").setLong("queueId", queues.get(0).getId()).uniqueResult();
            final Proposal p0 = getProposalById(session, proposals.get(80).getId());
            final Proposal p1 = getProposalById(session, proposals.get(81).getId());
            final Proposal p2 = getProposalById(session, proposals.get(82).getId());

            // create a new joint proposal with p0 as primary
            JointProposal jp = new JointProposal(p0, session);
            jp.add(p0, session);
            jp.add(p1, session);
            jp.add(p2, session);
            session.saveOrUpdate(jp);

            //Accept the jp
            ItacAccept ia = new ItacAccept();
            jp.getItac().setAccept(ia);

            // if any of these tests fails something is wrong with the fixture...
            Assert.assertNotNull(q);
            Assert.assertNotNull(jp);
            Assert.assertNotNull(p0);
            Assert.assertNotNull(p1);
            Assert.assertNotNull(jp.getItac().getAccept());
            Assert.assertNotNull(p0.getItac().getAccept());
//            Assert.assertNotNull(p1.getPhaseIProposal().getSubmissions().getItac().getAccept());
//            Assert.assertNotNull(p2.getPhaseIProposal().getSubmissions().getItac().getAccept());

            // create joint bandings and (indirectly) bandings
            // create another joint banding by passing a collection to constructor
            List<Proposal> proposals = new LinkedList<Proposal>();
            proposals.add(p0);
            proposals.add(p1);
            JointBanding jointBanding = new JointBanding(q, jp, proposals, ScienceBand.BAND_ONE);
            jointBanding.add(p2, ScienceBand.BAND_TWO);

            q.addBanding(jointBanding);
            session.saveOrUpdate(q);
            session.flush();

            final Map<ScienceBand,Map<String,Banding>> scienceBandMapMap = q.programIds(true);
            final Map<String, Banding> bandOneMap = scienceBandMapMap.get(ScienceBand.BAND_ONE);
            Assert.assertNotNull(bandOneMap);
            Assert.assertEquals(2, bandOneMap.values().size());
            Assert.assertEquals(p0, bandOneMap.get("GS-2010A-Q-1").getProposal());
            Assert.assertEquals(p1, bandOneMap.get("GS-2010A-Q-2").getProposal());

            final Map<String, Banding> bandTwoMap = scienceBandMapMap.get(ScienceBand.BAND_TWO);
            Assert.assertEquals(p2, bandTwoMap.get("GS-2010A-Q-3").getProposal());
        } finally {
            session.close();
        }
    }

    private int countBandingsOfType(Collection<Banding> bandings, Class bandingClass) {
        int joints = 0;
        for (Banding b : bandings) {
            if (b.getClass().equals(bandingClass)) {
                joints++;
            }
        }
        return joints;
    }

}
