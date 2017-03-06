package edu.gemini.tac.itac.web;

import java.util.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import edu.gemini.tac.persistence.*;
import edu.gemini.tac.persistence.joints.JointProposal;
import edu.gemini.tac.persistence.security.AuthorityRole;
import edu.gemini.tac.service.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.ui.ModelMap;

import edu.gemini.tac.itac.web.UrlFor.Controller;
import org.springframework.web.servlet.ModelAndView;

public abstract class AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(AbstractApplicationController.class.getName());

    protected Link[] subheaders = new Link[]{};
	protected String[] subsubheaders = new String[]{};

    @Resource(name = "logService")
    private AbstractLogService logService;

    @Resource(name = "peopleService")
	protected IPeopleService peopleService;

	@Resource(name = "committeeService")
	protected ICommitteeService committeeService;

    @Resource(name = "proposalService")
    protected IProposalService proposalService;

    @Resource(name = "applicationService")
    protected IApplicationService applicationService;

    @Resource(name = "partnerService")
    protected IPartnerService partnerService;

    @Resource(name = "itacProperties")
    protected Properties itacProperties;

    @Value("${itac.debug}")
    private boolean DEBUG;

    @Value("${itac.version}")
    private String VERSION;
    /**
	 * Awkward before method in order to centralize identifier for the 
	 * header template.  Intended to provide information relevant to
	 * the first level of menus.
	 * 
	 * @param modelMap
	 * @param subHeaders
	 */
	protected void addSubHeaders(final ModelMap modelMap, final Link[] subHeaders) {
    	modelMap.addAttribute("subheaders", subHeaders);
    }
    /**
     * Awkward before method in order to centralize identifier for header template.
     * The subsubheaders tend to be laid out over to the right of the primary information
     * in that view.
     * 
     * @param modelMap
     * @param subsubHeaders
     */
    protected void addSubSubHeaders(final ModelMap modelMap, final Link[] subsubHeaders) {
    	modelMap.addAttribute("subsubheaders", subsubHeaders);
    }
    /**
     * Some pages deal with a particular resource.  This method identifies the
     * set of controllers at play for the current request.
     * 
     * @param modelMap
     */
    protected void addResourceInformation(final ModelMap modelMap) {
    	addTopLevelController(modelMap);
    	addController(modelMap);
    }
    
	protected void addController(final ModelMap modelMap) {
		modelMap.addAttribute("controller", getController());
	}

	protected void addTopLevelController(final ModelMap modelMap) {
		modelMap.addAttribute("topLevelController", getTopLevelController());
		final List<Committee> activeCommittees = committeeService.getAllActiveCommittees();
		
		final List<Link> topLevelLinks = new ArrayList<Link>();
		for (Committee c : activeCommittees) {
			topLevelLinks.add(new Link(UrlFor.Controller.PROPOSALS, UrlFor.Controller.COMMITTEES, c.getId().toString(), c.getName()));
		}
		topLevelLinks.add(new Link(UrlFor.Controller.COMMITTEES));

        final Person person = getUser();
        final Set<AuthorityRole> authorities = person.getAuthorities();
        for (AuthorityRole authority : authorities) {
            if (authority.getRolename().equals(AuthorityRole.ROLE_SECRETARY) || authority.getRolename().equals(AuthorityRole.ROLE_ADMIN)) {
                topLevelLinks.add(new Link(UrlFor.Controller.PEOPLE));
                topLevelLinks.add(new Link(UrlFor.Controller.CONFIGURATION));
            }
        }

		modelMap.addAttribute("topLevelLinks",topLevelLinks);
        modelMap.addAttribute("debug", DEBUG);
        modelMap.addAttribute("version", VERSION);
        modelMap.addAttribute("broadcastMessage", applicationService.findLatestBroadcastMessage());
        modelMap.addAttribute("allPartners", partnerService.findAllPartners());
	}

	protected void addTopLevelResource(final ModelMap modelMap, final String topLevelresource) {
		modelMap.addAttribute("topLevelResource", topLevelresource);
	}
	
	protected String[] getSubSubheaders() {return subsubheaders;};

    protected Person getUser() {
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final User user = (User) securityContext.getAuthentication().getPrincipal();
        final String userName = user.getUsername();
        final Person person = peopleService.findByName(userName);
        if(person == null){
            throw new IllegalStateException("No person corresponding to user [" + userName + "] found in database");
        }
        return person;
    }

    protected Person addUserToModel(final ModelAndView modelAndView) {
        final Person user = getUser();
        modelAndView.addObject("user", user);
        return user;
    }

    /**
     * Edits a single value in a proposal.
     * Used for in-place editing in several pages (e.g. proposal show, proposal bulk-edit and queue bulk-edit).
     * @param committeeId
     * @param proposalId
     * @param targetType
     * @param field
     * @param value
     * @param naturalId
     * @param response
     */
    protected void doProposalEdit(final Long committeeId, final Long proposalId, final String targetType, final String field, final String value, final String naturalId, HttpServletResponse response) {
        LOGGER.log(Level.DEBUG, "edit(" + committeeId + ", " + proposalId + ", " + targetType + ", " + field + ", " + value + ", " + naturalId + ")");
        //Security check
        if (allowedToEdit(committeeId, proposalId, getUser())) {
            final Committee committee = committeeService.getCommittee(committeeId);
            final StringBuilder fromValueBuffer = new StringBuilder();
            try {
                Proposal p = proposalService.editProposalWithAlreadyValidatedData(committeeId, proposalId, targetType, naturalId, field, value.trim(), fromValueBuffer);
                final String logMessage = getUser().getName() + " edited <a href=\"/tac/committees/" + committeeId + "/proposals/" + proposalId + "\">" + p.getPartnerReferenceNumber() + "</a> set [" + targetType + "," + naturalId + "," + field + "," + value + "]. Previous value [" + fromValueBuffer.toString() +"]";
                logService.addLogEntry(logService.createNewForCommitteeAndProposal(logMessage, new HashSet<LogEntry.Type>() {{
                    add(LogEntry.Type.PROPOSAL_EDITED);
                }}, committee, p));

                response.setStatus(200);
            } catch (RuntimeException e) {
                LOGGER.error("Problem editing proposal " + proposalId, e);
                response.setStatus(500);
            }
        } else {
            LOGGER.error("User not allowed to POST edit for proposal");
            response.setStatus(403);
        }
    }

    /**
     * Checks is user is actually allowed to edit a proposal.
     * @param committeeId
     * @param proposalId
     * @param user
     * @return
     */
    protected boolean allowedToEdit(Long committeeId, Long proposalId, Person user) {
        //Secretary and admin -- yes
        final Set<AuthorityRole> authorities = user.getAuthorities();
        for (AuthorityRole authority : authorities) {
            if (authority.getRolename().equals(AuthorityRole.ROLE_SECRETARY) || authority.getRolename().equals(AuthorityRole.ROLE_ADMIN)) {
                return true;
            }
        }

        //Proposal partner also allowed
        Partner userPartner = user.getPartner();
        Proposal p = proposalService.getProposal(committeeId, proposalId);
        if (p.getPartnerAbbreviation().equals(userPartner.getAbbreviation())) {
            return true;
        }

        //ITAC-344 allows other partners to edit too
        if (p instanceof JointProposal) {
            String partnerAbbreviation = p.getPartnerAbbreviation();
            if (partnerAbbreviation.indexOf(userPartner.getAbbreviation()) > -1) {
                return true;
            }
        }

        return false;
    }

    abstract protected Controller getController();
	abstract protected Controller getTopLevelController();
}
