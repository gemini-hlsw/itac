package edu.gemini.tac.persistence.resourceswap;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.Site;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.blueprint.flamingos2.Flamingos2BlueprintImaging;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Tests to support FR-10 "Switch sites".
 * <p/>
 * User: lobrien
 * Date: Dec 1, 2010
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ResourceComplementModelTest extends HibernateFixture {

    @Before
    public void before() {
        super.teardown();
        super.before();
        initAll();
        proposals = getProposals();
    }

    @Test
    public void reversesResourceCategoryType() {
        Proposal p = proposals.get(0);
        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        for (Observation observation : phaseIProposal.getObservations()) {
            BlueprintBase bp = observation.getBlueprint();
            Assert.assertTrue(bp.getSite() == Site.NORTH);
        }
        phaseIProposal.switchTo(Site.SOUTH);
        final List<BlueprintBase> blueprints = phaseIProposal.getBlueprints();
        for (BlueprintBase b : blueprints) {
            Assert.assertEquals(Site.SOUTH, b.getSite());
        }
    }

    @Test
    public void resourceCanReverse() {
        Proposal p = proposals.get(0);
        final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
        for (Observation observation : phaseIProposal.getObservations()) {
            BlueprintBase bp = observation.getBlueprint();
            Assert.assertTrue(bp.getSite() == Site.NORTH);
        }
        phaseIProposal.switchTo(Site.SOUTH);
        final List<BlueprintBase> blueprints = phaseIProposal.getBlueprints();
        for (BlueprintBase b : blueprints) {
            assertTrue(b instanceof Flamingos2BlueprintImaging);
        }
    }
}
