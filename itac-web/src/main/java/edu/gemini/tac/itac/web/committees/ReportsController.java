package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.service.IReportService;
import edu.gemini.tac.service.reports.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.*;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Controller
public abstract class ReportsController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());

    @Resource(name = "reportService")
    protected IReportService reportService;

    /**
     * Looks up the report for a given report name, creates is and configures it (filters).
     * @param reportName
     * @param site
     * @param partner
     * @param instrument
     * @param laserOpsOnly
     * @param httpServletRequest
     * @return
     */
    protected Report getReport(String reportName, String site, String partner, String instrument, Boolean laserOpsOnly, HttpServletRequest httpServletRequest) {
        Report report;
        if (reportName.equals(DuplicateTargetsReport.NAME)) {
            report = new DuplicateTargetsReport();

        } else if (reportName.equals(NiciRestrictedTargetsReport.NAME)) {
            ServletContext servletContext = httpServletRequest.getSession().getServletContext();
            report =  new NiciRestrictedTargetsReport(servletContext.getResourceAsStream("/WEB-INF/reports/niciRestrictedTargetsList.txt"));

        } else if (reportName.equals(ObservingConditionsReport.ForAllBands.NAME)) {
            report =  new ObservingConditionsReport.ForAllBands();

        } else if (reportName.equals(ObservingConditionsReport.ForBand1.NAME)) {
            report =  new ObservingConditionsReport.ForBand1();

        } else if (reportName.equals(ObservingConditionsReport.ForBand2.NAME)) {
            report =  new ObservingConditionsReport.ForBand2();

        } else if (reportName.equals(ObservingConditionsReport.ForBand3.NAME)) {
            report =  new ObservingConditionsReport.ForBand3();

        } else if (reportName.equals(ObservingConditionsReport.ForBand4.NAME)) {
            report =  new ObservingConditionsReport.ForBand4();

        } else if (reportName.equals(RaDistributionReport.ByBand.NAME)) {
            report =  new RaDistributionReport.ByBand();

        } else if (reportName.equals(RaDistributionReport.ByInstrument.NAME)) {
            report =  new RaDistributionReport.ByInstrument();

        } else if (reportName.equals(InstrumentConfigurationReport.NAME)) {
            report =  new InstrumentConfigurationReport();

        } else if (reportName.equals(ObservationTimeReport.ByBand.NAME)) {
            report =  new ObservationTimeReport.ByBand();

        } else if (reportName.equals(ObservationTimeReport.ByInstrument.NAME)) {
            report =  new ObservationTimeReport.ByInstrument();

        } else if (reportName.equals(ObservationTimeReport.ByPartner.NAME)) {
            report =  new ObservationTimeReport.ByPartner();

        } else if (reportName.equals(ProposalsReport.NAME)) {
            report =  new ProposalsReport();
        } else if (reportName.equals(ProposalSubmissionReport.NAME)){
           report = new ProposalSubmissionReport();
        } else if (reportName.equals(SchedulingConstraintsReport.NAME)){
            report = new SchedulingConstraintsReport();
        } else {
            throw new IllegalArgumentException("unknown report name " + reportName);
        }

        // set information for creation of hyperlinks in report
        StringBuffer url = new StringBuffer();
        url.append("http://");
        url.append(httpServletRequest.getServerName());
        url.append(":");
        url.append(httpServletRequest.getServerPort());
        report.setUrl(url.toString());

        // set filters (will be null if no parameters were passed as part of the url)
        report.setSiteFilter(site);
        report.setPartnerFilter(partner);
        report.setInstrumentFilter(instrument);
        report.setLaserOpsOnlyFilter(laserOpsOnly);

        return report;
    }

    /**
     * Gets the retry url that should be used in case of errors.
     * This is basically the original url plus "ignoreErrors=true" added to the query string. With this additional
     * query parameter errors will be ignored and the report will be created regardless of any errors; this in
     * turn means that all data of proposals with errors will be ignored!
     * @param request
     * @return
     */
    protected String getRetryReportUrl(HttpServletRequest request) {
        StringBuffer sb = new StringBuffer();
        sb.append(request.getRequestURI());
        sb.append("?");
        String query = request.getQueryString();
        if (query != null) {
            sb.append(query);
            sb.append("&");
        }
        sb.append("ignoreErrors=true");
        return sb.toString();
    }

    /**
     * Gets a ModelAndView object that is either one of the different jasper views or a png view.
     * @param report
     * @param renderType
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    protected ModelAndView renderReport(
            final Report report,
            final String renderType,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) {

        if (renderType.equals("png")) {
            // stream png data directly to client
            return renderReportDataAsPng(report, httpServletRequest, httpServletResponse);

        } else {
            // create report and use spring jasper views to render them
            return renderReportUsingJRView(report, renderType, httpServletRequest);
        }
    }

    /**
     * Gets a png image of a report.
     * @param report
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    private ModelAndView renderReportDataAsPng(
            final Report report,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) {

        File reportDir = null;
        File htmlFile = null;
        File imageFile = null;

        try {
            File sysTempDir = new File(System.getProperty("java.io.tmpdir"));
            reportDir = new File(sysTempDir, "itac_report_" + Long.toString(System.nanoTime()));
            LOGGER.log(Level.DEBUG, "rendering report to temporary directory " + reportDir.getAbsolutePath());

            // creating temporary files on disk
            reportDir.mkdir();
            htmlFile = new File(reportDir, report.getName() + ".html");

            // read report file from disk
            // Note: if necessary we could speed this up by not compiling the template everytime again
            ServletContext servletContext = httpServletRequest.getSession().getServletContext();
            JasperDesign design = JRXmlLoader.load(servletContext.getResourceAsStream("/WEB-INF/reports/" + report.getName() + ".jrxml"));
            JasperReport jreport = JasperCompileManager.compileReport(design);
            JRDataSource dataSource = new JRBeanCollectionDataSource(report.getData());

            // fill report with provided data
            Map parameters = new HashMap();
            parameters.put("subtitleString", report.getSubtitleString());
            JasperPrint print = JasperFillManager.fillReport(jreport, parameters, dataSource);

            // configure exporter and render to temporary files
            JRHtmlExporter exporter = new JRHtmlExporter();
            exporter.setParameter(JRExporterParameter.JASPER_PRINT, print);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, new FileOutputStream(htmlFile));
            exporter.setParameter(JRHtmlExporterParameter.HTML_HEADER, "");
            exporter.setParameter(JRHtmlExporterParameter.HTML_FOOTER, "");
            exporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.TRUE);
            exporter.setParameter(JRHtmlExporterParameter.IMAGES_DIR_NAME, reportDir.getAbsolutePath());
            exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, "/images/");
            exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.FALSE);
            exporter.exportReport();

            // read image and return it
            // Note: works only if exactly one image is produced?
            imageFile = new File(reportDir.getAbsolutePath() + "/img_0_0_0");
            FileInputStream fis = new FileInputStream(imageFile);
            byte[] bytes = new byte[(int)imageFile.length()];
            fis.read(bytes, 0, (int)imageFile.length());
            fis.close();

            httpServletResponse.setContentType("image/png");
            httpServletResponse.setContentLength(bytes.length);
            httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + report.getName() + ".png\"");
            httpServletResponse.getOutputStream().write(bytes);

            return null;
        }
        catch (Exception ex) {
            // catch any exception and re-throw as runtime exception so we don't need to
            // bother about marked exceptions in all method signatures
            throw new RuntimeException("rendering of report as png failed: " + ex.getMessage(), ex);
        }
        finally {
            // (are there cases where more than one image is created?)
            // recursive deletion of report directory would probably be the way to go
            if (htmlFile != null) {
                htmlFile.delete();
            }
            if (imageFile != null) {
                imageFile.delete();
            }
            if (reportDir != null) {
                reportDir.delete();
            }
        }
    }

    /**
     * Creates a model and view object based on JasperReport views provided by Spring framework.
     * @param report
     * @param renderType
     * @param httpServletRequest
     * @return
     */
    private ModelAndView renderReportUsingJRView(
            final Report report,
            final String renderType,
            final HttpServletRequest httpServletRequest) {

        ModelAndView modelAndView = new ModelAndView();

        final WebApplicationContext ctx =
                WebApplicationContextUtils.getRequiredWebApplicationContext(
                        httpServletRequest.getSession().getServletContext());

        final Properties headers = new Properties();
        headers.put("Content-Disposition", "inline; filename="+report.getName()+"."+renderType);

        AbstractJasperReportsSingleFormatView jasperView = null;
        if (renderType.equals("csv")) {
            jasperView = new JasperReportsCsvView();
        } else if(renderType.equals("html")){
            jasperView = new JasperReportsHtmlView();
        } else if(renderType.equals("xls")){
            jasperView = new JasperReportsXlsView();
        } else if (renderType.equals("pdf")) {
            jasperView = new JasperReportsPdfView();
        } else {
            throw new IllegalArgumentException("unknown report type " + renderType);
        }

        jasperView.setUrl("/WEB-INF/reports/"+report.getName()+".jrxml");
        jasperView.setApplicationContext(ctx);
        jasperView.setHeaders(headers);
        jasperView.setReportDataKey("reportData");

        String subtitleString = report.getSubtitleString();
        if (report.hasErrors()) {
            subtitleString += " (CONTAINS ERRORS / INCOMPLETE DATA)";
        }

        final Map<String,Object> model = new HashMap<String,Object>();
        model.put("reportData", report.getData());
        model.put("subtitleString", subtitleString);
        modelAndView.setView(jasperView);
        modelAndView.addAllObjects(model);

        return modelAndView;
    }
}
