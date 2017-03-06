package edu.gemini.tac.itac.web.committees;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class WindowsController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(WindowsController.class.getName());
	
	@RequestMapping(value = "/committees/{committeeId}/windows", method = RequestMethod.GET)
    public ModelAndView index(@PathVariable final Long committeeId) {
		LOGGER.log(Level.DEBUG, "index");
		
        final ModelAndView modelAndView = new ModelAndView();
        CommitteesController.addCommitteeToModel(committeeService, committeeId, modelAndView);
        addResourceInformation(modelAndView.getModelMap());
		 
        addSubHeaders(modelAndView.getModelMap(), CommitteesController.getSubheaderLinks(committeeId));
        addTopLevelResource(modelAndView.getModelMap(), String.valueOf(committeeId));
        modelAndView.setViewName("committees/windows/index");

        return modelAndView;
    }
	
	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.WINDOWS;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.COMMITTEES;
	}
}
