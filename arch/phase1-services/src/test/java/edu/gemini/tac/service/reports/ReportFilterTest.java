package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.Observation;
import edu.gemini.tac.persistence.queues.ScienceBand;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Simple tests for report filtering.
 */
public class
        ReportFilterTest {

    @Test
    public void testSiteFilter() {
        TestReport report = new TestReport();

        // no filter set, anything matches
        Assert.assertTrue(report.matchesSiteFilter(null));
        Assert.assertTrue(report.matchesSiteFilter("any"));
        Assert.assertTrue(report.matchesSiteFilter("theSite"));

        // set filter and check again
        report.setSiteFilter("theSite");
        Assert.assertFalse(report.matchesSiteFilter(null));
        Assert.assertFalse(report.matchesSiteFilter("any"));
        Assert.assertTrue(report.matchesSiteFilter("theSite"));
    }

    @Test
    public void testPartnerFilter() {
        TestReport report = new TestReport();

        // no filter set, anything matches
        Assert.assertTrue(report.matchesPartnerFilter(null));
        Assert.assertTrue(report.matchesPartnerFilter("any"));
        Assert.assertTrue(report.matchesPartnerFilter("thePartner"));

        // set filter and check again
        report.setPartnerFilter("thePartner");
        Assert.assertFalse(report.matchesPartnerFilter(null));
        Assert.assertFalse(report.matchesPartnerFilter("any"));
        Assert.assertTrue(report.matchesPartnerFilter("thePartner"));
    }

    @Test
    public void testLaserOpsOnlyFilter() {
        TestReport report = new TestReport();

        // no filter set, anything matches
        Assert.assertTrue(report.matchesLaserOpsOnlyFilter(false));
        Assert.assertTrue(report.matchesLaserOpsOnlyFilter(true));

        // set filter and check again
        report.setLaserOpsOnlyFilter(true);
        Assert.assertFalse(report.matchesLaserOpsOnlyFilter(false));
        Assert.assertTrue(report.matchesLaserOpsOnlyFilter(true));
    }

    @Test
    public void testInstrumentFilter() {
        TestReport report = new TestReport();

        // no filter set, anything matches
        Assert.assertTrue(report.matchesInstrumentFilter(Instrument.MICHELLE));
        Assert.assertTrue(report.matchesInstrumentFilter(Instrument.GMOS_N));

        // set filter and check again
        report.setInstrumentFilter(Instrument.GMOS_N.name());
        Assert.assertFalse(report.matchesInstrumentFilter(Instrument.MICHELLE));
        Assert.assertTrue(report.matchesInstrumentFilter(Instrument.GMOS_N));
    }

    private class TestReport extends Report {
        TestReport() {
            super("Test");
        }

        @Override
        protected void collectDataForObservation(Proposal proposal, Observation observation) {
        }
    }

}
