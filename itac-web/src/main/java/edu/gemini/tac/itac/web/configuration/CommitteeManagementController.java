package edu.gemini.tac.itac.web.configuration;

import edu.gemini.tac.itac.web.AbstractApplicationController;
import edu.gemini.tac.itac.web.UrlFor;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.Person;
import edu.gemini.tac.persistence.Semester;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ddawson
 * Date: 6/1/11
 * Time: 3:36 PM
 * To change this template use File | Settings | File Templates.
 */
@Controller
@RequestMapping("/configuration")
public class CommitteeManagementController extends AbstractApplicationController {
    private static final Logger LOGGER = Logger.getLogger(ConditionBinsController.class.getName());

    public CommitteeManagementController() {
        super();

        subheaders = ConfigurationController.getSubheaderLinks();
    }

    @RequestMapping(value = "/committees", method = RequestMethod.GET)
    public ModelAndView index() {
        LOGGER.log(Level.DEBUG, "index");

        final ModelAndView mav = new ModelAndView();
        mav.addObject("user", getUser());
        addSubHeaders(mav.getModelMap(), subheaders);
        addResourceInformation(mav.getModelMap());

        final List<Committee> allCommittees = committeeService.getAllCommittees();
        mav.addObject("committees", allCommittees);
        mav.setViewName("/configuration/committees/index");

        return mav;
    }

    @RequestMapping(value = "/committees", method = RequestMethod.POST)
    public ModelAndView createNewCommittee(@RequestParam(required = true) final String committeeName,
                                           @RequestParam(required = true) final String semesterName,
                                           @RequestParam(required = true) final Integer semesterYear) {
        LOGGER.log(Level.DEBUG, "createNewCommittee");

        final ModelAndView mav = new ModelAndView();
        mav.addObject("user", getUser());
        addSubHeaders(mav.getModelMap(), subheaders);
        addResourceInformation(mav.getModelMap());

        final Semester newSemester = new Semester();
        newSemester.setName(semesterName);
        newSemester.setYear(semesterYear);

        final Committee newCommittee = new Committee();
        newCommittee.setName(committeeName);
        newCommittee.setSemester(newSemester);

        final List<Person> allPeople = peopleService.getAllPeople();
        newCommittee.getMembers().addAll(allPeople);

        committeeService.saveCommitee(newCommittee);

        final List<Committee> allCommittees = committeeService.getAllCommittees();
        mav.addObject("committees", allCommittees);
        mav.setViewName("/configuration/committees/index");

        return mav;
    }

    @RequestMapping(value = "committees/{committeeId}/edit", method = RequestMethod.POST)
    public ModelAndView editCommittee(@PathVariable Long committeeId,
                                      @RequestParam(required = false) Boolean active) {
        LOGGER.log(Level.DEBUG, "editCommittee");

        final ModelAndView mav = new ModelAndView();
        mav.addObject("user", getUser());
        addSubHeaders(mav.getModelMap(), subheaders);
        addResourceInformation(mav.getModelMap());

        final Committee committee = committeeService.getCommittee(committeeId);
        committee.setActive((active == null) ? Boolean.FALSE : active);
        committeeService.saveCommitee(committee);

        final List<Committee> allCommittees = committeeService.getAllCommittees();
        mav.addObject("committees", allCommittees);
        mav.setViewName("redirect:/tac/configuration/committees");

        return mav;
    }

    protected UrlFor.Controller getController() {
        return UrlFor.Controller.COMMITTEES_MANAGEMENT;
    }

    @Override
    protected UrlFor.Controller getTopLevelController() {
        return UrlFor.Controller.CONFIGURATION;
    }
}
