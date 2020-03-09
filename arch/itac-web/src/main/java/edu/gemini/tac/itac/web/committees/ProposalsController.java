package edu.gemini.tac.itac.web.committees;

import edu.gemini.model.p1.mutable.Band;
import edu.gemini.model.p1.mutable.ExchangePartner;
import edu.gemini.model.p1.mutable.InvestigatorStatus;
import edu.gemini.model.p1.mutable.Keyword;
import edu.gemini.tac.exchange.ExchangeStatistics;
import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.SwallowingHttpServletResponse;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.*;
import edu.gemini.tac.persistence.phase1.blueprint.BlueprintBase;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.phase1.submission.*;
import edu.gemini.tac.persistence.security.AuthorityRole;
import edu.gemini.tac.service.AbstractLogService;
import edu.gemini.tac.service.IPartnerService;
import edu.gemini.tac.service.IProposalService;
import edu.gemini.tac.service.IQueueService;
import edu.gemini.tac.util.ProposalUnwrapper;
import edu.gemini.tac.util.ProposalImporter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.*;
import java.util.*;

@Controller(value = "ProposalsController")
public class ProposalsController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(ProposalsController.class.getName());

    // pdf storage and access locations
    @Value("${itac.pdf.folder}")
    private File pdfFolder;

    @Resource(name = "logService")
    private AbstractLogService logService;

    @Resource(name = "partnerService")
    private IPartnerService partnerService;

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Resource(name = "zipArchiveView")
    private ZipArchiveView zav;

    @Resource(name = "queueService")
    private IQueueService queueService;

    public void setProposalService(IProposalService proposalService) {
        this.proposalService = proposalService;
    }


    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.PROPOSALS;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

    /**
     * Exports proposal as xml.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/export-xml", method = RequestMethod.GET)
    public ModelAndView export(@PathVariable final Long committeeId, @PathVariable final Long proposalId, HttpServletResponse response) throws Exception {
        LOGGER.log(Level.ALL, "export-xml");

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final byte[] bytes = proposalService.getProposalAsXml(proposal);
        final StringBuilder fileName = new StringBuilder();
        fileName.append(proposal.getPartner().getPartnerCountryKey());
        fileName.append('-');
        fileName.append(proposal.getId());
        fileName.append(".xml");

        response.setContentType("application/xml");
        response.setContentLength(bytes.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(bytes);

        return null;
    }

    /**
     * Gets the pdf that belongs to a proposal from the file system.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/pdf", method = RequestMethod.GET)
    public ModelAndView getPdf(@PathVariable final Long committeeId, @PathVariable final Long proposalId, HttpServletResponse response) throws Exception {
        LOGGER.log(Level.ALL, "pdf");

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final byte[] bytes = proposalService.getProposalPdf(proposal, pdfFolder);
        final StringBuilder fileName1 = new StringBuilder();
        fileName1.append(proposal.getPartner().getPartnerCountryKey());
        fileName1.append('-');
        fileName1.append(proposal.getId());
        fileName1.append(".pdf");
        final String fileName = fileName1.toString();

        response.setContentType("application/pdf");
        response.setContentLength(bytes.length);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.getOutputStream().write(bytes);

        return null;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/check")
    public ModelAndView check(@PathVariable final Long committeeId) {
        LOGGER.debug("ProposalsController.check()");

        final Committee committee = committeeService.getCommittee(committeeId);

        final String logMessage = getUser().getName() + " performed a proposal check.";
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
        }}, committee));

        List<Proposal> committeeProposals = committeeService.checkAllProposalsForCommittee(committeeId);
        List<Proposal> bypassFilteredProposals = new ArrayList<Proposal>();

        for (Proposal p: committeeProposals) {
            if (!p.getChecksBypassed())
                bypassFilteredProposals.add(p);
        }

        Map<ProposalIssueCategory, List<Proposal>> map = proposalService.proposalsInProblemCategories(bypassFilteredProposals);
        ModelAndView mav = getAccordionModelAndView(committeeId, bypassFilteredProposals);
        mav.addObject("proposalIssuesByCategory", map);
        mav.addObject("ProposalIssueCategoryValues", ProposalIssueCategory.values());
        mav.setViewName("committees/proposals/check");
        return mav;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/check-components")
    public ModelAndView checkComponents(@PathVariable final Long committeeId) {
        LOGGER.debug("ProposalsController.checkComponents()");

        final Committee committee = committeeService.getCommittee(committeeId);
        final String logMessage = getUser().getName() + " performed a proposal check.";
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
        }}, committee));

        List<Proposal> committeeProposals = committeeService.checkAllComponentsForCommittee(committeeId);
        return getAccordionModelAndView(committeeId, committeeProposals);
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/exchange", method = RequestMethod.GET)
    public ModelAndView exchange(@PathVariable final Long committeeId) {
        LOGGER.log(Level.DEBUG, "exchange");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);

        final Committee committee = committeeService.getCommitteeForExchangeAnalysis(committeeId);
        LOGGER.info(committee.getProposals().size() + " proposals located");
        final Map<String, List<Proposal>> exchangeTimeProposals = committee.getExchangeTimeProposals();

        final List<Proposal> fromKeckProposals = exchangeTimeProposals.get("from-" + ExchangePartner.KECK.name());
        final List<Proposal> fromSubaruProposals = exchangeTimeProposals.get("from-" + ExchangePartner.SUBARU.name());
        final List<Proposal> forKeckProposals = exchangeTimeProposals.get("for-" + ExchangePartner.KECK.name());
        final List<Proposal> forSubaruProposals = exchangeTimeProposals.get("for-" + ExchangePartner.SUBARU.name());
        modelAndView.addObject("fromKeckProposals", fromKeckProposals);
        modelAndView.addObject("fromSubaruProposals", fromSubaruProposals);
        modelAndView.addObject("forKeckProposals", forKeckProposals);
        modelAndView.addObject("forSubaruProposals", forSubaruProposals);

        final ExchangeStatistics exchangeStatistics = new ExchangeStatistics(partnerService.findAllPartners(), committee.getProposals());

        modelAndView.addObject("exchangeStatistics", exchangeStatistics);
        modelAndView.setViewName("committees/proposals/exchange");

        return modelAndView;
    }

    /* Bulk operations */

    @RequestMapping(value = "/committees/{committeeId}/proposals/export-html", method = RequestMethod.GET)
    public ModelAndView indexForBulkExport(@PathVariable final Long committeeId) {
        LOGGER.log(Level.DEBUG, "indexForBulkExport(" + committeeId + ")");
        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        List<Proposal> proposals = committeeService.getAllProposalsForCommittee(committeeId);
        modelAndView.addObject("proposals", proposals);

        modelAndView.setViewName("committees/proposals/export/bulk-export");
        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/export/export-html-do", method = RequestMethod.GET)
    public ModelAndView bulkExport(@PathVariable final Long committeeId, @RequestParam final List<Long> ids, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.log(Level.DEBUG, "bulkExport() for " + ids.size());

        ModelAndView mav = boilerPlateModelAndView(committeeId);
        Map<String, String> pages = exportToHtml(committeeId, ids, request, response);
        mav.setViewName("committees/proposals/export/export-html-do");

        ModelAndView wrapper = new ModelAndView(zav);
        wrapper.addObject("pages", pages);
        return wrapper;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/import", method = RequestMethod.GET)
    public ModelAndView importGet(@PathVariable final Long committeeId) {
        LOGGER.debug("importGet");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);

        modelAndView.setViewName("committees/proposals/import");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/import", method = RequestMethod.POST)
    public ModelAndView importPost(
            @PathVariable final Long committeeId,
            @RequestParam final MultipartFile file,
            @RequestParam(required = false) final boolean ignoreErrors,
            @RequestParam(required = false) final boolean replaceProposals
    ) throws IOException {
        LOGGER.debug("importPost");

        final ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", getUser());
        Committee committee = CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        String message = "Import failed, no changes were made to database.";
        ProposalImporter importer = new ProposalImporter(replaceProposals, pdfFolder);

        if (!ignoreErrors) {
            // import whole file in one big transaction, if a problem occurs ALL documents are ignored
            try {
                proposalService.importDocuments(importer, file.getOriginalFilename(), file.getInputStream(), committeeId);
                message = "Import was successful, all documents were imported. " + importer.getResults().size() + " proposals were successfully imported.";
            } catch (Exception e) {
                LOGGER.error("import failed: " + e.getMessage(), e);
            }
        } else {
            // import each document in a separate transaction, ignore document if a problem occurs
            ProposalUnwrapper unwrapper = new ProposalUnwrapper(file.getInputStream(), file.getOriginalFilename());
            List<ProposalUnwrapper.Entry> importFiles = unwrapper.getEntries();
            message = "Import was successful, all documents were imported.";
            for (ProposalUnwrapper.Entry importFile : importFiles) {
                try {
                    proposalService.importSingleDocument(importer, importFile, committeeId);
                } catch (Exception e) {
                    message = String.format("Import was only partially successful. %s documents were ignored during import.", importer.getResults().getFailedCount());
                    LOGGER.error("ignoring import failure: " + e.getMessage(), e);
                }
            }
            message += " " + (importer.getResults().getSuccessfulCount()) + " proposals were successfully imported.";
            unwrapper.cleanup();
        }

        final ProposalImporter.ResultCollection results = importer.getResults();
        final List<ProposalImporter.Result> successfulResults = results.getSuccessful();
        logService.logSuccessfullyImportedProposals(successfulResults);

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        modelAndView.addObject("message", message);
        modelAndView.addObject("successfulCount", importer.getResults().getSuccessfulCount());
        modelAndView.addObject("failedCount", importer.getResults().getFailedCount());
        modelAndView.addObject("importerResults", importer.getResults());
        modelAndView.setViewName("committees/proposals/import-done");

        final String logMessage = getUser().getName() + " imported proposal(s) from file " + file.getOriginalFilename();
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_BULK_IMPORT);
        }}, committee));


        return modelAndView;
    }

    /**
     * Supports the requirement to display all proposals associated with a given committee.  Allows
     * site users to filter proposals and narrow down to those that they need to examine.
     *
     * @param committeeId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals")
    public ModelAndView list(@PathVariable final Long committeeId) {
        LOGGER.debug("ProposalsController.list()");
        long start = System.currentTimeMillis();
        List<Proposal> committeeProposals = committeeService.getAllProposalsForCommittee(committeeId);
        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        modelAndView.addObject("partners", partnerService.findAllPartners());
        ProposalAccordionHelper.populateModelForProposalAccordionNoServiceNoValidation(modelAndView, committeeProposals);
        // forward to import page in case there are no proposals
        if (committeeProposals.size() == 0) {
            modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/import");
        } else {
            modelAndView.setViewName("committees/proposals/index");
        }

        long duration = System.currentTimeMillis() - start;
        LOGGER.debug("Duration: " + duration + " ms");
        return modelAndView;
    }

    /**
     * Bulk edit operation page for proposals.
     *
     * @param committeeId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals/bulk-edit")
    public ModelAndView bulkEdit(@PathVariable final Long committeeId) {
        LOGGER.debug("bulk-edit");
        long start = System.currentTimeMillis();
        List<Proposal> committeeProposals = committeeService.getAllProposalsForCommittee(committeeId);
        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        modelAndView.addObject("partners", partnerService.findAllPartners());
        modelAndView.addObject("proposals", committeeProposals);
        modelAndView.setViewName("committees/proposals/bulk-edit");

        long duration = System.currentTimeMillis() - start;
        LOGGER.debug("Duration: " + duration + " ms");
        return modelAndView;
    }


    @RequestMapping(value = "/committees/{committeeId}/proposals/partner/{partnerAbbreviation}")
    public ModelAndView listPartner(@PathVariable final Long committeeId, @PathVariable final String partnerAbbreviation) {
        // Not ready for prime time, does not pull in joint proposals
        LOGGER.debug("ProposalsController.listPartner()");
        List<Proposal> committeeProposals = committeeService.getAllProposalsForCommitteePartner(committeeId, partnerAbbreviation);
        validateNoLIE(committeeProposals);
        return getAccordionModelAndView(committeeId, committeeProposals);
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/search/{searchString}", method = RequestMethod.GET)
    public void proposalSearch(@PathVariable final Long committeeId, @PathVariable final String searchString, HttpServletResponse response) throws IOException {
        LOGGER.log(Level.DEBUG, "proposalSearch?" + searchString);
        StringBuilder sb = new StringBuilder("[ ");
        Set<Proposal> proposals = proposalService.search(committeeId, searchString);
        for (Proposal p : proposals) {
            sb.append("  {\n");
            LOGGER.log(Level.DEBUG, "Found: " + p.toString() + " " + p.getPartnerAbbreviation());

            sb.append("\n\t \"id\" : \"" + p.getEntityId() + " \", ");
            sb.append("\n\t \"pi\" : \"" + p.getPhaseIProposal().getInvestigators().getPi().getLastName() + "\", ");
            sb.append("\n\t \"title\" : \"" + p.getPhaseIProposal().getTitle() + "\", ");
            sb.append("\n\t \"naturalId\" : \"" + p.getPartnerAbbreviation() + "|" + p.getPartnerReferenceNumber() + "\", ");
            sb.append("\n\t \"pk\" : \"" + p.getPhaseIProposal().getSubmissionsKey() + "\" ");
            sb.append("\n }, ");
        }
        if (sb.lastIndexOf(",") > -1) {
            sb.deleteCharAt(sb.lastIndexOf(","));
        }
        sb.append("\n]");
        String body = sb.toString();

        LOGGER.log(Level.DEBUG, body);
//        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
//        wrapper.setContentType("application/json");
//        wrapper.setHeader("Content-length", "" + body.getBytes().length);
//        wrapper.getWriter().print(body);

        response.setContentType("application/json");
        response.setContentLength(body.getBytes().length);
        response.getOutputStream().write(body.getBytes());

    }

    /**
     * Displays additional information related to the proposal and allows editing of some of that
     * information.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}", method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.ALL, "index");

        final Person person = getUser();
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", person);
        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getResourceLinks(String.valueOf(committeeId), String.valueOf(proposalId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);

        if (proposal == null) {
            modelAndView.addObject("initialSearch", proposalId);
            modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/log");
            return modelAndView;
        }

        modelAndView.addAllObjects(modelMapForProposalShow(proposal));
        Set<ProposalIssue> issues = proposal.getIssues();
        if(issues == null){
            issues = new HashSet<ProposalIssue>();
        }
        modelAndView.addObject("proposal", proposal);
        modelAndView.addObject("issues", issues);
        modelAndView.addObject("pdf", "file:///tmp/itac-pdfs/"+committeeId+"/"+proposalId);

        modelAndView.setViewName("committees/proposals/show");

        return modelAndView;
    }

    /* Proposal instance methods */

    /**
     * Handles edited data related to the proposal
     * information.
     *
     * @param committeeId
     * @param proposalId
     * @return
     */
    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}", method = RequestMethod.POST)
    public void edit(
            @PathVariable final Long committeeId,
            @PathVariable final Long proposalId,
            @RequestParam(value = "class", required = true) final String targetType,
            @RequestParam(value = "field", required = false) final String field,
            @RequestParam(value = "value", required = false) final String value,
            @RequestParam(value = "naturalId", required = false) final String naturalId,
            HttpServletResponse response
    ) {
        doProposalEdit(committeeId, proposalId, targetType, field, value, naturalId, response);
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/check")
    public ModelAndView check(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.debug("ProposalsController.check()");

        committeeService.checkProposalForCommittee(committeeId, proposalId);

        // NOTE: a simple redirect will not do because the browser will not reload the page and
        // the new/changed issues will not be shown to the user; using RedirectView will cause
        // the redirect to be done server side and all new information is rendered properly
        ModelAndView mav = new ModelAndView();
        mav.setView(new RedirectView("/tac/committees/" + committeeId + "/proposals/" + proposalId));
        return mav;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/delete", method = RequestMethod.GET)
    public ModelAndView deleteGet(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.ALL, "deleteProposal(" + proposalId + ") GET");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);

        Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        modelAndView.addObject("proposal", proposal);
        if (proposal.isJoint() || proposal.isJointComponent()) {
            modelAndView.setViewName("committees/proposals/delete-joint-warn");
        } else {
            modelAndView.setViewName("committees/proposals/delete-proposal");
        }

        return modelAndView;
    }

    //Yeah, yeah, it should be a DELETE to the proposalId, not to a sub-URL
    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/delete", method = RequestMethod.POST)
    @PreAuthorize("isInRole('ROLE_ADMIN') or isInRole('ROLE_SECRETARY')")
    public ModelAndView deletePost(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.ALL, "deleteProposal(" + proposalId + ") POST");

        ModelAndView mav = boilerPlateModelAndView(committeeId);
        final Committee committee = CommitteesController.addCommitteeToModel(committeeService, committeeId, mav);

        proposalService.deleteProposal(proposalId, pdfFolder);

        final String logMessage = getUser().getName() + " deleted proposal " + proposalId;
        logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_DELETED);
        }}, committee));

        mav.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/");
        return mav;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/dot", method = RequestMethod.GET)
    public void dot
            (
                    @PathVariable final Long committeeId,
                    @PathVariable final Long proposalId,
                    HttpServletResponse response) throws IOException {
        Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        StringBuilder sb = new StringBuilder();
        sb.append("digraph IProposalElement{\n");
        sb.append("graph [rankdir=LR, size=\"9.5,11\", page=\"11.000000,8.500000\", margin=\"0.25,0.25\", center=true];\nnode [label=\"\\N\"];\n");

        try {
            sb.append(proposal.toDot());
        } catch (Exception x) {
            LOGGER.log(Level.WARN, x);
        }
        sb.append("}");
        String dot = sb.toString();
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response);
        wrapper.setHeader("Content-disposition", "attachment; filename=" + proposalId + ".dot");
        wrapper.setContentType("text/x-graphviz; charset=UTF-8");
        wrapper.setHeader("Content-length", "" + dot.getBytes().length);
        wrapper.getWriter().print(dot);
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/duplicate", method = RequestMethod.GET)
    public ModelAndView duplicateConfirm(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.ALL, "duplicateProposal(" + proposalId + ") POST");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);

        modelAndView.addObject("original", proposalService.getProposal(committeeId, proposalId));

        modelAndView.setViewName("committees/proposals/duplicate-proposal");


        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/duplicate", method = RequestMethod.POST)
    public ModelAndView duplicateProposal(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.ALL, "duplicateProposal(" + proposalId + ") POST");

        final ModelAndView modelAndView = new ModelAndView();
        final Committee committee = CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        Proposal original = proposalService.getProposal(committeeId, proposalId);
        Proposal duplicate = proposalService.duplicate(original, pdfFolder);

        final String logMessage = getUser().getName() + " duplicated proposal <a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\">" + original.getPartnerReferenceNumber() + "</a> creating <a href=\"/tac/committees/" + committeeId + "/proposals/" + duplicate.getId() + "\">" + duplicate.getPartnerReferenceNumber() + "</a>";
        logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_DUPLICATED);
        }}, committee, original));
        final String duplicateLogMessage = getUser().getName() + " duplicated from proposal <a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\">" + original.getPartnerReferenceNumber() + "</a> creating <a href=\"/tac/committees/" + committeeId + "/proposals/" + duplicate.getId() + "\">" + duplicate.getPartnerReferenceNumber() + "</a>";
        logService.addLogEntry(logService.createNewForProposalOnly(duplicateLogMessage, new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_DUPLICATED);
        }}, duplicate));


        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/" + duplicate.getId());

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/switch-site", method = RequestMethod.GET)
    public ModelAndView switchSiteConfirm(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, "switchSiteConfirm(" + committeeId + ", " + proposalId + ")");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        modelAndView.addObject("proposal", proposalService.getProposal(committeeId, proposalId));
        modelAndView.setViewName("committees/proposals/switch-site");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/switch-site", method = RequestMethod.POST)
    public ModelAndView switchSiteExecute(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, "switchSiteExecute()");

        final Committee committee = committeeService.getCommittee(committeeId);

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final Site site = proposal.getSite();
        proposalService.switchSites(proposal);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/" + proposalId);

        final String logMessage = getUser().getName() + " switched sites from " + site.getDisplayName() + " for proposal <a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\">" + proposal.getPartnerReferenceNumber() + "</a>";
        logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
        }}, committee, proposal));


        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/toggle-mode", method = RequestMethod.GET)
    public ModelAndView toggleModeConfirm(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, "toggleModeConfirm(" + committeeId + ", " + proposalId + ")");

        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        modelAndView.addObject("proposal", proposalService.getProposal(committeeId, proposalId));
        modelAndView.setViewName("committees/proposals/toggle-mode");

        return modelAndView;
    }

    @RequestMapping(value = "/committees/{committeeId}/proposals/{proposalId}/toggle-mode", method = RequestMethod.POST)
    public ModelAndView toggleModeExecute(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.DEBUG, "toggleModeExecute(" + committeeId + ", " + proposalId + ")");

        final Committee committee = committeeService.getCommittee(committeeId);

        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        final String originalObservingMode = proposal.getPhaseIProposal().getObservingMode();
        proposalService.setClassical(proposal, !proposal.isClassical());
        final Proposal toggledProposal = proposalService.getProposal(committeeId, proposalId);
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/" + proposalId);

        final String logMessage = getUser().getName() + " toggled observing mode for proposal <a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\">" + proposal.getPartnerReferenceNumber() + "</a> from " + originalObservingMode + " to " + toggledProposal.getPhaseIProposal().getObservingMode();
            logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
            }}, committee, proposal));

        return modelAndView;
    }

    private ModelAndView boilerPlateModelAndView(Long committeeId) {
        ModelAndView modelAndView = new ModelAndView();
        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        final Person person = getUser();
        modelAndView.addObject("user", person);
        modelAndView.addObject("adminOrSecretary", person.isAdmin() || person.isInRole("ROLE_SECRETARY"));

        return modelAndView;
    }

    private Map<String, String> exportToHtml(final Long committeeId, final List<Long> proposalIds, final HttpServletRequest request, final HttpServletResponse response) {
        Map<String, String> pages = new HashMap<String, String>();
        for (Long proposalId : proposalIds) {
            final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
            final String htmlFilename = proposal.getPartner().getAbbreviation() + "/" + proposal.getPartnerReferenceNumber().replace(".", "_").replace("/", "_").replace("\\", "_").replace("\"", "_");
            final String page = exportToHtml(committeeId, proposalId, request, response);

            zav.addToArchive(htmlFilename, page);

            try {
                final byte[] pdfBytes = proposalService.getProposalPdf(proposal, pdfFolder);

                if ((pdfBytes != null) && pdfBytes.length > 0)
                    zav.addToArchive(htmlFilename + ".pdf", pdfBytes);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof FileNotFoundException) {
                    LOGGER.error("Problem getting PDF file for proposal " + proposal.getId(), e);
                } else {
                    throw e;
                }
            }
            LOGGER.log(Level.DEBUG, page);
        }

        return pages;
    }

    private String exportToHtml(final Long committeeId, final Long proposalId, final HttpServletRequest request, final HttpServletResponse response) {
        Proposal p = proposalService.getProposal(committeeId, proposalId);
        Map<String, Object> modelMap = modelMapForProposalShow(p);
        for (String key : modelMap.keySet()) {
            request.setAttribute(key, modelMap.get(key));
        }
        request.setAttribute("user", getUser());

        final Writer sout = new StringWriter();
        final String characterEncoding = "UTF8";
        try {
            final StringWriter tacCssWriter = new StringWriter();
            InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("/tac.css");
            IOUtils.copy(resourceAsStream, tacCssWriter, characterEncoding);
            final StringWriter printCssWriter = new StringWriter();
            resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("/blueprint/print.css");
            IOUtils.copy(resourceAsStream, tacCssWriter, characterEncoding);

            request.setAttribute("cssString", tacCssWriter.toString() + "\n" + printCssWriter.toString());
        } catch (IOException x) {
            LOGGER.log(Level.ERROR, x);
        }

        final SwallowingHttpServletResponse myResponseWrapper = new SwallowingHttpServletResponse(response, sout, characterEncoding);
        try {
            final RequestDispatcher rd = request.getRequestDispatcher("/WEB-INF/jsp/committees/proposals/show.jsp");

            rd.include(request, myResponseWrapper);
        } catch (Exception x) {
            LOGGER.log(Level.ERROR, x);
        }
        return sout.toString();
    }

    private ModelAndView getAccordionModelAndView(final Long committeeId, final List<Proposal> committeeProposals) {
        ModelAndView modelAndView = boilerPlateModelAndView(committeeId);
        modelAndView.addObject("partners", partnerService.findAllPartners());
        validateNoLIE(committeeProposals);
        ProposalAccordionHelper.populateModelForProposalAccordion(proposalService, modelAndView, committeeProposals);

        // forward to import page in case there are no proposals
        if (committeeProposals.size() == 0) {
            modelAndView.setViewName("redirect:/tac/committees/" + committeeId + "/proposals/import");
        } else {
            modelAndView.setViewName("committees/proposals/index");
        }

        return modelAndView;
    }

    private Map<String, Object> modelMapForProposalShow(Proposal proposal) {
        Map<String, Object> modelMap = new HashMap<String, Object>();
        //Helper objects to avoid long call chains. Not placed in modelMap
        if (proposal == null) {
            modelMap.put("broadcastMessage", "Could not locate proposal with this identifier -- are you coming from a page with 'stale' data?");
            return modelMap;
        }
        if (proposal.getPhaseIProposal() == null) {
            modelMap.put("broadcastMessage", "No phase I proposal found for ITAC proposal. Please report error to development.");
            return modelMap;
        }
        final PhaseIProposal p1p = proposal.getPhaseIProposal();
        final Investigators investigators = proposal.getPhaseIProposal().getInvestigators();

        Set<Condition> conditionsWithoutDefault = new HashSet<Condition>();
        for (Observation observation : p1p.getObservations()) {
            final Condition condition = observation.getCondition();
            if (condition != null) {
                conditionsWithoutDefault.add(condition);
            }
        }

        final Set<String> instruments = new HashSet<String>();
        for (BlueprintBase bb : proposal.getBlueprints())
            instruments.add(bb.getInstrument().getDisplayName());
        modelMap.put("instruments", instruments.toArray());

        String hostPartnerCountryKey = p1p.getPrimary().getPartner().getPartnerCountryKey();
        // NOTE: gemini reference and time awarded are not available in the context of a committee
        // if we want to display them in the context of a queue we need the banding... currently we don't do that
        // and just display this information always as N/A. If we want to display it pass in the banding and
        // use the getAwardedTime and getGeminiId methods on the banding to get dynamically created information.
        String geminiReference = "Not Available";
        String timeAwarded = "Not Available";
        modelMap.put("geminiReference", geminiReference);
        modelMap.put("timeAwarded", timeAwarded);

        final PrincipalInvestigator pi = investigators.getPi();
        String isThesis = pi.getStatus() == InvestigatorStatus.GRAD_THESIS ? "Yes" : "No";
        modelMap.put("thesis", isThesis);


        populateModelMapWithBand3Data(modelMap, p1p.getObservations(), p1p.getBand3Request());

        modelMap.put("poorWeather", Boolean.toString(proposal.isPw()));

        String title = proposal.getPhaseIProposal().getTitle();
        modelMap.put("title", title);

        String piName = pi.getFirstName() + " " + pi.getLastName();
        modelMap.put("pi", piName);

        String pinst = pi.getInstitutionAddress().getInstitution();
        modelMap.put("pinst", pinst);


        String piStatus = pi.getStatusDisplayString();
        modelMap.put("piStatus", piStatus);

        String piPhone = "";
        for(String phone : pi.getPhoneNumbers()){
            piPhone += phone + ", ";
        }
        if(piPhone.lastIndexOf(',') > -1){
            piPhone = piPhone.substring(0, piPhone.lastIndexOf(','));
        }
        modelMap.put("piPhone", piPhone);
        modelMap.put("piEmail", pi.getEmail());

        List<String> cois = new ArrayList<String>();
        for (CoInvestigator coi : investigators.getCoi()) {
            String coiString = coi.getFirstName() + " " + coi.getLastName() + ": " + coi.getInstitution() + ", <a href=\"mailto:" + coi.getEmail() + "\">" + coi.getEmail() + "</a>";
            cois.add(coiString);
        }
        modelMap.put("cois", cois);

        String abstractAsString = proposal.getPhaseIProposal().getProposalAbstract();
        modelMap.put("abstract", StringEscapeUtils.escapeHtml(abstractAsString).replace("\n", "<br/>"));    // ITAC-424: show linebreaks properly

        /* Version 1 had optimal and impossible date range information included.  That information is no longer encoded
        in the schema.  */

        Set<String> keywords = new HashSet<String>();
        for(Keyword keyword : p1p.getKeywords()){
            keywords.add(keyword.value());
        }
        modelMap.put("keywords", keywords);

        /* Version 1 had allocations and publications included.  That information is no longer encoded in the schema
        and is now externalized to the attached document (which we don't peek into.).  */

        modelMap.put("proposalId", proposal.getId());

        modelMap.put("hrefToAttachment", p1p.getMeta().getAttachment());
        modelMap.put("proposal", proposal);

        return modelMap;
    }

    private void populateModelMapWithBand3Data(final Map<String, Object> modelMap,
                                               final Set<Observation> observations,
                                               final SubmissionRequest band3Request) {
        boolean isBand3Eligible = false;

        for(Observation o : observations){
            final Band band = o.getBand();
            if(band.equals(Band.BAND_3)){
                isBand3Eligible = true;
            }
        }
        modelMap.put("band3", isBand3Eligible);
        if (band3Request != null) {
            modelMap.put("band_3_request", Boolean.TRUE);
            modelMap.put("band_3_minimum_usable_time", band3Request.getMinTime().toPrettyString());
            modelMap.put("band_3_requested_time", band3Request.getTime().toPrettyString());
        }
    }

    private void validateNoLIE(Collection<Proposal> ps) {
        for (Proposal p : ps) {
            validateNoLIE(p);
        }
    }

    private void validateNoLIE(Proposal p) {
        try {
            p.getPhaseIProposal().getSubmissions();
        } catch (Exception x) {
            LOGGER.error("LIE with proposal " + p);
        }
    }

    public Link[] getCollectionLinks(final String topLevelResourceId) {
        ArrayList<Link> links = new ArrayList<Link>();
        Collections.addAll(links, new Link[]{
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "exchange", "Exchange"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "export-html", "Export HTML"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "check", "Check"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "check-components", "Components"),
                new Link(UrlFor.Controller.JOINT_PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "joint", "Joints"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "bulk-edit", "Bulk Edit")
        });

        for (AuthorityRole authority : getUser().getAuthorities()) {
            if (authority.getRolename().equals(AuthorityRole.ROLE_SECRETARY) || authority.getRolename().equals(AuthorityRole.ROLE_ADMIN)) {
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "import", "Import"));
            } else if (authority.getRolename().equals((AuthorityRole.ROLE_COMMITTEEE_MEMBER))) {
//                Not ready for prime time, does not pull in joint proposals
                links.add(new Link(UrlFor.Controller.PROPOSALS,
                            UrlFor.Controller.COMMITTEES,
                            topLevelResourceId,
                            "",
                            getUser().getPartner().getAbbreviation() +" List").
                        setFilter("partner").
                        setFilterResource(getUser().getPartner().getAbbreviation()));
            }
        }

        Link[] objects = links.toArray(new Link[]{});
        return objects;
    }

    public Link[] getResourceLinks(final String committeeId, final String proposalId) {
        ArrayList<Link> links = new ArrayList<Link>();
        Collections.addAll(links, new Link[]{
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "export-xml", "Export XML")
        });
        final Person person = getUser();
        final Set<AuthorityRole> authorities = person.getAuthorities();
        for (AuthorityRole authority : authorities) {
            if (authority.getRolename().equals(AuthorityRole.ROLE_SECRETARY) || authority.getRolename().equals(AuthorityRole.ROLE_ADMIN)) {
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "check", "Check"));
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "switch-site", "Switch Site"));
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "duplicate", "Duplicate"));
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "delete", "Delete"));
                links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "toggle-mode", "Toggle QC"));
            }
        }

        links.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, proposalId, committeeId, "log", "Log"));

        Link[] ls = links.toArray(new Link[]{});
        return ls;
    }

    /* Setter methods for Spring configuration */
    public void setPdfFolder(String pdfFolder) {
        this.pdfFolder = new File(pdfFolder);
        this.pdfFolder.mkdirs();
        // check state of pdf folder on web server startup (this is when Spring will call this setter)
        // I assume it's preferable to not start the web application with an invalid configuration instead of producing errors at some random time.
        Validate.isTrue(this.pdfFolder.exists(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " could not be created! Check configuration.");
        Validate.isTrue(this.pdfFolder.isDirectory(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a directory! Check configuration.");
        Validate.isTrue(this.pdfFolder.canRead(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a readable!");
        Validate.isTrue(this.pdfFolder.canWrite(), "Pdf folder " + this.pdfFolder.getAbsolutePath() + " is not a writable!");
    }
}
