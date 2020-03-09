package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.persistence.fixtures.builders.ProposalBuilder;
import edu.gemini.tac.persistence.fixtures.builders.QueueProposalBuilder;
import edu.gemini.tac.persistence.phase1.TimeUnit;
import edu.gemini.tac.service.IReportService;
import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.InputStream;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/services-applicationContext.xml"})
public class NiciRestrictedTargetsReportTest extends FastHibernateFixture.BasicLoadOnce {
    private static final Logger LOGGER = Logger.getLogger(NiciRestrictedTargetsReportTest.class.getName());

    @Resource(name = "reportService")
    IReportService reportService;
    
    @Before
    public void before() {
        super.before();
        ProposalBuilder.setDefaultCommittee(getCommittee(0));
        ProposalBuilder.setDefaultPartner(getPartner("US"));
    }
    
    @Test
    @Transactional
    public void doesNotGiveFalsePositives() {
        loadNonMatchingProposals();
        List<ReportBean> result = executeReport();
        Assert.assertEquals(0, result.size()); // we expect no matches
    }

    @Test
    @Transactional
    public void detectsSimplePreciseMatch() {
        loadMatchingProposalPreciseMatch();
        List<ReportBean> result = executeReport();
        Assert.assertEquals(1, result.size());  // we expect one match
    }

    @Test
    @Transactional
    public void detectsSimpleUnpreciseMatch() {
        loadMatchingProposalUnpreciseMatch();
        List<ReportBean> result = executeReport();
        Assert.assertEquals(1, result.size()); // we expect one match
    }

    @Test
    @Transactional
    public void detectsAllMatches() {
        loadNonMatchingProposals();
        loadMatchingProposalPreciseMatch();
        loadNonMatchingProposals();
        loadMatchingProposalUnpreciseMatch();
        loadNonMatchingProposals();
        List<ReportBean> result = executeReport();
        Assert.assertEquals(2, result.size()); // we expect two matches
    }

    private List<ReportBean> executeReport() {
        InputStream restrictedTargets = getClass().getResourceAsStream("/edu/gemini/tac/service/reports/niciRestrictedTargetsList.txt");
        Report report = new NiciRestrictedTargetsReport(restrictedTargets);
        List<ReportBean> result = reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        // we don't expect any errors
        Assert.assertFalse(report.hasErrors());
        // return result for further checks
        return result;
    }

    private void loadNonMatchingProposals() {
        // use any instrument different than NICI
        ProposalBuilder.setDefaultBlueprint(ProposalBuilder.GMOS_N_IMAGING);
        new QueueProposalBuilder().
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                addObservation("0:2:0.0", "0:5:0.0", 1, TimeUnit.HR).
                addObservation("0:4:0.0", "0:6:0.0", 1, TimeUnit.HR).
                create(getSessionFactory().getCurrentSession());
        new QueueProposalBuilder().
                addObservation("1:0:0.0", "1:0:0.0", 1, TimeUnit.HR).
                create(getSessionFactory().getCurrentSession());
        new QueueProposalBuilder().
                addObservation("1:0:0.0", "2:0:0.0", 1, TimeUnit.HR).
                create(getSessionFactory().getCurrentSession());
        new QueueProposalBuilder().
                addObservation("5:0:0.0", "4:0:0.0", 1, TimeUnit.HR).
                create(getSessionFactory().getCurrentSession());
        new QueueProposalBuilder().
                // matching coordinates but not NICI as instrument -> must not result in a match!
                addObservation("10:0:0.0", "10:0:0.0", 1, TimeUnit.HR).  // precise restricted pos
                addObservation("10:0:3.9", "9:59:01.0", 1, TimeUnit.HR). // within one arcmin
                create(getSessionFactory().getCurrentSession());

        // use NICI but for observations that do not match
        ProposalBuilder.setDefaultBlueprint(ProposalBuilder.NICI_STANDARD);
        new QueueProposalBuilder().
                addObservation("0:0:0.0", "0:0:0.0", 1, TimeUnit.HR).
                addObservation("0:2:0.0", "0:5:0.0", 1, TimeUnit.HR).
                addObservation("0:4:0.0", "0:6:0.0", 1, TimeUnit.HR).
                create(getSessionFactory().getCurrentSession());
    }

    private void loadMatchingProposalPreciseMatch() {
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.NICI_STANDARD).
                addObservation("3:0:0.0", "3:0:0.0", 1, TimeUnit.HR). // no match
                addObservation("10:0:0.0", "10:0:0.0", 1, TimeUnit.HR). // precise match
                addObservation("5:4:0.0", "5:6:0.0", 1, TimeUnit.HR). // no match
                create(getSessionFactory().getCurrentSession());
    }

    private void loadMatchingProposalUnpreciseMatch() {
        new QueueProposalBuilder().
                setBlueprint(ProposalBuilder.NICI_STANDARD).
                addObservation("3:0:0.0", "3:0:0.0", 1, TimeUnit.HR).  // no match
                addObservation("10:0:3.9", "9:59:01.0", 1, TimeUnit.HR). // match within one arcmin (1 sec = 15 arcsec)
                addObservation("5:4:0.0", "5:6:0.0", 1, TimeUnit.HR).  // no match
                create(getSessionFactory().getCurrentSession());
    }
}
