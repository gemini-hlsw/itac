package edu.gemini.tac.service.reports;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.ConditionsBin;
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

/**
 * Test cases for observing conditions report.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class ObservingConditionsReportTest extends FastHibernateFixture.BasicLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(ObservingConditionsReportTest.class.getName());

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    @Transactional
    public void canCreateForAllBands() {
        Queue q = loadQueue();
        Report report = new ObservingConditionsReport.ForAllBands();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), q.getId());
        checkResult(report, 2.0, 4.0);
    }

    @Test
    @Transactional
    public void canCreateForBandOne() {
        Queue q = loadQueue();
        Report report = new ObservingConditionsReport.ForBand1();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), q.getId());
        checkResult(report, 1.0, 2.0);
    }

    private void checkResult(Report report, double expBestCondHrs, double expWorstCondHrs) {
        Assert.assertFalse(report.hasErrors());
        List<ReportBean> result = report.getData();
        double bestHrs  = 0; // IQ20_CC50_SB50
        double worstHrs = 0; // IQANY_CCANY_SBANY
        for (ReportBean b : result) {
            ObservingConditionsReport.ObservingConditionsReportBean bean = (ObservingConditionsReport.ObservingConditionsReportBean) b;
            if (bean.getConditionName().equals(ConditionsBin.IQ20_CC50_SB50.getName())) {
                bestHrs += bean.getHours();
            } else if (bean.getConditionName().equals(ConditionsBin.IQANY_CCANY_SBANY.getName())) {
                worstHrs += bean.getHours();
            }
        }
        Assert.assertEquals(expBestCondHrs, bestHrs);
        Assert.assertEquals(expWorstCondHrs, worstHrs);
    }

    private Queue loadQueue() {
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner("US"));

        Proposal p0 = new QueueProposalBuilder().
                setBand(Band.BAND_1_2).
                setCondition(ProposalBuilder.BEST_CONDITIONS).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setCondition(ProposalBuilder.WORST_CONDITIONS).
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                setCondition(ProposalBuilder.WORST_CONDITIONS).
                addObservation("3:0:0.0", "3:0:0.0", 1, TimeUnit.HR).
                // NOTE: observation times will be taken into account in ratio to the recommended time!
                setAccept(new TimeAmount(3.0, TimeUnit.HR), new TimeAmount(3.0, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());

        Proposal p1 = new QueueProposalBuilder().
                setBand(Band.BAND_1_2).
                setCondition(ProposalBuilder.BEST_CONDITIONS).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                setCondition(ProposalBuilder.WORST_CONDITIONS).
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                setCondition(ProposalBuilder.WORST_CONDITIONS).
                addObservation("3:0:0.0", "3:0:0.0", 1, TimeUnit.HR).
                setAccept(new TimeAmount(3.0, TimeUnit.HR), new TimeAmount(3.0, TimeUnit.HR), 1).
                create(sessionFactory.getCurrentSession());

        Queue q = new QueueBuilder(getCommittee(0)).
                addBanding(p0, ScienceBand.BAND_ONE, new TimeAmount(3.0d, TimeUnit.HR)).
                addBanding(p1, ScienceBand.BAND_TWO, new TimeAmount(3.0d, TimeUnit.HR)).
                create(sessionFactory.getCurrentSession());

        return q;
    }

}
