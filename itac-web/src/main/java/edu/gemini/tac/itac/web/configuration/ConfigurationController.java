package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.Link;
import edu.gemini.tac.itac.web.UrlFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.logging.Logger;

@Controller
@RequestMapping("/configuration")
public class ConfigurationController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationController.class.getName());

    public static Link[] getSubheaderLinks() {
        return new Link[]{
                new Link(UrlFor.Controller.CONDITION_BINS, UrlFor.Controller.CONFIGURATION),
                new Link(UrlFor.Controller.RADEC, UrlFor.Controller.CONFIGURATION),
                new Link(UrlFor.Controller.EMAIL_TEMPLATES, UrlFor.Controller.CONFIGURATION),
                new Link(UrlFor.Controller.EMAIL_CONTACTS, UrlFor.Controller.CONFIGURATION),
                new Link(UrlFor.Controller.COMMITTEES_MANAGEMENT, UrlFor.Controller.CONFIGURATION),
                new Link(UrlFor.Controller.QUEUE_CONFIGURATION, UrlFor.Controller.CONFIGURATION)
        };
    }

    public ConfigurationController() {
        super();

        subheaders = getSubheaderLinks();
    }

    @RequestMapping(method = RequestMethod.GET)
    public String index() {
        LOGGER.finest("Configuration index");

        return "redirect:/tac/configuration/committees";
    }

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.CONFIGURATION;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.CONFIGURATION;
    }
}
