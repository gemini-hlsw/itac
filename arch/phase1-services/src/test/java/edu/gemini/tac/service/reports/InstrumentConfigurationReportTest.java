package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.Instrument;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.service.IReportService;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class InstrumentConfigurationReportTest extends FastHibernateFixture.Basic {
    private static final Logger LOGGER = Logger.getLogger(InstrumentConfigurationReportTest.class.getName());

    @Resource(name = "sessionFactory")
    protected SessionFactory sessionFactory;

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    @Transactional
    public void instrumentReport() {
        loadData();

        Report report = new InstrumentConfigurationReport();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());

        Assert.assertFalse(report.hasErrors());
        // 5 GNIRS, 1 GMOS-N and 1 GMOS-S configuration = 7 in total
        Assert.assertEquals(7, result.size());

        int gnirsCnt = 0;
        int gmosSCnt = 0;
        int gmosNCnt = 0;
        for (ReportBean b : result) {
            InstrumentConfigurationReport.InstrumentConfigReportBean bean = (InstrumentConfigurationReport.InstrumentConfigReportBean) b;
            if (bean.getInstrument().contains(Instrument.GNIRS.getDisplayName())) {
                // all different GNIRS configuration have precisely one observation with one hour = 60 mins duration
                Assert.assertEquals(60.0, bean.getAllocTime());
                gnirsCnt++;
            }
            else if (bean.getInstrument().contains(Instrument.GMOS_S.getDisplayName())) {
                Assert.assertEquals(120.0, bean.getAllocTime()); // 2hrs in total or 120mins
                gmosSCnt++;
            }
            else if (bean.getInstrument().contains(Instrument.GMOS_N.getDisplayName())) {
                Assert.assertEquals(180.0, bean.getAllocTime()); // 3hrs in total or 180mins
                gmosNCnt++;
            }
            else {
                // bonk! did not expect this
                Assert.fail();
            }
        }
        Assert.assertEquals(5, gnirsCnt);
        Assert.assertEquals(1, gmosNCnt);
        Assert.assertEquals(1, gmosSCnt);
    }

    public void loadData() {
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner(0));

        // -- create observations for five different GNIRS configurations, 1hr each
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS05).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_O6_PS15).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_LGS).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.GNIRS_IMAGING_H2_PS05_NGS).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());


        // -- create two observations for identical GMOS-S longslit configuration, 2hrs total
        new QueueProposalBuilder().
                setProgramId("AAA").
                setReceipt("AAA", new Date()).
                setBlueprint(ProposalBuilder.GMOS_S_LONGSLIT).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setProgramId("AAA").
                setReceipt("AAA", new Date()).
                setBlueprint(ProposalBuilder.GMOS_S_LONGSLIT).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());

        // -- create three observations for identical GMOS-N imaging configuration, 3hrs total
        new QueueProposalBuilder().
                setProgramId("BBB").
                setReceipt("BBB", new Date()).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setProgramId("BBB").
                setReceipt("BBB", new Date()).
                setBlueprint(ProposalBuilder.GMOS_N_IMAGING).
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
    }
}
