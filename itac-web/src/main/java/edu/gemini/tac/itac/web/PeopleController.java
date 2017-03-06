package edu.gemini.tac.itac.web;

import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.service.IPeopleService;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;

@Controller
@RequestMapping("/people")
public class PeopleController extends AbstractApplicationController {
	private static final Logger LOGGER = Logger.getLogger(PeopleController.class.getName());

	@Resource(name = "peopleService")
	private IPeopleService peopleService;
	
	public static Link[] getSubheaderLinks() {
		return new Link[]{ 
				new Link(UrlFor.Controller.ME, UrlFor.Controller.PEOPLE)/*,
				new Link(UrlFor.Controller.ADMIN, UrlFor.Controller.PEOPLE)*/
			};	
	}
	
	public PeopleController() {
		super();
	}

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index() {
        return me();
    }

	
    @RequestMapping(value = "/me", method = RequestMethod.GET)
    public ModelAndView me() {
        final ModelAndView modelAndView = new ModelAndView();
        
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), getSubheaderLinks());
        modelAndView.addObject("user", peopleService.findMe(getUser().getName()));
        
        modelAndView.setViewName("people/me");
        
        return modelAndView;
    }

    @RequestMapping(value = "/me", method = RequestMethod.POST)
    public ModelAndView meEdit(@RequestParam(required = true) final String name,
            @RequestParam(required = true) final String password,
            @RequestParam(required = true, value = "confirm-password") final String confirmPassword) {
        final ModelAndView modelAndView = new ModelAndView();
        final Person me = getUser();

        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), getSubheaderLinks());

        modelAndView.setViewName("people/me");

        final Person byName = peopleService.findByName(name);
        if (byName != null && (!me.getId().equals(byName.getId()))) {
            modelAndView.addObject("flash", "Can only update information for yourself.");
            modelAndView.addObject("user", peopleService.findMe(me.getName()));
        } else if (!password.equals(confirmPassword)) {
            modelAndView.addObject("flash", "Passwords must match.");
            modelAndView.addObject("user", peopleService.findMe(me.getName()));
        } else {
            me.setName(name);
            me.setPassword(password);
            peopleService.update(me);
            modelAndView.addObject("notice", "Successfully updated.");

            Authentication request = new UsernamePasswordAuthenticationToken(me.getName(), me.getPassword());
            SecurityContextHolder.getContext().setAuthentication(request);

            modelAndView.addObject("user", peopleService.findMe(me.getName()));
        }

        return modelAndView;
    }

    @RequestMapping(value = "/admin", method = RequestMethod.GET)
    public ModelAndView admin() {
        final ModelAndView modelAndView = new ModelAndView();
        
        addResourceInformation(modelAndView.getModelMap());
        addSubHeaders(modelAndView.getModelMap(), getSubheaderLinks());
        modelAndView.setViewName("people/admin/index");
        
        return modelAndView;
    }
    
	@Override
	protected UrlFor.Controller getController() {
		return UrlFor.Controller.PEOPLE;
	}

	@Override
	protected UrlFor.Controller getTopLevelController() {
		return UrlFor.Controller.PEOPLE;
	}	
}
