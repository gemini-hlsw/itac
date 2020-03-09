package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.emails.Template;
import edu.gemini.tac.service.IEmailsService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.List;

@Controller
public class EmailTemplatesController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(EmailTemplatesController.class.getName());
	
    @Resource(name="emailsService")
    private IEmailsService emailsService;

	public EmailTemplatesController() {
		super();
		
		subheaders = ConfigurationController.getSubheaderLinks();
	}
	
    @RequestMapping(value = "/configuration/email-templates", method = RequestMethod.POST)
    public ModelAndView editTemplateGet(
            @RequestParam final String submit,
            @RequestParam final String subject,
            @RequestParam final String template,
            @RequestParam final int index
    ) {
    	LOGGER.log(Level.DEBUG, "editTemplateGet");
    	
        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);

        List<Template> templates = emailsService.getTemplates();
        if (submit.equals("save")) {
            emailsService.updateTemplate(templates.get(index - 1), subject, template);
        }

        modelAndView.addObject("templates", templates);
        modelAndView.setViewName("configuration/emails/email-templates");
        return modelAndView;
    }
    
    @RequestMapping(value = "/configuration/email-templates", method = RequestMethod.GET)
    public ModelAndView editTemplatePost() {
    	LOGGER.log(Level.DEBUG, "editTemplatePost");

        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);

        modelAndView.addObject("templates", emailsService.getTemplates());

        modelAndView.setViewName("configuration/emails/email-templates");
        return modelAndView;
    }

	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.EMAIL_TEMPLATES;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.CONFIGURATION;
	}
}
