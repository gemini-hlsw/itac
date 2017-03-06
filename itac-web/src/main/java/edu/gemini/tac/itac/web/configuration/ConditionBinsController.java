package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.condition.ConditionSet;
import edu.gemini.tac.service.configuration.IConditionsService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ConditionBinsController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(ConditionBinsController.class.getName());

	@Resource(name = "conditionsService")
	private IConditionsService conditionsService;
	
	public ConditionBinsController() {
		super();
		
		subheaders = ConfigurationController.getSubheaderLinks();
	}
	
    @RequestMapping(value = "/configuration/condition-bins", method = RequestMethod.GET)
    public ModelAndView index() {
    	LOGGER.log(Level.DEBUG, "index");
    	
        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);
        final List<ConditionSet> conditionSets = conditionsService.getAllConditionSets();
        modelAndView.addObject("conditionSets", conditionSets);
        modelAndView.setViewName("configuration/conditions/index");

        return modelAndView;
    }

    @RequestMapping(value = "/configuration/condition-bins/{conditionSetId}/copy", method = RequestMethod.GET)
    public ModelAndView copy(@PathVariable final Long conditionSetId) {
    	LOGGER.log(Level.DEBUG, "copy");

        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);
        ConditionSet conditionSet = conditionsService.getConditionSetById(conditionSetId);
        modelAndView.addObject("conditionSet", conditionSet);
        modelAndView.setViewName("configuration/conditions/copy");

        return modelAndView;
    }

    @RequestMapping(value = "/configuration/condition-bins/{conditionSetId}/copy", method = RequestMethod.POST)
    public ModelAndView copyPost(@PathVariable final Long conditionSetId,
                                 @RequestParam Map<String, String> params) {
    	LOGGER.log(Level.DEBUG, "copyPost");

        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);
        final ConditionSet conditionSet = conditionsService.getConditionSetById(conditionSetId);

        final String name = params.get("name");
        params.remove("name");

        final Long[] conditionIds = new Long[params.size()];
        final Integer[] percentages = new Integer[params.size()];

        final Set<Map.Entry<String,String>> entries = params.entrySet();
        Iterator<Map.Entry<String,String>> iterator = params.entrySet().iterator();

        for (int i = 0; i < conditionIds.length; i++) {
            final Map.Entry<String,String> entry = iterator.next();
            conditionIds[i] = Long.valueOf(entry.getKey().substring("condition-".length()));
            percentages[i] = Integer.valueOf(entry.getValue());
        }

        conditionsService.copyConditionSetAndUpdatePercentages(conditionSet, name, conditionIds, percentages);

        modelAndView.addObject("conditionSet", conditionSet);
        modelAndView.setViewName("redirect:/tac/configuration/condition-bins");

        return modelAndView;
    }

	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.CONDITION_BINS;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.CONFIGURATION;
	}
}
