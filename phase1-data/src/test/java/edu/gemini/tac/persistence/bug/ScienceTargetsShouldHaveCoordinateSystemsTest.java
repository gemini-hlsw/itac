package edu.gemini.tac.persistence.bug;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.HibernateFixture;
import edu.gemini.tac.persistence.phase1.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Set;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/data-applicationContext.xml"})
public class ScienceTargetsShouldHaveCoordinateSystemsTest extends HibernateFixture {
    private static final Logger LOGGER = Logger.getLogger(ScienceTargetsShouldHaveCoordinateSystemsTest.class.getName());

    @Before
    public void before() {
        super.teardown();
        super.before();
        proposals = getProposals();
    }

    @Test
    public void testCoordinateSystemsPresent() {
        LOGGER.log(Level.DEBUG, "testCoordinateSystemsPresent()");
        for (Proposal p : proposals) {
            final Set<Observation> observationSet = p.getPhaseIProposal().getObservations();
            for (Observation o : observationSet) {
                testCoordinateSystemsPresent(o.getTarget());
                testCoordinateSystemsPresent(o.getGuideStars());
            }
        }
    }

    private void testCoordinateSystemsPresent(Target t) {
        Coordinates coordinates = null;
        if (t instanceof SiderealTarget){
            coordinates = ((SiderealTarget) t).getCoordinates();
            Assert.assertNotNull("Could not find coordinates for target " + t, coordinates);
        } else if (t instanceof  NonsiderealTarget) {
            //Nonsidereals may have many ephemeris elements, but they must have at least 1
            final NonsiderealTarget nonsiderealTarget = (NonsiderealTarget) t;
            final List<EphemerisElement> ephemeris = nonsiderealTarget.getEphemeris();
            Assert.assertNotNull(ephemeris);
            Assert.assertTrue(ephemeris.size() > 0);
            coordinates = ephemeris.get(0).getCoordinates();
            Assert.assertNotNull("Could not find coordinates for target " + t, coordinates);
        } else if (t instanceof TooTarget) {
            // too targets don't have coordinates by design
            Assert.assertNotNull(t.getName());
        } else {
            throw new IllegalArgumentException("unknown target type");
        }
    }

    private void testCoordinateSystemsPresent(Set<GuideStar> gs) {
        for (GuideStar g : gs) {
            testCoordinateSystemsPresent(g.getTarget());
        }
    }

}
