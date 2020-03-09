package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.service.IPartnerService;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * Configuring Emails for committee contacts
 */
@Controller
public class EmailAddressesController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(EmailAddressesController.class.getName());

    @Resource(name = "partnerService")
    private IPartnerService partnerService;


    public EmailAddressesController() {
        super();

        subheaders = ConfigurationController.getSubheaderLinks();
    }

    private ModelAndView boilerPlateModelAndView() {
        ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), subheaders);
        return modelAndView;
    }

    @RequestMapping(value = "/configuration/email-addresses", method = RequestMethod.GET)
    public ModelAndView addressesGet() {
        LOGGER.log(Level.DEBUG, "addresses (GET)");

        ModelAndView modelAndView = boilerPlateModelAndView();
        final List<Partner> partners = partnerService.findAllPartners();
        modelAndView.addObject(partners);

        modelAndView.setViewName("configuration/emails/email-addresses");
        return modelAndView;
    }

    @RequestMapping(value = "/configuration/email-addresses", method = RequestMethod.POST)
    public void addressesPost(
            @RequestParam final String partnerName,
            @RequestParam final String newEmailAddress,
            HttpServletResponse response) {
        LOGGER.log(Level.DEBUG, "addresses (POST)");
        try {
            Validate.notEmpty(partnerName, "Partner name not passed");
            Validate.notEmpty(newEmailAddress, "New email address not passed");
            /* Not going to validate it, because we want flexibility e.g., "foo@bar.com;blurg@baz.com" */
            //validateEmail(newEmailAddress);
            partnerService.setNgoContactEmail(partnerName, newEmailAddress);
            //Reset content (i.e., "Accepted data, reset the document view")
            response.setStatus(205);
        } catch (Exception x) {
            LOGGER.error(x);
            response.setStatus(500);
        }
    }

    @Override
    protected UrlFor.Controller getController() {
        return UrlFor.Controller.EMAIL_CONTACTS;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.CONFIGURATION;
    }
}
