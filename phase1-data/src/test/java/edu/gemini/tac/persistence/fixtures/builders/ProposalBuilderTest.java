package edu.gemini.tac.persistence.fixtures.builders;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.model.p1.mutable.GnirsFilter;
import edu.gemini.model.p1.mutable.GnirsPixelScale;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.altair.AltairConfiguration;
import edu.gemini.tac.persistence.phase1.sitequality.CloudCover;
import edu.gemini.tac.persistence.phase1.sitequality.ImageQuality;
import edu.gemini.tac.persistence.phase1.sitequality.SkyBackground;
import edu.gemini.tac.persistence.phase1.sitequality.WaterVapor;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * Test proposal creation using one of the proposal builders.
 * Those builders are the tool of choice for creating test data for complext test szenarios like queue creation
 * and reporting.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ProposalBuilderTest extends FastHibernateFixture {

    @Resource(name = "sessionFactory")
    protected SessionFactory sessionFactory;

    @Test
    @Transactional
    public void canCreateMinimalQueueProposal() {
        new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        sessionFactory.getCurrentSession().flush();   // force errors!
    }

    @Test
    @Transactional
    public void canCreateMultipleQueueProposals() {
        for (int i = 0; i < 10; i++) {
            new QueueProposalBuilder().
                    setCommittee(getCommittee(0)).
                    setPartner(getPartner("US")).
                    addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                    create(sessionFactory.getCurrentSession());
            sessionFactory.getCurrentSession().flush();   // force errors!
        }
    }

    @Test
    @Transactional
    public void canCreateMinimalClassicalProposal() {
        new ClassicalProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        sessionFactory.getCurrentSession().flush();   // force errors!
    }

    @Test
    @Transactional
    public void canCreateMinimalExchangeProposal() {
        new ExchangeProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                setBlueprint(ProposalBuilder.SUBARU).
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        sessionFactory.getCurrentSession().flush();   // force errors!
    }

    @Test
    @Transactional
    public void canCreateQueueProposalWithDefaults() {
        // use the setDefault* methods to set defaults that should be shared between all builders
        // NOTE: committee and partner are the only values that HAVE to be set and for which no default is given
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner("US"));
        // for the setters the values used below are used as defaults; these values don't have to be set
        // if the values below are useful or don't matter
        ProposalBuilder.setDefaultBand(Band.BAND_1_2);
        ProposalBuilder.setDefaultBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05);
        ProposalBuilder.setDefaultCondition(ProposalBuilder.BEST_CONDITIONS);

        // now we create a queue proposal with two obserations using the defaults from above
        Proposal p = new QueueProposalBuilder().
                addObservation("Vega", "00:00:00.000", "00:00:00.000", 1, TimeUnit.HR).
                addObservation("Noma", "10:00:00.333", "20:00:00.333", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        
        sessionFactory.getCurrentSession().flush();   // force errors!
        Assert.assertNotNull(p.getId());
    }

    @Test
    @Transactional
    public void canCreateComplexQueueProposal() {
        // create additional blueprint, condition and target
        BlueprintBase   gnirs       = ProposalBuilder.createGnirsImaging(GnirsFilter.ORDER_3, GnirsPixelScale.PS_005, AltairConfiguration.NONE);
        Condition       condition   = ProposalBuilder.createCondition(CloudCover.CC_50, ImageQuality.IQ_70, SkyBackground.SB_100, WaterVapor.WV_100);
        Target          target      = ProposalBuilder.createSiderealTarget("Star0", "10:00:00.000", "00:00:00.000");

        // create a new queue proposal
        Proposal p = new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                setBand(Band.BAND_1_2).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS).
                    setCondition(condition).
                        addObservation(target, new TimeAmount(1, TimeUnit.HR)).
                        addObservation("Star1", "15:00:00.000", "00:00:00.000", 2, TimeUnit.HR).
                        addObservation("Star2", "20:00:00.000", "00:00:00.000", 2, TimeUnit.HR).
                        addObservation(Band.BAND_3, gnirs, condition, target, new TimeAmount(1, TimeUnit.HR)).
                    setCondition(ProposalBuilder.BEST_CONDITIONS).
                        addObservation("Vega", "13:13:13.0", "14:14:14.0", 2, TimeUnit.HR).
                        addObservation("Nona", "14:13:13.0", "14:14:14.0", 1, TimeUnit.NIGHT).
                        addObservation("Hama", "15:13:13.0", "14:14:14.0", 5, TimeUnit.HR).
                setBand(Band.BAND_3).
                setBlueprint(gnirs).
                    setCondition(ProposalBuilder.WORST_CONDITIONS).
                        addObservation("Vega", "13:13:13.0", "14:14:14.0", 2, TimeUnit.HR).
                        addObservation("Nona", "14:13:13.0", "14:14:14.0", 1, TimeUnit.NIGHT).
                        addObservation("Hama", "15:13:13.0", "14:14:14.0", 5, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());

        sessionFactory.getCurrentSession().flush();   // force errors!
        Assert.assertNotNull(p.getId());

        // Check that blueprints, conditions and targets are properly re-used / shared between observations

        // two blueprints: gnirs and GNIRS_IMAGING
        Assert.assertEquals(2, p.getPhaseIProposal().getBlueprints().size());
        // three conditions: condition, BEST_CONDITIONS, WORST_CONDITIONS
        Assert.assertEquals(3, p.getPhaseIProposal().getConditions().size());
        // six targets: Star1, Star2, Star3, Vega, Nona, Hama
        Assert.assertEquals(6, p.getPhaseIProposal().getTargets().size());
        // ten observations
        Assert.assertEquals(10, p.getPhaseIProposal().getObservations().size());

    }

}
