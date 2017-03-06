package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.bin.BinConfiguration;
import edu.gemini.tac.service.configuration.IBinConfigurationsService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

@Controller
@RequestMapping("/configuration/radec")
public class RaDecController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(RaDecController.class.getName());

	@Resource(name = "binConfigurationsService")
	private IBinConfigurationsService binConfigurationsService;
	
	public RaDecController() {
		super();
		
		subheaders = ConfigurationController.getSubheaderLinks();
	}
	
    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index() {
    	LOGGER.log(Level.DEBUG, "index");
    	
        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);
        
        final List<BinConfiguration> binConfigurations = binConfigurationsService.getAllBinConfigurations();
        
        modelAndView.addObject("binConfigurations", binConfigurations);
        
        modelAndView.setViewName("configuration/radec/index");
        return modelAndView;
    }

	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.RADEC;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.CONFIGURATION;
	}
}
