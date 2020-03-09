package edu.gemini.tac.service.reports;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.TimeAmount;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.persistence.queues.ScienceBand;
import edu.gemini.tac.service.IReportService;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class ObservationTimeReportTest extends FastHibernateFixture.Basic {
    private static final Logger LOGGER = Logger.getLogger(ObservationTimeReportTest.class.getName());

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    @Transactional
    public void canCreateFromCommittee() {
        createPropsalsAndQueue();
        Report report = new ObservationTimeReport.ByInstrument();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(10, result.size());
        
        int countForUS = 0;
        int countForCL = 0;
        int countForAR = 0;
        int countForGMOSN = 0;
        int countForGNIRS = 0;
        for (ReportBean b : result) {
            ObservationTimeReport.ObservationTimeReportBean bean = (ObservationTimeReport.ObservationTimeReportBean) b;
            
            // -- book keeping for instruments
            if (bean.getInstrument().contains(Instrument.GMOS_N.getDisplayName()))      countForGMOSN++;
            else if (bean.getInstrument().contains(Instrument.GNIRS.getDisplayName()))  countForGNIRS++;
            else Assert.fail("unexpected instrument");

            // -- book keeping for countries
            if (bean.getPartner().contains(getPartner("US").getAbbreviation()))         countForUS++;
            else if (bean.getPartner().contains(getPartner("CL").getAbbreviation()))    countForCL++;
            else if (bean.getPartner().contains(getPartner("AR").getAbbreviation()))    countForAR++;
            else Assert.fail("unexpected partner");
        }
        
        Assert.assertEquals(4, countForGMOSN);
        Assert.assertEquals(6, countForGNIRS);
        Assert.assertEquals(4, countForUS);
        Assert.assertEquals(2, countForAR);
        Assert.assertEquals(4, countForCL);
    }

    @Test
    @Transactional
    public void canCreateFromQueue() {
        Queue queue = createPropsalsAndQueue();
        Report report = new ObservationTimeReport.ByBand();
        List<ReportBean> result = reportService.collectReportDataForQueue(report, getCommittee(0).getId(), queue.getId());
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(10, result.size());

        int countForBand1 = 0;
        int countForBand2 = 0;
        int countForBand3 = 0;
        for (ReportBean b : result) {
            ObservationTimeReport.ObservationTimeReportBean bean = (ObservationTimeReport.ObservationTimeReportBean) b;

            // -- book keeping for bands
            if (bean.getBand().contains("One"))      countForBand1++;
            else if (bean.getBand().contains("Two")) countForBand2++;
            else if (bean.getBand().contains("Three")) countForBand3++;
            else Assert.fail("unexpected band");
        }

        Assert.assertEquals(4, countForBand1);
        Assert.assertEquals(2, countForBand2);
        Assert.assertEquals(4, countForBand3);

    }

    private Queue createPropsalsAndQueue() {
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner("US"));
        Proposal p0 = new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setAccept(new TimeAmount(2, TimeUnit.HR), new TimeAmount(2, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());
        Proposal p1 = new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS05).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setAccept(new TimeAmount(2, TimeUnit.HR), new TimeAmount(2, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());
        Proposal p2 = new QueueProposalBuilder().
                setPartner(getPartner("AR")).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_NGS).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setAccept(new TimeAmount(2, TimeUnit.HR), new TimeAmount(2, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());
        Proposal p3 = new QueueProposalBuilder().
                setPartner(getPartner("CL")).
                setBand(Band.BAND_3).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS15).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_NGS).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                setAccept(new TimeAmount(2, TimeUnit.HR), new TimeAmount(4, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());
        
        Queue q0 = new QueueBuilder(getCommittee(0)).
                addBanding(p0, ScienceBand.BAND_ONE, new TimeAmount(2.0d, TimeUnit.HR)).
                addBanding(p1, ScienceBand.BAND_ONE, new TimeAmount(2.0d, TimeUnit.HR)).
                addBanding(p2, ScienceBand.BAND_TWO, new TimeAmount(2.0d, TimeUnit.HR)).
                addBanding(p3, ScienceBand.BAND_THREE, new TimeAmount(4.0d, TimeUnit.HR)).
                create(sessionFactory.getCurrentSession());
        
        return q0;
    }

}
