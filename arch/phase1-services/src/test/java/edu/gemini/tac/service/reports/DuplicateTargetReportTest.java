package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.TimeUnit;
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
 * Duplicate target reports tests.
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class DuplicateTargetReportTest extends FastHibernateFixture.Basic {
    private static final Logger LOGGER = Logger.getLogger(DuplicateTargetReportTest.class.getName());

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    @Transactional
    public void oneProposalHasNoDuplicates() {
        loadProposal();
        Report report = new DuplicateTargetsReport();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(0, result.size());  // no duplicates
    }

    @Test
    @Transactional
    public void twoIdenticalProposalsHaveDuplicates() {
        loadProposal();
        loadProposal();
        Report report = new DuplicateTargetsReport();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        // two identical proposals with 1 science targets will result in 1 duplicates
        // represented by two beans (one for each proposal), hence a total of 2 rows is expected
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(2, result.size());
    }

    @Test
    @Transactional
    public void threeIdenticalProposalsHaveDuplicates() {
        loadProposal();
        loadProposal();
        loadProposal();
        Report report = new DuplicateTargetsReport();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        // three identical proposals with 1 science targets will result in 1 duplicate
        // represented by three beans (one for each proposal), hence a total of 3 rows is expected
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(3, result.size());
    }

    @Test
    @Transactional
    public void detectsAllDuplicates() {
        loadProposalsWithDuplicates();
        Report report = new DuplicateTargetsReport();
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        // we expect 0 errors and 5 duplicates
        Assert.assertFalse(report.hasErrors());
        Assert.assertEquals(5, result.size());
    }

    private void loadProposal() {
        new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                create(sessionFactory.getCurrentSession());
    }

    private void loadProposalsWithDuplicates() {
        // duplicates have to be within 30 arcseconds of each other
        new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("AR")).
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR). // duplicate in proposal1 and 2
                addObservation("2:0:0.0", "2:0:0.0", 1, TimeUnit.HR). // duplicate in proposal2
                addObservation("3:0:0.0", "3:0:0.0", 1, TimeUnit.HR). // unique
                                                                      // -- total: 2 targets with duplicates
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("US")).
                addObservation("1:0:1.9", "1:0:29.9", 1, TimeUnit.HR). // duplicate in proposal0 and 2
                addObservation("4:0:0.0", "4:0:0.0", 1, TimeUnit.HR). // unique
                addObservation("5:0:0.0", "5:0:0.0", 1, TimeUnit.HR). // unique
                                                                      // -- total: 1 target with duplicates
                create(sessionFactory.getCurrentSession());
        new QueueProposalBuilder().
                setCommittee(getCommittee(0)).
                setPartner(getPartner("CL")).
                addObservation("0:59:58.5", "0:59:33.0", 1, TimeUnit.HR). // duplicate in proposal0 and 1
                addObservation("2:0:0.5", "1:59:39.3", 1, TimeUnit.HR). // dupliate in proposal0
                addObservation("6:0:0.0", "6:0:0.0", 1, TimeUnit.HR).   // unique
                                                                        // -- total: 2 targets with duplicats
                create(sessionFactory.getCurrentSession());
                                                                        // == total overall: 5 duplicates

    }

}
