package edu.gemini.tac.service.reports;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.service.IReportService;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/*
 * Test cases for ra distribution report.
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class RaDistributionReportTest extends FastHibernateFixture.BasicLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(RaDistributionReportTest.class.getName());

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    @Transactional
    public void canCreateByBandForQueue() {
        Queue q = loadQueue();
        Report report = new RaDistributionReport.ByBand();
        List<ReportBean> result = reportService.collectReportDataForQueue(report, getCommittee(0).getId(), q.getId());
        Assert.assertFalse(report.hasErrors());

        int band1cnt = 0;
        int band2cnt = 0;
        for (ReportBean r : result) {
            RaDistributionReport.RaDistributionReportBean bean = (RaDistributionReport.RaDistributionReportBean) r;
            Assert.assertTrue(bean.getRaBin() >= -1 && bean.getRaBin() <= 23);
            if (bean.getBandName().contains("One")) {
                band1cnt++;
            } else if (bean.getBandName().contains("Two")) {
                band2cnt++;
            }
        }
        Assert.assertEquals(2, band1cnt);
        Assert.assertEquals(2, band2cnt);
    }

    @Test
    @Transactional
    @ExpectedException(RuntimeException.class)
    public void canNotCreateByBandForCommittee() {
        // By Band reports can only be collected for queues, not for committees!
        loadQueue();
        Report report = new RaDistributionReport.ByBand();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.fail();
    }

    @Test
    @Transactional
    public void canCreateByInstrumentForCommittee() {
        loadQueue();
        Report report = new RaDistributionReport.ByInstrument();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        checkByInstrumentResult(report);
    }

    @Test
    @Transactional
    public void canCreateByInstrumentForQueue() {
        Queue q = loadQueue();
        Report report = new RaDistributionReport.ByInstrument();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), q.getId());
        checkByInstrumentResult(report);
    }

    private void checkByInstrumentResult(Report report) {
        Assert.assertFalse(report.hasErrors());
        List<ReportBean> result = report.getData();
        int gnirsCnt = 0;
        int gmosCnt = 0;
        for (ReportBean b : result) {
            RaDistributionReport.RaDistributionReportBean bean = (RaDistributionReport.RaDistributionReportBean) b;
            if (bean.getInstrumentName().contains(Instrument.GNIRS.getDisplayName())) {
                gnirsCnt++;
            } else if (bean.getInstrumentName().contains(Instrument.GMOS_N.getDisplayName())) {
                gmosCnt++;
            }
        }
        Assert.assertEquals(2, gnirsCnt);
        Assert.assertEquals(2, gmosCnt);
    }

    private Queue loadQueue() {
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner("US"));
        
        Proposal p0 = new QueueProposalBuilder().
                setBand(Band.BAND_1_2).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS05).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());

        Proposal p1 = new QueueProposalBuilder().
                setBand(Band.BAND_1_2).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS05).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        
        Queue q = new QueueBuilder(getCommittee(0)).
                addBanding(p0, ScienceBand.BAND_ONE).
                addBanding(p1, ScienceBand.BAND_TWO).
                create(sessionFactory.getCurrentSession());
        
        return q;
    }


}
