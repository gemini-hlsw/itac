package edu.gemini.tac.itac.web;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.*;

@Controller
@RequestMapping("/home")
public class HomeController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(HomeController.class.getName());

    @Resource(name ="errorMap")
    Map<String, Integer> errorMap;


    @RequestMapping(method = RequestMethod.GET)
    public String home() {
        LOGGER.log(Level.ALL, "home");
        return "redirect:/tac/committees";
    }

    @RequestMapping(value = "errors", method = RequestMethod.GET)
    public ModelAndView errors() {
        LOGGER.log(Level.ALL,"home");

        final ModelAndView mav = new ModelAndView();
        mav.setViewName("errors");


        final SortedSet<String> errors  = new TreeSet<String>();
        for (Map.Entry<String,Integer> entry : errorMap.entrySet()) {
            errors.add(String.format("%08d", entry.getValue()) + " => " + entry.getKey());
        }

        mav.addObject("errorMap",errorMap);
        mav.addObject("errors", errors.toString());

        return mav;
    }

    @Override
    protected edu.gemini.tac.itac.web.UrlFor.Controller getController() {
        return UrlFor.Controller.HOME;
    }

    @Override
    protected edu.gemini.tac.itac.web.UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.HOME;
    }
}
