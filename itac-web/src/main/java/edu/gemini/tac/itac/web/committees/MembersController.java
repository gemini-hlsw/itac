package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class MembersController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(MembersController.class.getName());

	public static final String[] COMMITTEE_MEMBERS_SUBSUBHEADERS = {"List", "Edit"};

	public MembersController() {
		super();

		subsubheaders = COMMITTEE_MEMBERS_SUBSUBHEADERS;
	}

	public static Link[] getCollectionLinks(final String topLevelResourceId) {
		return new Link[]{
				new Link(UrlFor.Controller.MEMBERS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "list", "List members"),
				new Link(UrlFor.Controller.MEMBERS, UrlFor.Controller.COMMITTEES, topLevelResourceId, "edit", "Edit membership")
			};
	}

	@RequestMapping(value = "/committees/{committeeId}/members", method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId) {
        final ModelAndView modelAndView = new ModelAndView();

        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addTopLevelController(modelAndView.getModelMap());
        addController(modelAndView.getModelMap());

        modelAndView.addObject("user", getUser());
        modelAndView.addObject("committee",committeeService.getCommitteeWithMemberships(committeeId));
        modelAndView.setViewName("committees/members");

        return modelAndView;
    }

//	@RequestMapping(value = "/committees/{committeeId}/members/edit", method = RequestMethod.GET)
//    public ModelAndView edit(@PathVariable final Long committeeId) {
//		LOGGER.finest("edit");
//
//        final ModelAndView modelAndView = new ModelAndView();
//        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
//        addResourceInformation(modelAndView.getModelMap());
//
//        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
//        addSubSubHeaders(modelAndView.getModelMap(), getCollectionLinks(String.valueOf(committeeId)));
//        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));
//        modelAndView.setViewName("committees/members/edit");
//
//        return modelAndView;
//    }
	
	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.MEMBERS;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.COMMITTEES;
	}
}
