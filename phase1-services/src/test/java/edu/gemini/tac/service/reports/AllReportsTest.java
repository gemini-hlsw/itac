package edu.gemini.tac.service.reports;

import edu.gemini.tac.persistence.fixtures.FastHibernateFixture;
import edu.gemini.tac.service.IReportService;
import junit.framework.Assert;
import org.apache.commons.lang.Validate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.io.InputStream;

/**
 * Runs all reports against the fixture with all proposals loaded.
 * Since we don't know the contents of the proposals we can not really do any testing but it's still possible
 * to detect errors by just running the code.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/services-applicationContext.xml"})
public class AllReportsTest extends FastHibernateFixture.WithQueuesLoadOnce {

    @Resource(name = "reportService")
    IReportService reportService;

    @Test
    public void canRunInstrumentConfigurationReport() {
        Report report = new InstrumentConfigurationReport();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunDuplicateTargetsReport() {
        Report report = new DuplicateTargetsReport();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservationTimeReportByBand() {
        Report report = new ObservationTimeReport.ByBand();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservationTimeReportByInstrument() {
        Report report = new ObservationTimeReport.ByInstrument();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservationTimeReportByPartner() {
        Report report = new ObservationTimeReport.ByPartner();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservingConditionsReportForAllBands() {
        Report report = new ObservingConditionsReport.ForAllBands();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservingConditionsReportForBand1() {
        Report report = new ObservingConditionsReport.ForBand1();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservingConditionsReportForBand2() {
        Report report = new ObservingConditionsReport.ForBand2();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservingConditionsReportForBand3() {
        Report report = new ObservingConditionsReport.ForBand3();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunObservingConditionsReportForBand4() {
        Report report = new ObservingConditionsReport.ForBand4();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunRaDistributionReportByBand() {
        Report report = new RaDistributionReport.ByBand();
        reportService.collectReportDataForQueue(report, getCommittee(0).getId(), getQueue(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunRaDistributionReportByInstrument() {
        Report report = new RaDistributionReport.ByInstrument();
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }

    @Test
    public void canRunNiciRestrictedTargetsReport() {
        InputStream restrictedTargets = getClass().getResourceAsStream("/edu/gemini/tac/service/reports/niciRestrictedTargetsList.txt");
        Assert.assertNotNull(restrictedTargets);
        Report report = new NiciRestrictedTargetsReport(restrictedTargets);
        reportService.collectReportDataForCommittee(report, getCommittee(0).getId());
        Assert.assertFalse(report.hasErrors());
    }
}
