package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.phase1.proposal.PhaseIProposal;
import edu.gemini.tac.persistence.security.AuthorityRole;
import edu.gemini.tac.service.AbstractLogService;
import edu.gemini.tac.service.IJointProposalService;
import edu.gemini.tac.service.IMatch;
import edu.gemini.tac.service.IProposalService;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Handle Web Requests having to do with Joint Proposals
 *
 * Author: lobrien
 * Date: 1/25/11
 */

@Controller(value = "jointProposalController")
@RequestMapping("/committees/{committeeId}/proposals/joint")
public class JointProposalController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(JointProposalController.class);

    public Link[] getCollectionLinks(final String topLevelResourceId) {
        ArrayList<Link> links = new ArrayList<Link>();
        Collections.addAll(links, new Link[]{
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "", "List"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "import", "Import"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "exchange", "Exchange"),
                new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "export-html", "Export HTML")
        });
        Link[] objects = links.toArray(new Link[]{});
        return objects;
    }

    @Resource(name = "proposalService")
    private IProposalService proposalService;

    @Resource(name = "jointProposalService")
    private IJointProposalService jointProposalService;

    @Resource(name = "logService")
    private AbstractLogService logService;

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.JOINT_PROPOSALS;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.COMMITTEES;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId,
                              @RequestParam(required = false) final String flash) {
        LOGGER.log(Level.DEBUG, "joint");

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("user", getUser());
        modelAndView.addObject("flash", flash);

        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));

        //Retrieve all proposals in this committee that are joint...
        final List<Proposal> committeeProposals = committeeService.getAllProposalsForCommittee(committeeId);
        modelAndView.addObject("committeeProposals", committeeProposals);

        List<Proposal> joints = proposalService.getProposals(committeeId, new IMatch<Proposal>() {
            public boolean match(Proposal p) {
                return p.getCommittee().getId().equals(committeeId) && p instanceof JointProposal;
            }
        });
        //In fact, the List is a Tuple of Proposal, List<Proposal> (Primary and Secondary proposals in jp)
        Map<Proposal, List<Object>> proposals = new HashMap<Proposal, List<Object>>();
        Map<Proposal, Map<String, String>> sortKeys = new HashMap<Proposal, Map<String, String>>();
        for(Proposal p : joints){
            //This will populate the graph
//            proposalService.populateIMergeableGraph(p);
            p = proposalService.populateProposalForListing(p);
            List<Object> components = new ArrayList<Object>();
            proposals.put(p, components);
            JointProposal jp = (JointProposal) p;
            components.add(0, jp.getPrimaryProposal());
            components.add(1, jp.getSecondaryProposals());

            //Create sort strings (NorthOrSouth, PI, etc.)
            Map<String, String> keyByKeyName = new HashMap<String, String>();
            sortKeys.put(p, keyByKeyName);
            //Partner
            keyByKeyName.put("Partner", p.getPartner().getAbbreviation());
            //North or South
            final PhaseIProposal phaseIProposal = p.getPhaseIProposal();
            final Site site = phaseIProposal.getSite();
            if (site != null) { // Exchange proposals do not possess sites.
                if (site.equals(Site.NORTH))
                    keyByKeyName.put("Location", "North");
                if (site.equals(Site.SOUTH))
                    keyByKeyName.put("Location", "South");
            }

            //Laser Guide Star?
            keyByKeyName.put("LGS", Boolean.toString(p.isLgs()));
            //Poor Weather?
            keyByKeyName.put("PW", Boolean.toString(p.isPw()));
            //Not Band 3?
            keyByKeyName.put("B3", Boolean.toString(p.isBand3()));
            //Classical or Queue
            if (phaseIProposal.isQueue())
                keyByKeyName.put("Mode", "Queue");
            if (phaseIProposal.isClassical())
                keyByKeyName.put("Mode", "Classical");

        }
        modelAndView.setViewName("/committees/proposals/joint");
        modelAndView.addObject("proposals", proposals);
        modelAndView.addObject("sortKeys", sortKeys);
        modelAndView.addObject("committeeId", committeeId);
        modelAndView.addObject("jointProposalAdmin", getUser().isInRole(AuthorityRole.ROLE_JOINT_PROPOSAL_ADMIN));

        return modelAndView;
    }

    @RequestMapping(value = "/{proposalId}/remove/{componentId}", method=RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isInRole('ROLE_JOINT_PROPOSAL_ADMIN')")
    public void removeComponentFromJoint(@PathVariable final Long committeeId, @PathVariable final Long proposalId, @PathVariable final Long componentId) {
        LOGGER.log(Level.INFO, "Removing " + componentId + " from Joint Proposal " + proposalId);
        final Committee committee = committeeService.getCommittee(committeeId);

        try {
            if (containsComponent(committeeId, proposalId, componentId)) {
                if (!isMaster(committeeId, proposalId, componentId)) {
                    Proposal recreation = jointProposalService.recreateWithoutComponent(committeeId, proposalId, componentId);
                    LOGGER.log(Level.DEBUG, "Recreated " + proposalId + " as " + recreation.getId() + " sans " + proposalId);
                    final String jointPartnerReferenceNumber = recreation.getPartnerAbbreviation() + " : " + recreation.getPartnerReferenceNumber();
                    final String jointLink = "<a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\"> " + jointPartnerReferenceNumber + "</a>";
                    final String componentLink = "<a href=\"/tac/committees/" + committeeId + "/proposals/" + componentId + "\">proposal</a>";
                    final String logMessage = getUser().getName() + " removed " + componentLink + " from " + jointLink + ".";
                    logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
                        add(LogEntry.Type.PROPOSAL_JOINT);
                    }}, committee, recreation));
                } else {
                    LOGGER.log(Level.ERROR, "Trying to remove master component");
                    throw new RuntimeException("Cannot remove master component from proposal; you must delete entire proposal");
                }
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARN, t);
        }
    }

    @RequestMapping(value = "/{proposalId}/with_master/{componentId}", method=RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isInRole('ROLE_JOINT_PROPOSAL_ADMIN')")
    public void setPrimaryProposal(@PathVariable final Long committeeId, @PathVariable final Long proposalId, @PathVariable final Long componentId) {
        LOGGER.log(Level.INFO, "Making " + componentId + " master of Joint Proposal " + proposalId);
        final Committee committee = committeeService.getCommittee(committeeId);

        if(containsComponent(committeeId, proposalId, componentId)){
            JointProposal jp = (JointProposal) proposalService.getProposal(committeeId, proposalId);
            Proposal nm = proposalService.getProposal(committeeId, componentId);
            JointProposal newJoint = jointProposalService.duplicateWithNewMaster(jp, nm);
            LOGGER.log(Level.DEBUG, "New joint proposal created " + newJoint.getId());
            final String logMessage = getUser().getName() + " changed master of previous joint proposal " + jp.getPartnerAbbreviation() + " : " + jp.getPartnerReferenceNumber() + ".  New <a href=\"/tac/committees/" + committeeId + "/proposals/" + newJoint.getId() + "\">" + newJoint.getPartnerAbbreviation() + " : " + newJoint.getPartnerReferenceNumber() + "</a>.";
            logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.PROPOSAL_JOINT);
            }}, committee, jp));
        }
    }

    @RequestMapping(value = "/{proposalId}/new_components/", method=RequestMethod.POST)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isInRole('ROLE_JOINT_PROPOSAL_ADMIN')")
    public void addNewComponent(@PathVariable final Long committeeId,
                                @PathVariable final Long proposalId,
                                @RequestParam(value = "newComponentIds[]") final String[] newComponentIdStrings,
                                final HttpServletResponse response) {
        for (String newComponentIdString : newComponentIdStrings) {
            LOGGER.info(String.format("Adding new components %s to existing master proposal %s", StringUtils.join(newComponentIdStrings, ","), proposalId));
        }
        final StringBuilder flashBuilder = new StringBuilder();
        boolean successful = true;
        boolean multipleProposalsFromPartner = false;
        boolean crossSiteViolation = false;

        final Committee committee = committeeService.getCommittee(committeeId);
        final Proposal proposal = proposalService.getProposal(committeeId, proposalId);
        Validate.isTrue(proposal.isJoint());
        JointProposal jointProposal = (JointProposal) proposal;
        final Set<Partner> partnerSet = new HashSet<Partner>();
        for (Proposal p : proposal.getProposals()) {
            partnerSet.add(p.getPartner());
        }
        final Set<Partner> originalPartnerSet = new HashSet<Partner>();
        originalPartnerSet.addAll(partnerSet);

        final Map<Long, Proposal> idToComponentsMap = new HashMap<Long, Proposal>();
        for (String newComponentIdString : newComponentIdStrings) {
            final Long newComponentId = Long.valueOf(newComponentIdString);
            final Proposal newComponentProposal = proposalService.getProposal(committeeId, newComponentId);
            final Partner partner = newComponentProposal.getPartner();
            final Site newComponentProposalSite = newComponentProposal.getSite();

            if (!partnerSet.contains(partner)) {
                idToComponentsMap.put(newComponentId, newComponentProposal);
                partnerSet.add(partner);
            } else {
                successful = false;
                multipleProposalsFromPartner = true;
                if (originalPartnerSet.contains(partner))
                    flashBuilder.append(String.format("<p>Joint proposal already contained a proposal from partner %s.</p>", partner.getName()));
                else
                    flashBuilder.append(String.format("<p>Joint proposal would contain multiple proposals from partner %s.</p>", partner.getName()));
            }

            if (newComponentProposalSite != jointProposal.getSite()) {
                successful = false;
                crossSiteViolation = true;
                flashBuilder.append(String.format("<p>Joint proposal is site %s.  New component proposal %s is site %s.</p>", jointProposal.getSite().getDisplayName(), newComponentProposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId(), newComponentProposal.getSite().getDisplayName()));
            }
        }

        if (!multipleProposalsFromPartner && !crossSiteViolation) {
            for (String newComponentIdString : newComponentIdStrings) {
                final Proposal newComponentProposal = proposalService.getProposal(committeeId, Long.valueOf(newComponentIdString));
                LOGGER.info(String.format("Merging new component %s to existing master proposal %s", newComponentIdString, proposalId));
                jointProposalService.add(jointProposal, newComponentProposal);
                jointProposal = (JointProposal) proposalService.getProposal(committeeId, jointProposal.getId());

                final String logMessage = String.format("%s added proposal %s into joint proposal %s as new component.",
                        getUser().getName(),
                        newComponentProposal.getDetailsUrl(),
                        jointProposal.getDetailsUrl());
                logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
                    add(LogEntry.Type.PROPOSAL_JOINT);
                }}, committee, jointProposal));
                logService.addLogEntry(logService.createNewForProposalOnly(logMessage, new HashSet<LogEntry.Type>() {{
                    add(LogEntry.Type.PROPOSAL_JOINT);
                }}, newComponentProposal));
                flashBuilder.append(String.format("<p>New component proposal %s:%s added to joint proposal %s:%s successfully.</p>",
                        newComponentProposal.getPartner().getAbbreviation(),
                        newComponentProposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId(),
                        jointProposal.getPartner().getAbbreviation(),
                        jointProposal.getPhaseIProposal().getPrimary().getReceipt().getReceiptId()));
            }
        }

        if (successful) {
            response.setStatus(200);
            response.setContentType("application/json");
            PrintWriter writer = null;
            try {
                writer = response.getWriter();
                writer.println(String.format("{\"message\": \"%s\"}", flashBuilder.toString()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (writer != null) writer.close();
            }
        }
        else {
            response.setStatus(500);
            response.setContentType("application/json");
            PrintWriter writer = null;
            try {
                writer = response.getWriter();
                writer.println(String.format("{\"message\": \"%s\"}",flashBuilder.toString()));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (writer != null) writer.close();
            }
        }
    }

    @RequestMapping(value = "/{proposalId}", method=RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isInRole('ROLE_JOINT_PROPOSAL_ADMIN')")
    public ModelAndView dissolveJoint(@PathVariable final Long committeeId, @PathVariable final Long proposalId) {
        LOGGER.log(Level.INFO, "Dissolving joint proposal " + proposalId);
        final Committee committee = committeeService.getCommittee(committeeId);

        ModelAndView mav = new ModelAndView();
        try{
            JointProposal jp = (JointProposal) proposalService.getProposal(committeeId, proposalId);
            final String logMessage = getUser().getName() + " deleted joint proposal " + jp.getPartnerAbbreviation() + " : " + jp.getPartnerReferenceNumber();
            committeeService.dissolve(committee, jp);
            logService.addLogEntry(logService.createNewForCommittee(logMessage, new HashSet<LogEntry.Type>() {{
                add(LogEntry.Type.PROPOSAL_JOINT);
            }}, committee));

        }catch (Throwable t){
            LOGGER.log(Level.WARN, t);
            mav.setViewName("redirect:/error_page");
        }
        return mav;
    }

    private boolean isMaster(Long committeeId, Long proposalId, Long componentId){
        JointProposal joint = (JointProposal) proposalService.getProposal(committeeId, proposalId);
        Proposal primary = joint.getPrimaryProposal();
        return primary.getEntityId().equals(componentId);
    }

    private boolean containsComponent(Long committeeId, Long proposalId, Long componentId) {
        //Confirm that the componentId refers to a component
        Proposal joint = proposalService.getProposal(committeeId, proposalId);
        for(Proposal component : joint.getProposals()){
            if(component.getId().equals(componentId)){
                return true;
            }
        }
        LOGGER.log(Level.WARN, "Could not find " + componentId + " as a component of " + proposalId);
        return false;
    }

    @RequestMapping(value="create", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("isInRole('ROLE_JOINT_PROPOSAL_ADMIN')")
    public String createJointProposal(@PathVariable Long committeeId,
                                      @RequestParam final Long masterProposalId,
                                      @RequestParam final String secondaryProposalIds) {
        LOGGER.log(Level.INFO, "Creating JointProposal from Proposals [" + masterProposalId + "] & [" + secondaryProposalIds + "]");
        final Committee committee = committeeService.getCommittee(committeeId);

        final Proposal master = proposalService.getProposal(committeeId, masterProposalId);
        final StringBuilder flashBuilder = new StringBuilder();
        boolean successful = true;

        String[] secondaries = secondaryProposalIds.split(",");
        if(secondaries.length < 1){
            //TODO: Error-handling for input validation
            throw new NotImplementedException();
        }
        JointProposal merged = null;
        final StringBuilder logMessageBuffer = new StringBuilder(getUser().getName());
        logMessageBuffer.append(" created new joint proposal with master ");
        addProposalLinkToBuilder(committeeId, master, logMessageBuffer);

        logMessageBuffer.append("from secondaries ");

        for(String secondaryProposalId : secondaries){
            try{
                Long id = Long.parseLong(secondaryProposalId);
                Proposal secondary = proposalService.getProposal(committeeId, id);
                try {
                    if(merged == null){
                        merged = jointProposalService.mergeProposals(master, secondary);
                    }else{
                        jointProposalService.add(merged, secondary);
                    }
                    addProposalLinkToBuilder(committeeId,secondary,logMessageBuffer);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn(e);
                    flashBuilder.append(e.getMessage());
                    successful = false;
                }
            }catch(Throwable x){
                LOGGER.log(Level.ERROR, "Exception merging [" + secondaryProposalId + "]");
                LOGGER.log(Level.ERROR, x);
                flashBuilder.append("Exception merging [" + secondaryProposalId + "]:" + x.getMessage());
                successful = false;
            }
            logMessageBuffer.append(" ");
        }

        logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessageBuffer.toString(), new HashSet<LogEntry.Type>() {{
            add(LogEntry.Type.PROPOSAL_JOINT);
        }}, committee, master));


        if (successful)
            return "redirect:/tac/committees/" + committeeId + "/proposals/" + merged.getId();
        else
            return "redirect:/tac/committees/" + committeeId + "/proposals/joint?flash=" + flashBuilder.toString();
    }

    private void addProposalLinkToBuilder(final long committeeId, final Proposal proposal, StringBuilder builder) {
        builder.append("<a href=\"/tac/committees/" + committeeId + "/proposals/" + proposal.getId() + "\">" +
                proposal.getPartnerAbbreviation() + " : " + proposal.getPartnerReferenceNumber() + "</a>");
    }
}


