package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.queues.PartnerSequence;
import edu.gemini.tac.persistence.queues.Queue;
import edu.gemini.tac.psconversion.Partners;
import edu.gemini.tac.qengine.api.config.CsvToPartnerSequenceParser;
import edu.gemini.tac.qengine.api.config.CustomPartnerSequence;
import edu.gemini.tac.qengine.api.config.ProportionalPartnerSequence;
import edu.gemini.tac.qengine.ctx.Site;
import edu.gemini.tac.service.ICommitteeService;
import edu.gemini.tac.service.IPartnerService;
import edu.gemini.tac.service.configuration.IPartnerSequenceService;
import edu.gemini.tac.service.reports.Report;
import net.sf.jasperreports.engine.JRException;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Controller("committees")
public class CommitteesController extends ReportsController {
    private static final Logger LOGGER = Logger.getLogger(CommitteesController.class.getName());

    @Resource(name = "partnerService")
	protected IPartnerService partnerService;

    @Resource(name = "partnerSequenceService")
    protected IPartnerSequenceService partnerSequenceService;

    public CommitteesController() {
        super();
    }

    public static Link[] getSubheaderLinks(final Long topLevelResourceId) {
        return new Link[]{
                new Link(UrlFor.Controller.QUEUES, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.MEMBERS, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)).setIcon("user-icon.png"),
                new Link(UrlFor.Controller.ROLLOVERS, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.REPORTS, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.SHUTDOWN_CONFIGURATION, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.PARTNER_SEQUENCE_CONFIGURATION, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId)),
                new Link(UrlFor.Controller.LOG, UrlFor.Controller.COMMITTEES, String.valueOf(topLevelResourceId))
        };
    }

    @RequestMapping(value = "/committees", method = RequestMethod.GET)
    public ModelAndView index() {
        LOGGER.debug("index");

        final ModelAndView modelAndView = new ModelAndView();

        addResourceInformation(modelAndView.getModelMap());

        Person userAsPerson = getUser();
        modelAndView.addObject("user", userAsPerson);

        final List<Committee> committees = peopleService.getAllCommitteesFor(userAsPerson);
        final Map<Committee, List<Proposal>> committeeToPartnerProposalsMap = new HashMap<Committee, List<Proposal>>();
        for (Committee c : committees) {
            committeeToPartnerProposalsMap.put(c, c.getProposalsForPartner(userAsPerson.getPartner()));
        }
        modelAndView.addObject("committees", committees);
        modelAndView.addObject("committeeToPartnerProposalsMap", committeeToPartnerProposalsMap);
        modelAndView.setViewName("committees/index");

        return modelAndView;
    }


    @RequestMapping(value = "/committees/{committeeId}", method = RequestMethod.GET)
    public ModelAndView show(@PathVariable final Long committeeId) {
        final ModelAndView modelAndView = new ModelAndView();

        Person userAsPerson = getUser();
        modelAndView.addObject("user", userAsPerson);

        final Committee committee = addCommitteeToModel(committeeService, committeeId, modelAndView);
        final List<Queue> allQueuesForCommittee = committeeService.getAllQueuesForCommittee(committeeId);
        final List<Queue> finalizedQueues = new ArrayList<Queue>(2);

        for (Queue q : allQueuesForCommittee) {
            if (q.getFinalized())
                finalizedQueues.add(q);
        }

        if (allQueuesForCommittee.size() > 0) {
            modelAndView.addObject("lastGeneratedQueue", allQueuesForCommittee.get(0));
            modelAndView.addObject("finalizedQueues", finalizedQueues);
        }

        addSubHeaders(modelAndView.getModelMap(), getSubheaderLinks(committeeId));
        addTopLevelController(modelAndView.getModelMap());
        addController(modelAndView.getModelMap());
        modelAndView.setViewName("committees/show");

        return modelAndView;
    }

    protected static Committee addCommitteeToModel(final ICommitteeService committeeService,
                                                   final Long committeeId, final ModelAndView modelAndView) {
        final Committee committee = committeeService.getCommittee(committeeId);

        modelAndView.addObject("committee", committee);

        return committee;
    }

    @RequestMapping(value = "/committees/{committeeId}/reports", method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId) {
        LOGGER.log(Level.DEBUG, "index");
        ModelAndView modelAndView = getReportModelAndView(committeeId);
        modelAndView.addObject("user", getUser());
        return modelAndView;
    }

    /**
     * Creates report data and returns a model and view object for committee specific reports.
     *
     * @param committeeId        the committeeId for which the report has to be created
     * @param reportName         the name of the report (must match the name of the jrxml template)
     * @param renderType         output format of report (csv, html, xls or pdf)
     * @param httpServletRequest the servlet request
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/reports/{reportName}.{renderType}", method = RequestMethod.GET)
    public ModelAndView createCommitteeReport(
            @PathVariable final Long committeeId,
            @PathVariable final String reportName,
            @PathVariable final String renderType,
            @RequestParam(required = false) final String site,
            @RequestParam(required = false) final String partner,
            @RequestParam(required = false) final String instrument,
            @RequestParam(required = false) final boolean laserOpsOnly,
            @RequestParam(required = false) final boolean ignoreErrors,
            final HttpServletRequest httpServletRequest,
            final HttpServletResponse httpServletResponse) throws IOException, JRException {

//        get report by name, collect report data for queue and render it
        Report report = getReport(reportName, site, partner, instrument, laserOpsOnly, httpServletRequest);
        reportService.collectReportDataForCommittee(report, committeeId);
        if (report.isEmpty()) {
            ModelAndView modelAndView = getReportModelAndView(committeeId);
            modelAndView.addObject("reportIsEmpty", true);
            return modelAndView;
        } else if (report.hasErrors() && !ignoreErrors) {
            ModelAndView modelAndView = getReportModelAndView(committeeId);
            modelAndView.addObject("reportUrl", getRetryReportUrl(httpServletRequest));
            modelAndView.addObject("errors", report.getErrors());
            return modelAndView;
        } else {
            return renderReport(report, renderType, httpServletRequest, httpServletResponse);
        }
    }

    private ModelAndView getReportModelAndView(Long committeeId) {
        final ModelAndView modelAndView = new ModelAndView();
        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));
        modelAndView.setViewName("committees/reports/index");
        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/partner_sequence", method = RequestMethod.GET)
    public ModelAndView partnerSequenceGet(@PathVariable final Long committeeId) {
        LOGGER.log(Level.DEBUG, "partnerSequenceGet");


        ModelAndView modelAndView = getReportModelAndView(committeeId);

        PartnerSequence partnerSequence = committeeService.getPartnerSequenceOrNull(committeeId);
        String csv = null;
        String partnerSequenceName = "ProportionalPartnerSequence";
        if(partnerSequence == null){
            edu.gemini.tac.qengine.api.config.PartnerSequence qPartnerSequence = new ProportionalPartnerSequence(Partners.fromDatabase(edu.gemini.tac.persistence.Site.NORTH).toOption().get().values(), Site.north);
            csv = qPartnerSequence.toString();
            LOGGER.log(Level.DEBUG, csv);
        }else{
            csv = partnerSequence.getCsv();
            partnerSequenceName = partnerSequence.getName();
        }
        modelAndView.addObject("partnerSequenceName", partnerSequenceName);
        modelAndView.addObject("partnerSequence", csv);
        modelAndView.addObject("partnerSequenceProportions", proportionsFromCsv(csv));

        modelAndView.addObject("user", getUser());
        modelAndView.setViewName("committees/partner_sequence/partner-sequence");
        return modelAndView;
    }

    private String proportionsFromCsv(String csv){
        final String[] seq = csv.split(",");
        Map<String,Integer> proportions = new HashMap<String, Integer>();
        for(String key : seq){
            if(proportions.get(key) == null){
                proportions.put(key, 0);
            }
            proportions.put(key, proportions.get(key) + 1);
        }
        StringBuffer description = new StringBuffer();
        for(String key : proportions.keySet()){
            description.append(key + ": " + String.format("%.2f",100.0 * proportions.get(key) / seq.length) + "% ");
        }
        return description.toString();
    }

    @RequestMapping(value = "/committees/{committeeId}/partner_sequence", method = RequestMethod.POST)
    public ModelAndView partnerSequencePost(
            @PathVariable final Long committeeId,
            @RequestParam(required = false) final Boolean proportional,
            @RequestParam(required = false) final String csv,
            @RequestParam(required = false) final String repeat,
            @RequestParam(required = false) final String name) {
        LOGGER.log(Level.DEBUG, "sequence (POST)");
        final ModelAndView modelAndView = new ModelAndView();

        Person userAsPerson = getUser();
        modelAndView.addObject("user", userAsPerson);

        final Committee committee = addCommitteeToModel(committeeService, committeeId, modelAndView);
        PartnerSequence ps = null;
        if(proportional != null && proportional){
            committeeService.setPartnerSequenceProportional(committeeId);
        }else{
            Validate.notNull(csv);
            Validate.notNull(repeat);
            Validate.notNull(name);
            ps = new PartnerSequence(committee, name, csv, repeat.equals("repeating"));
            committeeService.setPartnerSequence(committeeId, ps);
        }
        addPartnerSequenceResults(modelAndView, ps, committee);
        modelAndView.setViewName("committees/partner_sequence/partner-sequence-set");
        return modelAndView;
    }

    void addPartnerSequenceResults(ModelAndView mav, PartnerSequence ps, Committee committee){
        scala.collection.immutable.List<edu.gemini.tac.qengine.ctx.Partner> qPartners = queuePartners();
        if(ps == null){
            mav.addObject("psN", proportionalPartnerSequence(qPartners, Site.north));
            mav.addObject("psS", proportionalPartnerSequence(qPartners, Site.south));
        }else{
            mav.addObject("psN", customPartnerSequence(ps.getCsv(), ps.getRepeat(), ps.getName(), qPartners, Site.north));
            mav.addObject("psS", customPartnerSequence(ps.getCsv(), ps.getRepeat(), ps.getName(), qPartners, Site.south));
        }
    }

    private String proportionalPartnerSequence(scala.collection.immutable.List<edu.gemini.tac.qengine.ctx.Partner> qPartners, Site site) {
        ProportionalPartnerSequence ppsN = new ProportionalPartnerSequence(qPartners, site);
        return ppsN.sequence().take(100).toList().mkString(", ");
    }

    private String customPartnerSequence(String csv, boolean repeat, String name, scala.collection.immutable.List<edu.gemini.tac.qengine.ctx.Partner> qPartners, Site site) {
        final String trimmed = csv.replace(" ", "");
        final String expanded = trimmed.replace(",UH,", ",UH/CL,").replace(",CL,",",UH/CL,");

        scala.collection.immutable.List<edu.gemini.tac.qengine.ctx.Partner> seq = CsvToPartnerSequenceParser.parse(expanded, qPartners).right().get();
        scala.Option<edu.gemini.tac.qengine.ctx.Partner> noneInitialPick = scala.Option.apply(null);
        CustomPartnerSequence ppsN;
        if(! repeat){
            scala.Option someSeq = scala.Option.apply(new ProportionalPartnerSequence(qPartners, site));
            ppsN = new CustomPartnerSequence(seq, site, name, someSeq, noneInitialPick);
        }else{
            scala.Option maybeSeq = scala.Option.apply(null);
            ppsN = new CustomPartnerSequence(seq, site, name, maybeSeq, noneInitialPick);
        }
        return ppsN.sequence().take(100).toList().mkString(", ");
    }

    private scala.collection.immutable.List<edu.gemini.tac.qengine.ctx.Partner> queuePartners() {
        List<Partner> psPartners = partnerService.findAllPartners();
        List<edu.gemini.tac.qengine.ctx.Partner> qPartners = new ArrayList<edu.gemini.tac.qengine.ctx.Partner>();
        for(Partner psPartner : psPartners){
            String id = psPartner.getPartnerCountryKey();
            String name = psPartner.getName();
            double percent = psPartner.getPercentageShare();
            Set<Site> sites = new HashSet<Site>();
            if(psPartner.isNorth()){
                sites.add(Site.north);
            }
            if(psPartner.isSouth()){
                sites.add(Site.south);
            }
            scala.collection.immutable.Set<Site> siteSet = scala.collection.JavaConverters.asScalaSetConverter(sites).asScala().toSet();
            edu.gemini.tac.qengine.ctx.Partner qPartner = edu.gemini.tac.qengine.ctx.Partner.apply(id, name, percent * 100.0, siteSet);
            qPartners.add(qPartner);
        }
        return scala.collection.JavaConverters.asScalaBufferConverter(qPartners).asScala().toList();
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.COMMITTEES;
    }
}
