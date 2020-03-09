package edu.gemini.tac.itac.web;

import edu.gemini.tac.persistence.Person;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
@Controller
@RequestMapping("/session")
public class SessionController  extends AbstractApplicationController {
    public static final String SESSION_ATTRIBUTE_USER = "user";
    private static final Logger LOGGER = Logger.getLogger(SessionController.class.getName());

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView login() {
        LOGGER.log(Level.DEBUG, "login");
        final ModelAndView modelAndView = new ModelAndView();
        
        addResourceInformation(modelAndView.getModelMap());
        modelAndView.setViewName("session/login");
        
        return modelAndView;
    }

    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public ModelAndView logout(final HttpSession session) {
        LOGGER.log(Level.DEBUG, "logout");
        final ModelAndView modelAndView = new ModelAndView();
        
        session.removeAttribute(SESSION_ATTRIBUTE_USER);
        addResourceInformation(modelAndView.getModelMap());
        modelAndView.setViewName("home");
        
        return modelAndView;
    }
    
    @RequestMapping(method = RequestMethod.POST)
    public ModelAndView loginPost(final HttpSession session, @RequestParam(value = "userName") final String userName,
            @RequestParam(value = "password") final String password) {
        final ModelAndView modelAndView = new ModelAndView();
        addResourceInformation(modelAndView.getModelMap());

        final Person user = peopleService.findByName(userName);
        if ((user != null) && user.getPassword().equals(password)) {
            session.setAttribute(SESSION_ATTRIBUTE_USER, user);
            modelAndView.addObject("user", user);
            modelAndView.setViewName("people/me");
        } else {
            modelAndView.setViewName("session/login");
        }
        
        return modelAndView;
    }
    
    @Override
    protected edu.gemini.tac.itac.web.UrlFor.Controller getController() {
        return UrlFor.Controller.SESSION;
    }

    @Override
    protected edu.gemini.tac.itac.web.UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.SESSION;
    }
}
